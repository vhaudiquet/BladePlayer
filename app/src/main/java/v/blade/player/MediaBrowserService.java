package v.blade.player;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.media.MediaBrowserServiceCompat;
import androidx.media.session.MediaButtonReceiver;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import v.blade.BladeApplication;
import v.blade.library.Library;
import v.blade.library.Song;
import v.blade.sources.Source;
import v.blade.ui.PlayActivity;

public class MediaBrowserService extends MediaBrowserServiceCompat
{
    private static final String CURRENT_PLAYLIST_FILE = "/current_playlist.json";
    private static final String MEDIA_ROOT_ID = "MEDIA_ROOT";

    private static MediaBrowserService instance;

    protected MediaSessionCompat mediaSession;
    public MediaSessionCallback mediaSessionCallback;

    protected List<Song> playlist;
    protected int index;

    protected List<Song> shuffleBackupList;

    protected Source.Player current;
    private boolean isStarted = false;
    public PlayerNotification notification;

    protected void startIfNotStarted()
    {
        if(isStarted) return;

        //Start service if not started
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            startForegroundService(new Intent(this, MediaBrowserService.class));
        else
            startService(new Intent(this, MediaBrowserService.class));

        mediaSession.setActive(true);
        notification.update();

        isStarted = true;
    }

    @Override
    public void onCreate()
    {
        super.onCreate();
        instance = this;

        mediaSession = new MediaSessionCompat(this, "BLADE-MEDIA");
        mediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_QUEUE_COMMANDS);

        //Set session callbacks
        mediaSessionCallback = new MediaSessionCallback(this);
        mediaSession.setCallback(mediaSessionCallback);

        //NOTE : Set initial playback state here, so that we allow playing
        // (makes sense when service is restarting)
        if(playlist == null)
        {
            //We are stopped
            PlaybackStateCompat.Builder stateBuilder = new PlaybackStateCompat.Builder()
                    .setState(PlaybackStateCompat.STATE_STOPPED, 0L, 0)
                    .setActions(PlaybackStateCompat.ACTION_PREPARE);
            mediaSession.setPlaybackState(stateBuilder.build());
        }
        else mediaSessionCallback.updatePlaybackState(false);

