package v.blade.sources.spotify;

import android.content.Intent;
import android.os.Bundle;
import android.os.Process;
import android.util.ArrayMap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.spotify.sdk.android.auth.AuthorizationClient;
import com.spotify.sdk.android.auth.AuthorizationRequest;
import com.spotify.sdk.android.auth.AuthorizationResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;

import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import v.blade.BladeApplication;
import v.blade.BuildConfig;
import v.blade.R;
import v.blade.databinding.SettingsFragmentSpotifyBinding;
import v.blade.library.Library;
import v.blade.library.Playlist;
import v.blade.library.Song;
import v.blade.sources.Source;
import v.blade.sources.SourceInformation;
import v.blade.ui.ExploreFragment;
import v.blade.ui.SettingsActivity;
import xyz.gianlu.librespot.audio.decoders.AudioQuality;

/*
 * Spotify strategy :
 * - For access to library, we do Web API access using official AUTH lib + Retrofit
 * - For the player, we use librespot-java (the player part)
 * It would be nice to use librespot for everything, but i don't think it is possible to
 * use it 'as is' for web api access...
 */
//TODO : Spotify AUTH does not support PKCE if Spotify App is installed, i think ; we will need
// to go back to 'client secret' exchange, which is completely dumb, as then the secret is public
// (i have no backend) ; another solution would be to put the secret on valou3433.fr but yeah
public class Spotify extends Source
{
    public static final int NAME_RESOURCE = R.string.spotify;
    public static final int DESCRIPTION_RESOURCE = R.string.spotify_desc;
    public static final int IMAGE_RESOURCE = R.drawable.ic_spotify;
    private static final int CACHE_VERSION = 1;

    protected static final int SPOTIFY_IMAGE_LEVEL = 10;

    //Spotify AUTH : We are using 'Authorization Code Flow' with 'PKCE extension'
    private static final String BASE_API_URL = "https://api.spotify.com/v1/";
    private static final String AUTH_TYPE = "Bearer ";
    private static final String CLIENT_ID = BuildConfig.SPOTIFY_CLIENT_ID;
    //This should not be exposed, but i have no other choice, as the Spotify app does not seem to support PKCE
    private static final String CLIENT_SECRET = BuildConfig.SPOTIFY_CLIENT_SECRET;
    protected static final String[] SCOPES = {"app-remote-control", "streaming", "playlist-modify-public", "playlist-modify-private", "playlist-read-private", "playlist-read-collaborative", "user-follow-modify", "user-follow-read", "user-library-modify", "user-library-read", "user-read-email", "user-read-private",
            "user-read-recently-played", "user-top-read", "user-read-playback-position", "user-read-playback-state", "user-modify-playback-state", "user-read-currently-playing"};
    private static final int SPOTIFY_REQUEST_CODE = 0x11;
    protected static final String REDIRECT_URI = "spotify-sdk://auth";

    public AudioQuality spotifyAudioQuality = AudioQuality.HIGH;

    public Spotify()
    {
        super();
        this.name = BladeApplication.appContext.getString(NAME_RESOURCE);
        this.player = new SpotifyPlayer(this);
    }

    @SuppressWarnings({"FieldMayBeFinal", "unused"})
    private static class SpotifyTokenResponse
    {
        private String access_token = "";
        private String token_type = "";
        private int expires_in = -1;
        private String refresh_token = "";
        private String scope = "";

        public SpotifyTokenResponse()
        {
        }
    }

    //Spotify login information
    //Player login
    private String account_name; //i.e. username, retrieved by api
    private String account_login; //what the user uses to login (mail or username)
    private String account_password;
    private String user_id;

    //API login
    private String ACCESS_TOKEN;
    private String REFRESH_TOKEN;
    private int TOKEN_EXPIRES_IN;
    protected String AUTH_STRING;

    private Retrofit retrofit;
    protected SpotifyService service;

