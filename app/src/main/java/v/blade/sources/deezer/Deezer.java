package v.blade.sources.deezer;

import androidx.fragment.app.Fragment;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import v.blade.BladeApplication;
import v.blade.BuildConfig;
import v.blade.R;
import v.blade.library.Playlist;
import v.blade.sources.Source;
import v.blade.ui.ExploreFragment;

public class Deezer extends Source
{
    public static final int NAME_RESOURCE = R.string.deezer;
    public static final int DESCRIPTION_RESOURCE = R.string.deezer_desc;
    public static final int IMAGE_RESOURCE = R.drawable.ic_deezer;

    private static final String AUTH_TYPE = "Bearer ";
    private static final String CLIENT_ID = BuildConfig.DEEZER_CLIENT_ID;
    //This should not be exposed, but i have no other choice, as the Spotify app does not seem to support PKCE
    private static final String CLIENT_SECRET = BuildConfig.DEEZER_CLIENT_SECRET;

    protected String account_name; //i.e. username, retrieved by api
    protected String account_login; //what the user uses to login (mail or username)
    protected String account_password;

    //API login
    private String ACCESS_TOKEN;
    private int TOKEN_EXPIRES_IN;

    public Deezer()
    {
        super();
        this.name = BladeApplication.appContext.getString(NAME_RESOURCE);
        //this.player = new DeezerPlayer(this);
    }

    @Override
    public int getImageResource()
    {
        return IMAGE_RESOURCE;
    }

    @SuppressWarnings({"FieldMayBeFinal", "unused"})
    private static class DeezerTokenResponse
    {
        private String access_token = "";
        private String token_type = "";
        private int expires_in = -1;
        private String scope = "";

        private DeezerErrorObject error = null;

        public DeezerTokenResponse()
        {
        }
    }

    @SuppressWarnings({"FieldMayBeFinal", "unused"})
    private static class DeezerErrorObject
    {
        String type;
        String message;
        int code = 0;

        public DeezerErrorObject()
        {
        }
    }

    protected boolean refreshAccessTokenSync()
    {
        System.out.println("BLADE-DEEZER: Refreshing access token...");

        // Compute MD5 hash of password, to String, in hexadecimal format
        String hashed_password;
        try
        {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            digest.reset();
            byte[] digest_result = digest.digest(account_password.getBytes());
            BigInteger bi = new BigInteger(1, digest_result);
            hashed_password = String.format("%0" + (digest_result.length << 1) + "x", bi);
        }
        catch(NoSuchAlgorithmException e)
        {
            System.err.println("BLADE-DEEZER: NoSuchAlgorithmException (MD5)");
            e.printStackTrace();
            return false;
        }

        // Compute MD5 hash of request (client id + login + password + client secret)
        String request = CLIENT_ID + account_login + hashed_password + CLIENT_SECRET;
        String hashed_request;
        try
        {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            digest.reset();
            byte[] digest_result = digest.digest(request.getBytes());
            BigInteger bi = new BigInteger(1, digest_result);
            hashed_request = String.format("%0" + (digest_result.length << 1) + "x", bi);
        }
        catch(NoSuchAlgorithmException e)
        {
            System.err.println("BLADE-DEEZER: NoSuchAlgorithmException (MD5)");
            e.printStackTrace();
            return false;
        }

        // Emit request
        OkHttpClient client = new OkHttpClient();
        HttpUrl.Builder httpBuilder = Objects.requireNonNull(HttpUrl.parse("https://api.deezer.com/auth/token")).newBuilder();
        httpBuilder.addQueryParameter("app_id", CLIENT_ID)
                .addQueryParameter("login", account_login)
                .addQueryParameter("password", hashed_password)
                .addQueryParameter("hash", hashed_request);
        Request r = new Request.Builder()
                .addHeader("User-Agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/79.0.3945.130 Safari/537.36")
                .url(httpBuilder.build())
                .get().build();
        System.out.println("BLADE-DEEZER: " + r.toString());
        okhttp3.Call call = client.newCall(r);

        try
        {
            okhttp3.Response response = call.execute();
            if(!response.isSuccessful() || response.code() != 200 || response.body() == null)
            {
                String responseBody = response.body() == null ? "Unknown error" : response.body().string();
                System.err.println("BLADE-DEEZER: Could not refresh token" + " (" + response.code() + " : " + responseBody + ")");
                return false;
            }

            Gson gson = new Gson();
            String rstring = response.body().string();
            DeezerTokenResponse sr = gson.fromJson(rstring, DeezerTokenResponse.class);
            if(sr == null)
            {
                System.err.println("BLADE-DEEZER: Could not refresh token" + " (Could not parse JSON Token)");
                return false;
            }

            if(sr.error != null)
            {
                System.err.println("BLADE-DEEZER: Could not refresh token, Deezer error: " + sr.error.message);
                return false;
            }
            if(sr.access_token == null || Objects.equals(sr.access_token, ""))
            {
                System.err.println("BLADE-DEEZER: Could not refresh token, unknown Deezer error");
                return false;
            }

            ACCESS_TOKEN = sr.access_token;
            TOKEN_EXPIRES_IN = sr.expires_in;
            Source.saveSources();

            System.out.println("BLADE-DEEZER: Successfully refreshed access token (ACCESS_TOKEN=" + ACCESS_TOKEN + ")");
        }
        catch(IOException e)
        {
            System.err.println("BLADE-DEEZER: Could not refresh access token (IOException trying to obtain token)");
            return false;
        }

        return true;
    }


    @Override
    public void initSource()
    {
        if(status != SourceStatus.STATUS_NEED_INIT) return;

        status = SourceStatus.STATUS_CONNECTING;

        // refresh access token
        BladeApplication.obtainExecutorService().execute(() ->
        {
            refreshAccessTokenSync();
            status = SourceStatus.STATUS_READY;
        });
    }

    @Override
    public void synchronizeLibrary()
    {

    }

    @Override
    public Fragment getSettingsFragment()
    {
        return new DeezerSettingsFragment(this);
    }

    @Override
    public JsonObject saveToJSON()
    {
        return null;
    }

    @Override
    public void restoreFromJSON(JsonObject jsonObject)
    {

    }

    @Override
    public void explore(ExploreFragment view)
    {

    }

    @Override
    public void exploreSearch(String query, ExploreFragment view)
    {

    }

    @Override
    public void createPlaylist(String name, BladeApplication.Callback<Playlist> callback, Runnable failureCallback)
    {

    }
}
