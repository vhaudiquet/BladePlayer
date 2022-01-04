package v.blade.sources.spotify;

import android.content.Intent;
import android.os.Bundle;
import android.os.Looper;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.spotify.sdk.android.auth.AuthorizationClient;
import com.spotify.sdk.android.auth.AuthorizationRequest;
import com.spotify.sdk.android.auth.AuthorizationResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import kotlin.random.Random;
import okhttp3.Call;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import v.blade.BladeApplication;
import v.blade.R;
import v.blade.databinding.SettingsFragmentSpotifyBinding;
import v.blade.sources.Source;

/*
 * Spotify strategy :
 * - For access to library, we do Web API access using official AUTH lib + Retrofit
 * - For the player, we use librespot-java (the player part)
 * It would be nice to use librespot for everything, but i don't think it is possible to
 * use it 'as is' for web api access...
 */
public class Spotify extends Source
{
    public static final int NAME_RESOURCE = R.string.spotify;
    public static final int DESCRIPTION_RESOURCE = R.string.spotify_desc;
    public static final int IMAGE_RESOURCE = R.drawable.ic_spotify;

    //Spotify AUTH : We are using 'Authorization Code Flow' with 'PKCE extension'
    private static final String AUTH_TYPE = "Bearer ";
    private static final String CLIENT_ID = "048adc76814146e7bb049d89813bd6e0";
    protected static final String[] SCOPES = {"app-remote-control", "streaming", "playlist-modify-public", "playlist-modify-private", "playlist-read-private", "playlist-read-collaborative", "user-follow-modify", "user-follow-read", "user-library-modify", "user-library-read", "user-read-email", "user-read-private",
            "user-read-recently-played", "user-top-read", "user-read-playback-position", "user-read-playback-state", "user-modify-playback-state", "user-read-currently-playing"};
    private static final int SPOTIFY_REQUEST_CODE = 0x11;
    protected static final String REDIRECT_URI = "spotify-sdk://auth";

    //Spotify login informations
    private String account_name;

    @Override
    public int getImageResource()
    {
        return IMAGE_RESOURCE;
    }

    @Override
    public void synchronizeLibrary()
    {

    }

    @Override
    public Fragment getSettingsFragment()
    {
        return new Spotify.SettingsFragment(this);
    }

    public static class SettingsFragment extends Fragment
    {
        private final Spotify spotify;
        private SettingsFragmentSpotifyBinding binding;

        private String codeVerifier;

        private SettingsFragment(Spotify spotify)
        {
            super(R.layout.settings_fragment_spotify);
            this.spotify = spotify;
        }