    @Override
    public int getImageResource()
    {
        return IMAGE_RESOURCE;
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
                    else if(f instanceof SettingsFragment)
                    {
                        SettingsFragment sf = (SettingsFragment) f;
                        sf.refreshStatus();
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

        //build retrofit client
        retrofit = new Retrofit.Builder().baseUrl(BASE_API_URL).addConverterFactory(GsonConverterFactory.create()).build();
        service = retrofit.create(SpotifyService.class);

        //refresh access token
        System.out.println("BLADE-SPOTIFY: Refresh token " + REFRESH_TOKEN);
        OkHttpClient client = new OkHttpClient();
        RequestBody requestBody = new FormBody.Builder()
                .add("grant_type", "refresh_token")
                .add("refresh_token", REFRESH_TOKEN)
                .add("client_id", CLIENT_ID)
                .add("client_secret", CLIENT_SECRET)
                .build();
        Request request = new Request.Builder().url("https://accounts.spotify.com/api/token")
                .post(requestBody).build();
        okhttp3.Call call = client.newCall(request);
        BladeApplication.obtainExecutorService().execute(() ->
        {
            Process.setThreadPriority(Process.THREAD_PRIORITY_DEFAULT);

            System.out.println("BLADE-SPOTIFY: Player is logging in...");
            //First init the player
            boolean login = ((SpotifyPlayer) player).login(account_login, account_password);
            if(!login)
            {
                //Toast.makeText(BladeApplication.appContext, BladeApplication.appContext.getString(R.string.init_error) + " " + BladeApplication.appContext.getString(NAME_RESOURCE) + " (Could not login)", Toast.LENGTH_SHORT).show();
                System.err.println("BLADE-SPOTIFY: " + BladeApplication.appContext.getString(R.string.init_error) + " " + BladeApplication.appContext.getString(NAME_RESOURCE) + " (Could not login)");
                status = SourceStatus.STATUS_NEED_INIT;
                return;
            }
            System.out.println("BLADE-SPOTIFY: Player logged in, initializing...");
            player.init();

            //Then init
            try
            {
                System.out.println("BLADE-SPOTIFY: Initializing API, refreshing token...");

                okhttp3.Response response = call.execute();
                if(!response.isSuccessful() || response.code() != 200 || response.body() == null)
                {
                    String responseBody = response.body() == null ? "Unknown error" : response.body().string();
                    //Toast.makeText(BladeApplication.appContext, BladeApplication.appContext.getString(R.string.init_error) + " " + BladeApplication.appContext.getString(NAME_RESOURCE) + " (" + response.code() + " : " + responseBody + ")", Toast.LENGTH_SHORT).show();
                    System.err.println("BLADE-SPOTIFY: " + BladeApplication.appContext.getString(R.string.init_error) + " " + BladeApplication.appContext.getString(NAME_RESOURCE) + " (" + response.code() + " : " + responseBody + ")");
                    status = SourceStatus.STATUS_NEED_INIT;
                    return;
                }

                Gson gson = new Gson();
                String rstring = response.body().string();
                SpotifyTokenResponse sr = gson.fromJson(rstring, SpotifyTokenResponse.class);
                if(sr == null)
                {
                    //Toast.makeText(BladeApplication.appContext, BladeApplication.appContext.getString(R.string.init_error) + " " + BladeApplication.appContext.getString(NAME_RESOURCE) + " (Could not parse JSON Token)", Toast.LENGTH_SHORT).show();
                    System.err.println("BLADE-SPOTIFY: " + BladeApplication.appContext.getString(R.string.init_error) + " " + BladeApplication.appContext.getString(NAME_RESOURCE) + " (Could not parse JSON Token)");
                    status = SourceStatus.STATUS_NEED_INIT;
                    return;
                }

                ACCESS_TOKEN = sr.access_token;
                TOKEN_EXPIRES_IN = sr.expires_in;

                if(sr.refresh_token != null && !sr.refresh_token.equals(""))
                    REFRESH_TOKEN = sr.refresh_token;

                AUTH_STRING = AUTH_TYPE + ACCESS_TOKEN;

                status = SourceStatus.STATUS_READY;

                Source.saveSources();
                System.out.println("BLADE-SPOTIFY: Spotify initialized");

                // Notify UI
                notifyUiForStatus();
            }
            catch(IOException e)
            {
                status = SourceStatus.STATUS_NEED_INIT;
                //Toast.makeText(BladeApplication.appContext, BladeApplication.appContext.getString(R.string.init_error) + " " + BladeApplication.appContext.getString(NAME_RESOURCE) + " (IOException trying to obtain token)", Toast.LENGTH_SHORT).show();
                System.err.println("BLADE-SPOTIFY: " + BladeApplication.appContext.getString(R.string.init_error) + " " + BladeApplication.appContext.getString(NAME_RESOURCE) + " (IOException trying to obtain token)");
            }
        });
    }

    protected void refreshAccessTokenSync()
    {
        //build retrofit client
        retrofit = new Retrofit.Builder().baseUrl(BASE_API_URL).addConverterFactory(GsonConverterFactory.create()).build();
        service = retrofit.create(SpotifyService.class);

        //refresh access token
        System.out.println("BLADE-SPOTIFY: Refresh token " + REFRESH_TOKEN);
        OkHttpClient client = new OkHttpClient();
        RequestBody requestBody = new FormBody.Builder()
                .add("grant_type", "refresh_token")
                .add("refresh_token", REFRESH_TOKEN)
                .add("client_id", CLIENT_ID)
                .add("client_secret", CLIENT_SECRET)
                .build();
        Request request = new Request.Builder().url("https://accounts.spotify.com/api/token")
                .post(requestBody).build();
        okhttp3.Call call = client.newCall(request);
        try
        {
            okhttp3.Response response = call.execute();
            if(!response.isSuccessful() || response.code() != 200 || response.body() == null)
            {
                String responseBody = response.body() == null ? "Unknown error" : response.body().string();
                System.err.println("BLADE-SPOTIFY: Could not refresh token" + " (" + response.code() + " : " + responseBody + ")");
                return;
            }

            Gson gson = new Gson();
            String rstring = response.body().string();
            SpotifyTokenResponse sr = gson.fromJson(rstring, SpotifyTokenResponse.class);
            if(sr == null)
            {
                System.err.println("BLADE-SPOTIFY: Could not refresh token" + " (Could not parse JSON Token)");
                return;
            }

            ACCESS_TOKEN = sr.access_token;
            TOKEN_EXPIRES_IN = sr.expires_in;

            if(sr.refresh_token != null && !sr.refresh_token.equals(""))
                REFRESH_TOKEN = sr.refresh_token;

            AUTH_STRING = AUTH_TYPE + ACCESS_TOKEN;

            Source.saveSources();
        }
        catch(IOException e)
        {
            status = SourceStatus.STATUS_NEED_INIT;
            System.err.println("BLADE-SPOTIFY: Could not refresh access token (IOException trying to obtain token)");
        }
    }

    private static int computeTrackNumber(int discNumber, int trackNumber)
    {
        return 100 * discNumber + trackNumber;
    }

    @Override
    public void synchronizeLibrary()
    {
        try
        {
            System.out.println("BLADE-SPOTIFY: Syncing lib");
            /* Obtain user tracks */
            int tracksLeft;
            int tracksIndex = 0;
            do
            {
                Call<SpotifyService.PagingObject<SpotifyService.SavedTrackObject>> userTracks =
                        service.getUserSavedTracks(AUTH_STRING, 50, tracksIndex * 50);
                Response<SpotifyService.PagingObject<SpotifyService.SavedTrackObject>> response =
                        userTracks.execute();

                if(response.code() == 401)
                {
                    //Expired token
                    System.err.println("BLADE-SPOTIFY: Expired token while syncing library, refreshing...");
                    refreshAccessTokenSync();
                    synchronizeLibrary();
                    return;
                }

                if(response.code() != 200 || response.body() == null)
                {
                    System.err.println("BLADE-SPOTIFY: Error while syncing library: non-200 error code " + response.code());
                    System.err.println("BLADE-SPOTIFY: Response error body: " + response.errorBody().string());
                    break;
                }
                SpotifyService.PagingObject<SpotifyService.SavedTrackObject> trackPaging = response.body();

                for(SpotifyService.SavedTrackObject savedTrack : trackPaging.items)
                {
                    SpotifyService.TrackObject track = savedTrack.track;
                    if(track.album == null || track.artists == null || track.album.images.length == 0)
                        continue; //TODO check ?

                    //album artists
                    String[] aartists = new String[track.album.artists.length];
                    String[] aartistsImages = new String[track.album.artists.length];
                    for(int j = 0; j < track.album.artists.length; j++)
                    {
                        aartists[j] = track.album.artists[j].name;
                    }

                    //song artists
                    String[] artists = new String[track.artists.length];
                    String[] artistsImages = new String[track.artists.length];
                    for(int j = 0; j < track.artists.length; j++)
                    {
                        artists[j] = track.artists[j].name;
                        //artistsImages[j] = track.artists[j].images //TODO artists images ?
                    }

                    Library.addSong(track.name, track.album.name, artists, this, track.id, aartists,
                            track.album.images[track.album.images.length - 2].url,
                            computeTrackNumber(track.disc_number, track.track_number),
                            artistsImages, aartistsImages, track.album.images[0].url, SPOTIFY_IMAGE_LEVEL);
                }

                tracksLeft = trackPaging.total - 50 * (tracksIndex + 1);
                tracksIndex++;
            }
            while(tracksLeft > 0);

            /* Obtain user albums */
            int albumsLeft;
            int albumIndex = 0;
            do
            {
                Call<SpotifyService.PagingObject<SpotifyService.SavedAlbumObject>> userAlbums =
                        service.getUserSavedAlbums(AUTH_STRING, 50, albumIndex * 50);
                Response<SpotifyService.PagingObject<SpotifyService.SavedAlbumObject>> response =
                        userAlbums.execute();

                if(response.code() != 200 || response.body() == null) break;
                SpotifyService.PagingObject<SpotifyService.SavedAlbumObject> albumPaging = response.body();

                for(SpotifyService.SavedAlbumObject savedAlbum : albumPaging.items)
                {
                    SpotifyService.AlbumObject album = savedAlbum.album;
                    if(album.artists == null || album.tracks == null || album.images.length == 0)
                        continue;

                    //album artists
                    String[] aartists = new String[album.artists.length];
                    String[] aartistsImages = new String[album.artists.length];
                    for(int j = 0; j < album.artists.length; j++)
                    {
                        aartists[j] = album.artists[j].name;
                    }

                    //add every song in album
                    for(SpotifyService.SimplifiedTrackObject track : album.tracks.items)
                    {
                        //song artists
                        String[] artists = new String[track.artists.length];
                        String[] artistsImages = new String[track.artists.length];
                        for(int j = 0; j < track.artists.length; j++)
                        {
                            artists[j] = track.artists[j].name;
                            //artistsImages[j] = track.artists[j].images //TODO artists images ?
                        }

                        Library.addSong(track.name, album.name, artists, this, track.id, aartists,
                                album.images[album.images.length - 2].url,
                                computeTrackNumber(track.disc_number, track.track_number),
                                artistsImages, aartistsImages, album.images[0].url, SPOTIFY_IMAGE_LEVEL);
                    }
                }

                albumsLeft = albumPaging.total - 50 * (albumIndex + 1);
                albumIndex++;
            }
            while(albumsLeft > 0);

            /* Obtain user playlists */
            int playlistsLeft;
            int playlistIndex = 0;
            do
            {
                Call<SpotifyService.PagingObject<SpotifyService.SimplifiedPlaylistObject>> userPlaylists =
                        service.getListOfCurrentUserPlaylists(AUTH_STRING, 50, playlistIndex * 50);
                Response<SpotifyService.PagingObject<SpotifyService.SimplifiedPlaylistObject>> response =
                        userPlaylists.execute();

                if(response.code() != 200 || response.body() == null) break;
                SpotifyService.PagingObject<SpotifyService.SimplifiedPlaylistObject> playlistPaging = response.body();

                for(SpotifyService.SimplifiedPlaylistObject playlist : playlistPaging.items)
                {
                    //Obtain song list
                    ArrayList<Song> songList = new ArrayList<>();

                    int songsLeft;
                    int songIndex = 0;
                    do
                    {
                        Call<SpotifyService.PagingObject<SpotifyService.PlaylistTrackObject>> playlistTracks =
                                service.getPlaylistItems(AUTH_STRING, playlist.id, 100, songIndex * 100);
                        Response<SpotifyService.PagingObject<SpotifyService.PlaylistTrackObject>> response2 =
                                playlistTracks.execute();

                        if(response2.code() != 200 || response2.body() == null) break;
                        SpotifyService.PagingObject<SpotifyService.PlaylistTrackObject> songsPaging = response2.body();

                        for(SpotifyService.PlaylistTrackObject playlistTrack : songsPaging.items)
                        {
                            SpotifyService.TrackObject track = playlistTrack.track;
                            if(track.album == null || track.artists == null || track.album.images.length == 0)
                                continue;

                            //album artists
                            String[] aartists = new String[track.album.artists.length];
                            String[] aartistsImages = new String[track.album.artists.length];
                            for(int j = 0; j < track.album.artists.length; j++)
                            {
                                aartists[j] = track.album.artists[j].name;
                            }

                            //song artists
                            String[] artists = new String[track.artists.length];
                            String[] artistsImages = new String[track.artists.length];
                            for(int j = 0; j < track.artists.length; j++)
                            {
                                artists[j] = track.artists[j].name;
                                //artistsImages[j] = track.artists[j].images //TODO artists images ?
                            }

                            Song song = Library.addSongHandle(track.name, track.album.name, artists, this, track.id, aartists,
                                    track.album.images[track.album.images.length - 2].url,
                                    computeTrackNumber(track.disc_number, track.track_number),
                                    artistsImages, aartistsImages, track.album.images[0].url, SPOTIFY_IMAGE_LEVEL);
                            songList.add(song);
                        }

                        songsLeft = songsPaging.total - 100 * (songIndex + 1);
                        songIndex++;
                    }
                    while(songsLeft > 0);

                    Library.addPlaylist(playlist.name, songList,
                            playlist.images.length == 0 ? null :
                                    (playlist.images[0] == null ? null : playlist.images[0].url),
                            (playlist.collaborative ? (BladeApplication.appContext.getString(R.string.collaborative) + " - ") : "") +
                                    (playlist.owner.id.equals(user_id) ? "" : playlist.owner.display_name),
                            this, playlist.id);
                }

                playlistsLeft = playlistPaging.total - 50 * (playlistIndex + 1);
                playlistIndex++;
            }
            while(playlistsLeft > 0);
        }
        catch(IOException e)
        {
            System.err.println("BLADE-SPOTIFY: Error while syncing library: " + e.getMessage());
            e.printStackTrace();
        }

        System.out.println("BLADE-SPOTIFY: Lib sync done");
    }

    @Override
    public Fragment getSettingsFragment()
    {
        return new Spotify.SettingsFragment(this);
    }

    @Override
    public JsonObject saveToJSON()
    {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("class", Spotify.class.getName());
        jsonObject.addProperty("account_name", account_name);
        jsonObject.addProperty("refresh_token", REFRESH_TOKEN);
        jsonObject.addProperty("cache_version", CACHE_VERSION);
        jsonObject.addProperty("account_login", account_login);
        jsonObject.addProperty("account_password", account_password);
        jsonObject.addProperty("user_id", user_id);

        return jsonObject;
    }

    @Override
    public void restoreFromJSON(JsonObject jsonObject)
    {
        int cache_version = jsonObject.get("cache_version") == null ? 0 : jsonObject.get("cache_version").getAsInt();
        if(cache_version > CACHE_VERSION)
            System.err.println("Spotify cached source with version greater than current... Trying to read with old way.");

        JsonElement accountLoginJson = jsonObject.get("account_login");
        if(accountLoginJson != null) account_login = accountLoginJson.getAsString();
        else account_login = "null";

        JsonElement accountPasswordJson = jsonObject.get("account_password");
        if(accountPasswordJson != null) account_password = accountPasswordJson.getAsString();
        else account_password = "null";

        JsonElement accountNameJson = jsonObject.get("account_name");
        if(accountNameJson != null) account_name = accountNameJson.getAsString();
        else account_name = account_login;

        JsonElement refreshTokenJson = jsonObject.get("refresh_token");
        if(refreshTokenJson != null) REFRESH_TOKEN = refreshTokenJson.getAsString();
        else status = SourceStatus.STATUS_DOWN;

        JsonElement userIdJson = jsonObject.get("user_id");
        if(userIdJson != null) user_id = userIdJson.getAsString();
        else status = SourceStatus.STATUS_DOWN;
    }

    @Override
    public void explore(ExploreFragment view)
    {
        BladeApplication.obtainExecutorService().execute(() ->
        {
            Process.setThreadPriority(Process.THREAD_PRIORITY_FOREGROUND);

            Call<SpotifyService.FeaturedPlaylistsResult> call =
                    service.getFeaturedPlaylists(AUTH_STRING);
            try
            {
                Response<SpotifyService.FeaturedPlaylistsResult> response =
                        call.execute();

                if(response.code() == 401)
                {
                    //Expired token
                    refreshAccessTokenSync();
                    explore(view);
                    return;
                }

                SpotifyService.FeaturedPlaylistsResult r = response.body();
                if(response.code() != 200 || r == null || r.playlists == null)
                {
                    System.err.println("BLADE-SPOTIFY: Could not explore");
                    view.requireActivity().runOnUiThread(() ->
                            Toast.makeText(view.requireContext(),
                                    view.getString(R.string.could_not_show_explore),
                                    Toast.LENGTH_SHORT).show());
                    view.current = null;
                    return;
                }

                SpotifyExploreAdapter adapter = new SpotifyExploreAdapter(view);
                adapter.currentPlaylists = r.playlists;
                view.requireActivity().runOnUiThread(() ->
                        view.updateContent(adapter, getName(), true));
            }
            catch(IOException e)
            {
                view.requireActivity().runOnUiThread(() ->
                        Toast.makeText(view.requireContext(),
                                view.getString(R.string.could_not_show_explore),
                                Toast.LENGTH_SHORT).show());
                view.current = null;
            }
        });
    }

    @Override
    public void exploreSearch(String query, ExploreFragment view)
    {
        BladeApplication.obtainExecutorService().execute(() ->
        {
            Process.setThreadPriority(Process.THREAD_PRIORITY_FOREGROUND);

            //NOTE : for now we limit to 10 search results ; it seems ok (we could go to 50 but it is a lot...)
            Call<SpotifyService.SearchResult> call = service.search(AUTH_STRING, query, "track,artist,album", 10);
            try
            {
                Response<SpotifyService.SearchResult> response = call.execute();

                if(response.code() == 401)
                {
                    //Expired token
                    refreshAccessTokenSync();
                    exploreSearch(query, view);
                    return;
                }

                SpotifyService.SearchResult r = response.body();
                if(response.code() != 200 || r == null)
                {
                    System.err.println("BLADE-SPOTIFY: Could not search " + query);
                    view.requireActivity().runOnUiThread(() -> Toast.makeText(view.requireContext(), view.getString(R.string.could_not_search_for, query), Toast.LENGTH_SHORT).show());
                    return;
                }

                view.requireActivity().runOnUiThread(() ->
                        view.updateContent(new SpotifyExploreAdapter(r, view), query, true));
            }
            catch(IOException e)
            {
                view.requireActivity().runOnUiThread(() -> Toast.makeText(view.requireContext(), view.getString(R.string.could_not_search_for, query), Toast.LENGTH_SHORT).show());
            }
        });
    }

    @Override
    public void addSongToPlaylist(Song song, Playlist playlist, Runnable callback, Runnable failureCallback)
    {
        BladeApplication.obtainExecutorService().execute(() ->
        {
            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);

            String id = null;
            for(SourceInformation s : song.getSources())
            {
                if(s.source == this)
                {
                    id = (String) s.id;
                    break;
                }
            }

            //Add track to playlist on Spotify
            Call<SpotifyService.PlaylistAddResponse> call =
                    service.appendTrackToPlaylist(AUTH_STRING, (String) playlist.getSource().id, "spotify:track:" + id);
            try
            {
                Response<SpotifyService.PlaylistAddResponse> response = call.execute();

                if(response.code() == 401)
                {
                    //Expired token
                    refreshAccessTokenSync();
                    addSongToPlaylist(song, playlist, callback, failureCallback);
                    return;
                }

                if(response.code() != 201)
                {
                    System.err.println("BLADE-SPOTIFY: Could not add " + song.getName() + " to playlist " + playlist.getName() + " : " + response.code());
                    failureCallback.run();
                    return;
                }
            }
            catch(IOException e)
            {
                failureCallback.run();
                return;
            }

            super.addSongToPlaylist(song, playlist, callback, failureCallback);
        });
    }