        //Set mediaSession activity
        Intent openUI = new Intent(this, PlayActivity.class);
        openUI.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        int contentIntentFlags = PendingIntent.FLAG_CANCEL_CURRENT;
        if(android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M)
            contentIntentFlags |= PendingIntent.FLAG_IMMUTABLE;
        @SuppressLint("UnspecifiedImmutableFlag") //This must be a bug in my IDE, but the linter reports this warning here...
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0x42, openUI, contentIntentFlags);
        mediaSession.setSessionActivity(contentIntent);

        //Set service mediaSession
        setSessionToken(mediaSession.getSessionToken());

        //Init notification manager
        notification = new PlayerNotification(this);
    }

    /*
     * onGetRoot(), onLoadChildren() allows external to browse our media
     * TODO implement browsing and playFromMediaId as described in project notes
     */

    @Nullable
    @Override
    public BrowserRoot onGetRoot(@NonNull String clientPackageName, int clientUid, @Nullable Bundle rootHints)
    {
        return new BrowserRoot(MEDIA_ROOT_ID, null);
    }

    @Override
    public void onLoadChildren(@NonNull String parentId, @NonNull Result<List<MediaBrowserCompat.MediaItem>> result)
    {
        result.sendResult(new ArrayList<>());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        //NOTE ; we actually do NOT want to kill service when swiping notification, so this is useless
        /*
        if(intent != null && intent.getAction() != null && intent.getAction().equals("stop"))
        {
            //Set playback state to stopped to notify UI (TODO implement restarting ? but how do i restore playlist/index ?)
            PlaybackStateCompat.Builder stateBuilder = new PlaybackStateCompat.Builder()
                    .setState(PlaybackStateCompat.STATE_STOPPED, 0L, 0)
                    .setActions(PlaybackStateCompat.ACTION_PREPARE);
            mediaSession.setPlaybackState(stateBuilder.build());

            //Stop service
            isStarted = false;
            stopSelf();
            return START_STICKY;
        }*/

        MediaButtonReceiver.handleIntent(mediaSession, intent);
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent)
    {
        return super.onBind(intent);
    }

    public void setPlaylist(List<Song> list)
    {
        if(current != null) current.pause();
        current = null;
        this.playlist = list;
    }

    public void savePlaylist()
    {
        if(playlist == null || playlist.isEmpty())
        {
            File currentPlaylistFile = new File(BladeApplication.appContext.getFilesDir().getAbsolutePath() + CURRENT_PLAYLIST_FILE);
            currentPlaylistFile.delete();
            return;
        }

        Gson gson = new Gson();

        JsonObject currentPlaylistObject = new JsonObject();

        JsonArray songs = new JsonArray();
        for(Song s : playlist)
        {
            JsonObject songJson = Library.songJson(s, gson);
            songs.add(songJson);
        }
        currentPlaylistObject.add("songs", songs);

        currentPlaylistObject.addProperty("index", index);

        if(current != null)
            currentPlaylistObject.addProperty("position", current.getCurrentPosition());

        File currentPlaylistFile = new File(BladeApplication.appContext.getFilesDir().getAbsolutePath() + CURRENT_PLAYLIST_FILE);
        try
        {
            //noinspection ResultOfMethodCallIgnored
            currentPlaylistFile.delete();
            if(!currentPlaylistFile.createNewFile())
            {
                System.err.println("Could not save current playlist : could not create file");
                return;
            }

            BufferedWriter writer = new BufferedWriter(new FileWriter(currentPlaylistFile));
            writer.write(gson.toJson(currentPlaylistObject));
            writer.close();
        }
        catch(IOException ignored)
        {
        }
    }

    public void restorePlaylist()
    {
        File currentPlaylistFile = new File(BladeApplication.appContext.getFilesDir().getAbsolutePath() + CURRENT_PLAYLIST_FILE);
        if(!currentPlaylistFile.exists()) return;

        try
        {
            //read file
            BufferedReader reader = new BufferedReader(new FileReader(currentPlaylistFile));
            StringBuilder js = new StringBuilder();
            while(reader.ready()) js.append(reader.readLine()).append("\n");
            reader.close();

            //obtain from JSON
            JSONObject root = new JSONObject(js.toString());
            int index = root.getInt("index");

            ArrayList<Song> playlist = new ArrayList<>();
            JSONArray library = root.getJSONArray("songs");
            for(int i = 0; i < library.length(); i++)
            {
                JSONObject s = library.getJSONObject(i);
                playlist.add(Library.jsonSong(s, true));
            }

            if(playlist.isEmpty()) return;

            // Restore media player
            this.startIfNotStarted();
            this.setPlaylist(playlist);
            this.setIndex(index);
            mediaSession.getController().getTransportControls().pause();

            if(root.has("position"))
                mediaSession.getController().getTransportControls().seekTo(root.getLong("position"));
        }
        catch(IOException | JSONException ignored)
        {
        }
    }

    public void setIndex(int index)
    {
        if(current != null) current.pause();
        current = null;
        this.index = index;
    }

    public void updateIndexForReorder(int index)
    {
        this.index = index;
    }

    public List<Song> getPlaylist()
    {
        return playlist;
    }

    public int getIndex()
    {
        return index;
    }

    public void notifyPlaybackEnd()
    {
        //Handle repeat current song mode
        if(mediaSession.getController().getRepeatMode() == PlaybackStateCompat.REPEAT_MODE_ONE)
        {
            setIndex(index);
            mediaSessionCallback.onPlay();
            return;
        }

        if(playlist.size() - 1 == index)
        {
            setIndex(0);
            if(mediaSession.getController().getRepeatMode() == PlaybackStateCompat.REPEAT_MODE_NONE)
            {
                //Stop playback
                mediaSessionCallback.updatePlaybackState(false);
                notification.update();
            }
            else mediaSessionCallback.onPlay(); //Repeat playback
        }
        else
        {
            setIndex(index + 1);
            mediaSessionCallback.onPlay();
        }
    }

    public static MediaBrowserService getInstance()
    {
        return instance;
    }
}
