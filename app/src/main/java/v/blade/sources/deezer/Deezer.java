package v.blade.sources.deezer;

import androidx.fragment.app.Fragment;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Objects;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import v.blade.BladeApplication;
import v.blade.BuildConfig;
import v.blade.R;
import v.blade.library.Library;
import v.blade.library.Playlist;
import v.blade.library.Song;
import v.blade.sources.Source;
import v.blade.ui.ExploreFragment;
import v.blade.ui.SettingsActivity;

public class Deezer extends Source
{
    private static final int DEEZER_IMAGE_LEVEL = 11;
    public static final int NAME_RESOURCE = R.string.deezer;
    public static final int DESCRIPTION_RESOURCE = R.string.deezer_desc;
    public static final int IMAGE_RESOURCE = R.drawable.ic_deezer;

    private static final String BASE_API_URL = "https://api.deezer.com/";
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

    protected String AUTH_STRING;

    private Retrofit retrofit;
    private DeezerService service;

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
            AUTH_STRING = AUTH_TYPE + ACCESS_TOKEN;
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

    private void notifyUiForStatus()
    {
        // Notify UI
        if(BladeApplication.currentActivity instanceof SettingsActivity)
        {
            SettingsActivity settingsActivity = (SettingsActivity) BladeApplication.currentActivity;
            settingsActivity.runOnUiThread(() ->
            {
                for(Fragment f : settingsActivity.getSupportFragmentManager().getFragments())
                {
                    if(f instanceof SettingsActivity.SourcesFragment)
                    {
                        SettingsActivity.SourcesFragment sf = (SettingsActivity.SourcesFragment) f;
                        sf.updateSourcesView();
                    }
                    else if(f instanceof DeezerSettingsFragment)
                    {
                        DeezerSettingsFragment df = (DeezerSettingsFragment) f;
                        df.refreshStatus();
                    }
                }
            });
        }
    }

    @Override
    public void initSource()
    {
        if(status != SourceStatus.STATUS_NEED_INIT) return;

        status = SourceStatus.STATUS_CONNECTING;
        notifyUiForStatus();
        System.out.println("BLADE-DEEZER: Initializing Deezer...");

        //build retrofit client
        retrofit = new Retrofit.Builder().baseUrl(BASE_API_URL).addConverterFactory(GsonConverterFactory.create()).build();
        service = retrofit.create(DeezerService.class);

        // refresh access token
        BladeApplication.obtainExecutorService().execute(() ->
        {
            if(refreshAccessTokenSync()) status = SourceStatus.STATUS_READY;
            else
            {
                status = SourceStatus.STATUS_NEED_INIT;
                notifyUiForStatus();
                return;
            }

            notifyUiForStatus();
            Source.saveSources();
            System.out.println("BLADE-DEEZER: Deezer initialized");
        });
    }