    @Override
    public void createPlaylist(String name, BladeApplication.Callback<Playlist> callback, Runnable failureCallback)
    {
        BladeApplication.obtainExecutorService().execute(() ->
        {
            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);

            Map<String, Object> jsonParams = new ArrayMap<>();
            jsonParams.put("name", name);
            JSONObject jsonBody = new JSONObject(jsonParams);
            System.out.println("json: " + jsonBody);
            RequestBody body = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), jsonBody.toString());
            Call<SpotifyService.SimplifiedPlaylistObject> call = service.createPlaylist(AUTH_STRING, user_id, body);

            try
            {
                Response<SpotifyService.SimplifiedPlaylistObject> response = call.execute();
                SpotifyService.SimplifiedPlaylistObject r = response.body();
                if(response.code() == 401)
                {
                    //Expired token
                    refreshAccessTokenSync();
                    createPlaylist(name, callback, failureCallback);
                    return;
                }

                if(response.code() != 201 || r == null)
                {
                    System.err.println("BLADE-SPOTIFY: Could not create playlist " + name + " : " + response.code());
                    failureCallback.run();
                    return;
                }

                //Create playlist locally
                Playlist playlist = Library.addPlaylist(name, new ArrayList<>(), null, "", this, r.id);

                //Save library
                Library.save();
                saveSources();

                //Run callback
                callback.run(playlist);
            }
            catch(IOException e)
            {
                failureCallback.run();
            }
        });
    }

    @Override
    public void removePlaylist(Playlist playlist, Runnable callback, Runnable failureCallback)
    {
        BladeApplication.obtainExecutorService().execute(() ->
        {
            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);

            Call<Void> call = service.unfollowPlaylist(AUTH_STRING, (String) playlist.getSource().id);

            try
            {
                Response<Void> response = call.execute();
                if(response.code() == 401)
                {
                    //Expired token
                    refreshAccessTokenSync();
                    removePlaylist(playlist, callback, failureCallback);
                    return;
                }
                if(response.code() != 200)
                {
                    System.err.println("BLADE-SPOTIFY: Could not delete playlist " + playlist.getName() + " : " + response.code());
                    failureCallback.run();
                    return;
                }

                super.removePlaylist(playlist, callback, failureCallback);
            }
            catch(IOException e)
            {
                failureCallback.run();
            }
        });
    }

    @Override
    public void addToLibrary(Song song, Runnable callback, Runnable failureCallback)
    {
        BladeApplication.obtainExecutorService().execute(() ->
        {
            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);

            //Get song source
            SourceInformation current = null;
            for(SourceInformation s : song.getSources())
                if(s.source == this) current = s;

            if(current == null)
            {
                failureCallback.run();
                return;
            }

            Call<Void> call = service.saveTrack(AUTH_STRING, (String) current.id);

            try
            {
                Response<Void> response = call.execute();
                if(response.code() == 401)
                {
                    //Expired token
                    refreshAccessTokenSync();
                    addToLibrary(song, callback, failureCallback);
                    return;
                }
                if(response.code() != 200)
                {
                    System.err.println("BLADE-SPOTIFY: Could not save song " + song.getName() + " : " + response.code());
                    failureCallback.run();
                    return;
                }

                super.addToLibrary(song, callback, failureCallback);
            }
            catch(IOException e)
            {
                failureCallback.run();
            }
        });
    }

    @Override
    public void removeFromLibrary(Song song, Runnable callback, Runnable failureCallback)
    {
        BladeApplication.obtainExecutorService().execute(() ->
        {
            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);

            //Get song source
            SourceInformation current = null;
            for(SourceInformation s : song.getSources())
                if(s.source == this) current = s;

            if(current == null)
            {
                failureCallback.run();
                return;
            }

            Call<Void> call = service.removeTrack(AUTH_STRING, (String) current.id);

            try
            {
                Response<Void> response = call.execute();
                if(response.code() == 401)
                {
                    //Expired token
                    refreshAccessTokenSync();
                    removeFromLibrary(song, callback, failureCallback);
                    return;
                }
                if(response.code() != 200)
                {
                    System.err.println("BLADE-SPOTIFY: Could not remove song " + song.getName() + " : " + response.code());
                    failureCallback.run();
                    return;
                }

                super.removeFromLibrary(song, callback, failureCallback);
            }
            catch(IOException e)
            {
                failureCallback.run();
            }
        });
    }

    @Override
    public void removeFromPlaylist(Song song, Playlist playlist, Runnable callback, Runnable failureCallback)
    {
        BladeApplication.obtainExecutorService().execute(() ->
        {
            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);

            //Get song source
            SourceInformation current = null;
            for(SourceInformation s : song.getSources())
                if(s.source == this) current = s;

            if(current == null)
            {
                failureCallback.run();
                return;
            }

            try
            {
                //Generate request body
                JSONObject track = new JSONObject();
                track.put("uri", "spotify:track:" + current.id);
                JSONArray array = new JSONArray();
                array.put(track);
                Map<String, Object> jsonParams = new ArrayMap<>();
                jsonParams.put("tracks", array);
                JSONObject jsonBody = new JSONObject(jsonParams);
                System.out.println("json: " + jsonBody);
                RequestBody body = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), jsonBody.toString());
                Call<SpotifyService.PlaylistAddResponse> call = service.removePlaylistItem(AUTH_STRING, (String) playlist.getSource().id, body);

                Response<SpotifyService.PlaylistAddResponse> response = call.execute();
                if(response.code() == 401)
                {
                    //Expired token
                    refreshAccessTokenSync();
                    removeFromPlaylist(song, playlist, callback, failureCallback);
                    return;
                }
                if(response.code() != 200)
                {
                    System.err.println("BLADE-SPOTIFY: Could not remove " + song.getName() + " from playlist " + playlist.getName() + " : " + response.code());
                    failureCallback.run();
                    return;
                }

                super.removeFromPlaylist(song, playlist, callback, failureCallback);
            }
            catch(IOException | JSONException e)
            {
                failureCallback.run();
            }
        });
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

        public void refreshStatus()
        {
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
        }

        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
        {
            binding = SettingsFragmentSpotifyBinding.inflate(inflater, container, false);

            //Set status text
            refreshStatus();

            //Set account text
            if(spotify.account_name == null)
            {
                binding.settingsSpotifyAccount.setText(R.string.disconnected);
                binding.settingsSpotifyAccount.setTextColor(getResources().getColor(R.color.errorRed));

                // Hide audio quality and force init buttons
                binding.settingsSpotifyAudioQualityLayout.setVisibility(View.GONE);
                binding.settingsSpotifyInit.setVisibility(View.GONE);
            }
            else
            {
                binding.settingsSpotifyAccount.setText(spotify.account_name);
                binding.settingsSpotifyAccount.setTextColor(getResources().getColor(R.color.okGreen));

                // Hide login buttons (we are already connected)
                binding.settingsSpotifyUser.setVisibility(View.GONE);
                binding.settingsSpotifyPassword.setVisibility(View.GONE);
                binding.settingsSpotifySignIn.setVisibility(View.GONE);
                binding.settingsSpotifyCredentialsTwice.setVisibility(View.GONE);
            }

            //Set 'sign in' button action : call spotify auth
            binding.settingsSpotifySignIn.setOnClickListener(v ->
                    BladeApplication.obtainExecutorService().execute(() ->
                    {
                        Process.setThreadPriority(Process.THREAD_PRIORITY_DEFAULT);

                        //Try to login player
                        String userName = binding.settingsSpotifyUser.getText().toString();
                        String userPass = binding.settingsSpotifyPassword.getText().toString();

                        boolean login = ((SpotifyPlayer) spotify.getPlayer()).login(userName, userPass);
                        if(!login)
                        {
                            requireActivity().runOnUiThread(() ->
                                    Toast.makeText(getContext(), getString(R.string.auth_error) + " (Could not log in)", Toast.LENGTH_SHORT).show());
                            return;
                        }

                        spotify.getPlayer().init();
                        spotify.account_login = userName;
                        spotify.account_password = userPass;

                    /* PKCE: not supported by Spotify app
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
                    base64code = base64code.substring(0, index + 1);*/

                        AuthorizationRequest request = new AuthorizationRequest.Builder(CLIENT_ID,
                                AuthorizationResponse.Type.CODE, REDIRECT_URI)
                                .setShowDialog(false).setScopes(SCOPES)
                                //.setCustomParam("code_challenge_method", "S256")
                                //.setCustomParam("code_challenge", base64code)
                                .build();
                        AuthorizationClient.openLoginActivity(requireActivity(), SPOTIFY_REQUEST_CODE, request);
                    }));

            // Set 'audio quality' content + action
            ArrayAdapter<CharSequence> audioQualityAdapter = ArrayAdapter.createFromResource(this.requireActivity(), R.array.spotify_audio_quality, android.R.layout.simple_spinner_item);
            audioQualityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            binding.settingsSpotifyAudioQuality.setAdapter(audioQualityAdapter);
            switch(spotify.spotifyAudioQuality)
            {
                case NORMAL:
                    binding.settingsSpotifyAudioQuality.setSelection(0);
                    break;
                case HIGH:
                    binding.settingsSpotifyAudioQuality.setSelection(1);
                    break;
                case VERY_HIGH:
                    binding.settingsSpotifyAudioQuality.setSelection(2);
                    break;
            }
            binding.settingsSpotifyAudioQuality.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
            {
                @Override
                public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l)
                {
                    switch(i)
                    {
                        case 0:
                            spotify.spotifyAudioQuality = AudioQuality.NORMAL;
                            break;
                        case 1:
                            spotify.spotifyAudioQuality = AudioQuality.HIGH;
                            break;
                        case 2:
                            spotify.spotifyAudioQuality = AudioQuality.VERY_HIGH;
                            break;
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> adapterView)
                {
                }
            });

            binding.settingsSpotifyInit.setOnClickListener(view ->
            {
                spotify.status = SourceStatus.STATUS_NEED_INIT;
                spotify.initSource();
            });

            binding.settingsSpotifyRemove.setOnClickListener(view ->
            {
                Source.SOURCES.remove(spotify);
                requireActivity().onBackPressed();
                Toast.makeText(BladeApplication.appContext, R.string.please_sync_to_apply, Toast.LENGTH_LONG).show();
                //this is 'scheduleSave' after library sync
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
                System.out.println("BLADE-SPOTIFY: " + getString(R.string.auth_error) + " (" + response.getError() + ")");
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(getContext(), getString(R.string.auth_error) + " (" + response.getError() + ")", Toast.LENGTH_SHORT).show());
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
                    .add("client_secret", CLIENT_SECRET)
                    //.add("code_verifier", codeVerifier)
                    .build();
            Request request = new Request.Builder().url("https://accounts.spotify.com/api/token")
                    .post(body).build();
            okhttp3.Call call = client.newCall(request);

            BladeApplication.obtainExecutorService().execute(() ->
            {
                Process.setThreadPriority(Process.THREAD_PRIORITY_DEFAULT);

                try
                {
                    okhttp3.Response postResponse = call.execute();
                    if(!postResponse.isSuccessful() || postResponse.code() != 200 || postResponse.body() == null)
                    {
                        String responseBody = (postResponse.body() == null ? "Unknown error" : postResponse.body().string());
                        requireActivity().runOnUiThread(() ->
                                Toast.makeText(getContext(), getString(R.string.auth_error) + " (" + postResponse.code() + " : " + responseBody + ")", Toast.LENGTH_SHORT).show());
                        System.err.println("BLADE-SPOTIFY: Spotify AUTH token error : " + postResponse.code() + " : " + responseBody);
                        return;
                    }

                    Gson gson = new Gson();
                    String rstring = Objects.requireNonNull(postResponse.body()).string();
                    SpotifyTokenResponse sr = gson.fromJson(rstring, SpotifyTokenResponse.class);
                    if(sr == null)
                    {
                        System.out.println("BLADE-SPOTIFY: " + getString(R.string.auth_error) + " (Could not parse JSON Token)");
                        requireActivity().runOnUiThread(() ->
                                Toast.makeText(getContext(), getString(R.string.auth_error) + " (Could not parse JSON Token)", Toast.LENGTH_SHORT).show());
                        return;
                    }

                    //set token
                    spotify.ACCESS_TOKEN = sr.access_token;
                    spotify.REFRESH_TOKEN = sr.refresh_token;
                    spotify.TOKEN_EXPIRES_IN = sr.expires_in;
                    spotify.AUTH_STRING = AUTH_TYPE + spotify.ACCESS_TOKEN;

                    //init
                    spotify.retrofit = new Retrofit.Builder().baseUrl(BASE_API_URL).addConverterFactory(GsonConverterFactory.create()).build();
                    spotify.service = spotify.retrofit.create(SpotifyService.class);

                    //obtain account name and id
                    System.out.println("BLADE-SPOTIFY: AUTH_STRING=" + spotify.AUTH_STRING);
                    Response<SpotifyService.UserInformationObject> userResponse = spotify.service.getUser(spotify.AUTH_STRING).execute();
                    SpotifyService.UserInformationObject user = userResponse.body();
                    if(user == null)
                    {
                        //noinspection ConstantConditions
                        System.out.println("BLADE-SPOTIFY: " + getString(R.string.auth_error) + " (Could not obtain user ID : " + userResponse.code() + " : " + userResponse.errorBody().string() + ")");
                        requireActivity().runOnUiThread(() ->
                                Toast.makeText(getContext(), getString(R.string.auth_error) + " (Could not obtain user ID)", Toast.LENGTH_SHORT).show());
                        return;
                    }
                    if(user.display_name == null || user.id == null)
                    {
                        System.out.println("BLADE-SPOTIFY: " + getString(R.string.auth_error) + " (Could user ID null : " + userResponse.code() + ")");
                        requireActivity().runOnUiThread(() ->
                                Toast.makeText(getContext(), getString(R.string.auth_error) + " (User ID is null ???)", Toast.LENGTH_SHORT).show());
                        return;
                    }

                    spotify.account_name = user.display_name;
                    spotify.user_id = user.id;

                    //Re-set status and account textboxes
                    spotify.status = SourceStatus.STATUS_READY;
                    requireActivity().runOnUiThread(() ->
                    {
                        binding.settingsSpotifyStatus.setText(R.string.source_ready_desc);
                        binding.settingsSpotifyAccount.setText(user.display_name);
                        binding.settingsSpotifyAccount.setTextColor(getResources().getColor(R.color.okGreen));

                        // Hide login buttons (we are already connected)
                        binding.settingsSpotifyUser.setVisibility(View.GONE);
                        binding.settingsSpotifyPassword.setVisibility(View.GONE);
                        binding.settingsSpotifySignIn.setVisibility(View.GONE);
                        binding.settingsSpotifyCredentialsTwice.setVisibility(View.GONE);

                        // Show audio quality and force init buttons
                        binding.settingsSpotifyAudioQualityLayout.setVisibility(View.VISIBLE);
                        binding.settingsSpotifyInit.setVisibility(View.VISIBLE);
                    });

                    //Re-Save all sources
                    //this is 'scheduleSave' after library Sync
                    requireActivity().runOnUiThread(() ->
                            Toast.makeText(requireContext(), R.string.please_sync_to_apply, Toast.LENGTH_LONG).show());
                    System.out.println("BLADE-SPOTIFY: Added source Spotify");
                }
                catch(IOException e)
                {
                    System.out.println("BLADE-SPOTIFY: " + getString(R.string.auth_error) + " (IOException trying to obtain tokens)");
                    requireActivity().runOnUiThread(() ->
                            Toast.makeText(requireContext(), getString(R.string.auth_error) + " (IOException trying to obtain tokens)", Toast.LENGTH_SHORT).show());
                }
            });
        }
    }
}