        public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
        {
            binding = SettingsFragmentSpotifyBinding.inflate(inflater, container, false);

            //Set status text
            switch(spotify.getStatus())
            {
                case STATUS_DOWN:
                    binding.settingsSpotifyStatus.setText(R.string.source_down_desc);
                    break;
                case STATUS_NEED_INIT:
                    binding.settingsSpotifyStatus.setText(R.string.source_need_init_desc);
                    break;
                case STATUS_CONNECTING:
                    binding.settingsSpotifyStatus.setText(R.string.source_connecting_desc);
                    break;
                case STATUS_READY:
                    binding.settingsSpotifyStatus.setText(R.string.source_ready_desc);
                    break;
            }

            //Set account text
            if(spotify.account_name == null)
            {
                binding.settingsSpotifyAccount.setText(R.string.disconnected);
                binding.settingsSpotifyAccount.setTextColor(getResources().getColor(R.color.errorRed));
            }
            else
            {
                binding.settingsSpotifyAccount.setText(spotify.account_name);
                binding.settingsSpotifyAccount.setTextColor(getResources().getColor(R.color.okGreen));
            }

            //Set 'sign in' button action : call spotify auth
            binding.settingsSpotifySignIn.setOnClickListener(v ->
            {
                //Generate random code for PKCE
                int codeLen = Random.Default.nextInt(43, 128);

                byte leftLimit = 97; // letter 'a'
                byte rightLimit = 122; // letter 'z'
                byte[] randomCode = new byte[codeLen];
                for(int i = 0; i < codeLen; i++)
                    randomCode[i] = (byte) Random.Default.nextInt(leftLimit, rightLimit + 1);

                codeVerifier = new String(randomCode, StandardCharsets.US_ASCII);
                MessageDigest digest;
                try
                {
                    digest = MessageDigest.getInstance("SHA-256");
                }
                catch(NoSuchAlgorithmException e)
                {
                    Toast.makeText(getContext(), getString(R.string.auth_error) + " (Cannot calculate SHA256 Hash)", Toast.LENGTH_SHORT).show();
                    return;
                }

                digest.reset();
                byte[] code = digest.digest(randomCode);

                String base64code = Base64.encodeToString(code, Base64.URL_SAFE);
                //Remove trailing '=', '\n', ...
                int index;
                for(index = base64code.length() - 1; index >= 0; index--)
                {
                    if(base64code.charAt(index) != '='
                            && base64code.charAt(index) != '\n'
                            && base64code.charAt(index) != '\r'
                            && base64code.charAt(index) != ' ') break;
                }
                base64code = base64code.substring(0, index + 1);

                AuthorizationRequest request = new AuthorizationRequest.Builder(CLIENT_ID,
                        AuthorizationResponse.Type.CODE, REDIRECT_URI)
                        .setShowDialog(false).setScopes(SCOPES)
                        .setCustomParam("code_challenge_method", "S256")
                        .setCustomParam("code_challenge", base64code).build();
                AuthorizationClient.openLoginActivity(requireActivity(), SPOTIFY_REQUEST_CODE, request);
            });

            return binding.getRoot();
        }

        //We don't care about deprecation as this is just called from SettingsActivity ; could be changed
        @SuppressWarnings("deprecation")
        @Override
        public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data)
        {
            super.onActivityResult(requestCode, resultCode, data);

            if(requestCode != SPOTIFY_REQUEST_CODE) return;

            final AuthorizationResponse response = AuthorizationClient.getResponse(resultCode, data);
            if(response.getType() != AuthorizationResponse.Type.CODE) return;

            if(response.getError() != null && !response.getError().isEmpty())
            {
                Toast.makeText(getContext(), getString(R.string.auth_error) + " (" + response.getError() + ")", Toast.LENGTH_SHORT).show();
                return;
            }

            /* Authentication ok : we got code ; now we need to obtain access and refresh tokens */
            final String code = response.getCode();

            OkHttpClient client = new OkHttpClient();
            RequestBody body = new FormBody.Builder()
                    .add("grant_type", "authorization_code")
                    .add("code", code)
                    .add("redirect_uri", REDIRECT_URI)
                    .add("client_id", CLIENT_ID)
                    .add("code_verifier", codeVerifier)
                    .build();
            Request request = new Request.Builder().url("https://accounts.spotify.com/api/token")
                    .post(body).build();
            Call call = client.newCall(request);

            BladeApplication.obtainExecutorService().execute(() ->
            {
                //Prepare a looper so that we can Toast on error
                Looper.prepare();

                try
                {
                    Response postResponse = call.execute();
                    if(!postResponse.isSuccessful() || postResponse.code() != 200 || postResponse.body() == null)
                    {
                        //noinspection ConstantConditions
                        String responseBody = (postResponse.body() == null ? "Unknown error" : postResponse.body().string());
                        Toast.makeText(getContext(), getString(R.string.auth_error) + " (" + postResponse.code() + " : " + responseBody + ")", Toast.LENGTH_SHORT).show();
                        System.err.println("Spotify AUTH token error : " + postResponse.code() + " : " + responseBody);
                        return;
                    }

                    //TODO obtain token (GSON)... easy
                }
                catch(IOException e)
                {
                    Toast.makeText(getContext(), getString(R.string.auth_error) + " (IOException trying to obtain tokens)", Toast.LENGTH_SHORT).show();
                    return;
                }
            });
        }
    }
}