    @Override
    public void synchronizeLibrary()
    {
        System.out.println("BLADE-DEEZER: Syncing lib");

        try
        {
            /* Obtain user tracks */
            int tracksLeft;
            int trackIndex = 0;
            do
            {
                Call<DeezerService.UserTracksObject> userTracks =
                        service.getUserTracks(ACCESS_TOKEN, 50, trackIndex);

                Response<DeezerService.UserTracksObject> response =
                        userTracks.execute();

                if(response.code() != 200 || response.body() == null) break;

                DeezerService.UserTracksObject tracks = response.body();

                // Compute new track index, tracks left
                trackIndex += 50;
                tracksLeft = tracks.total - trackIndex;

                // Add each track
                for(DeezerService.UserTrackObject track : tracks.data)
                {
                    String[] artists = new String[1];
                    String[] artistsImages = new String[1];
                    artists[0] = track.artist.name;
                    artistsImages[0] = track.artist.picture_medium;

                    // TODO : Obtain album artists ?
                    String[] aartists = artists;
                    String[] aartistsImages = artistsImages;

                    Library.addSong(track.title, track.album.title, artists, this, track.id, aartists,
                            track.album.cover_medium,
                            1, //TODO : Obtain track rank
                            artistsImages, aartistsImages, track.album.cover_big, DEEZER_IMAGE_LEVEL);
                }
            } while(tracksLeft > 0);

            /* Obtain user albums */
            int albumsLeft;
            int albumsIndex = 0;
            do
            {
                Call<DeezerService.UserAlbumsObject> userAlbums =
                        service.getUserAlbums(ACCESS_TOKEN, 50, albumsIndex);

                Response<DeezerService.UserAlbumsObject> response =
                        userAlbums.execute();

                if(response.code() != 200 || response.body() == null) break;

                DeezerService.UserAlbumsObject albums = response.body();

                // Compute new album index, albums left
                albumsIndex += 50;
                albumsLeft = albums.total - albumsIndex;

                // Obtain each album tracks
                for(DeezerService.UserAlbumObject albumObject : albums.data)
                {
                    Call<DeezerService.AlbumObject> albumCall = service.getAlbum(albumObject.id);
                    Response<DeezerService.AlbumObject> albumResponse = albumCall.execute();
                    if(albumResponse.code() != 200 || albumResponse.body() == null) continue;

                    DeezerService.AlbumObject album = albumResponse.body();

                    //album artists
                    String[] aartists = new String[1];
                    String[] aartistsImages = new String[1];
                    aartists[0] = album.artist.name;
                    aartistsImages[0] = album.artist.picture_medium;

                    //add every song in album
                    int track_number = 0;
                    for(DeezerService.SimpleTrackObject track : album.tracks.data)
                    {
                        track_number++;

                        //song artists
                        String[] artists = new String[1];
                        String[] artistsImages = new String[1];
                        artists[0] = track.artist.name;

                        Library.addSong(track.title, album.title, artists, this, track.id, aartists,
                                album.cover_medium,
                                track_number,
                                artistsImages, aartistsImages, album.cover_big, DEEZER_IMAGE_LEVEL);
                    }
                }
            }
            while(albumsLeft > 0);

            /* Obtain user playlists */
            int playlistsLeft;
            int playlistIndex = 0;
            do
            {
                Call<DeezerService.UserPlaylistsObject> userPlaylists =
                        service.getUserPlaylists(ACCESS_TOKEN, 50, playlistIndex);

                Response<DeezerService.UserPlaylistsObject> response =
                        userPlaylists.execute();

                if(response.code() != 200 || response.body() == null) break;

                DeezerService.UserPlaylistsObject playlists = response.body();

                // Compute new album index, albums left
                playlistIndex += 50;
                playlistsLeft = playlists.total - playlistIndex;

                // Add each playlist
                for(DeezerService.UserPlaylistObject playlist : playlists.data)
                {
                    Call<DeezerService.PlaylistObject> playlistCall =
                            service.getPlaylist(playlist.id, ACCESS_TOKEN);
                    Response<DeezerService.PlaylistObject> playlistObjectResponse =
                            playlistCall.execute();

                    if(playlistObjectResponse.code() != 200 ||
                            playlistObjectResponse.body() == null) continue;

                    DeezerService.PlaylistObject p = playlistObjectResponse.body();

                    ArrayList<Song> songList = new ArrayList<Song>();

                    // Obtain every song in playlist
                    for(DeezerService.UserTrackObject track : p.tracks.data)
                    {
                        String[] artists = new String[1];
                        String[] artistsImages = new String[1];
                        artists[0] = track.artist.name;
                        artistsImages[0] = track.artist.picture_medium;

                        // TODO : Obtain album artists ?
                        String[] aartists = artists;
                        String[] aartistsImages = artistsImages;

                        Song song = Library.addSongHandle(track.title, track.album.title, artists, this, track.id, aartists,
                                track.album.cover_medium,
                                1, // TODO : obtain track number
                                artistsImages, aartistsImages, track.album.cover_big, DEEZER_IMAGE_LEVEL);
                        songList.add(song);
                    }

                    Library.addPlaylist(playlist.title, songList,
                            playlist.picture_medium,
                            (playlist.collaborative ? (BladeApplication.appContext.getString(R.string.collaborative) + " - ") : "") +
                                    "Deezer",//(playlist.creator.id.equals(user_id) ? "" : playlist.creator.name), // TODO keep creator id
                            this, playlist.id);
                }
            } while(playlistsLeft > 0);

            System.out.println("BLADE-DEEZER: Lib synced");
        }
        catch(IOException e)
        {
            System.err.println("BLADE-DEEZER: IOException while trying to sync library");
            e.printStackTrace();
        }
    }

    @Override
    public Fragment getSettingsFragment()
    {
        return new DeezerSettingsFragment(this);
    }

    @Override
    public JsonObject saveToJSON()
    {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("class", Deezer.class.getName());
        jsonObject.addProperty("account_name", account_name);
        jsonObject.addProperty("account_login", account_login);
        jsonObject.addProperty("account_password", account_password);

        return jsonObject;
    }

    @Override
    public void restoreFromJSON(JsonObject jsonObject)
    {
        JsonElement accountLoginJson = jsonObject.get("account_login");
        if(accountLoginJson != null) account_login = accountLoginJson.getAsString();
        else account_login = "null";

        JsonElement accountPasswordJson = jsonObject.get("account_password");
        if(accountPasswordJson != null) account_password = accountPasswordJson.getAsString();
        else account_password = "null";

        JsonElement accountNameJson = jsonObject.get("account_name");
        if(accountNameJson != null) account_name = accountNameJson.getAsString();
        else account_name = account_login;
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
