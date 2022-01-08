package v.blade.player;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.media.MediaBrowserServiceCompat;
import androidx.media.session.MediaButtonReceiver;

import java.util.List;

import v.blade.library.Song;
import v.blade.sources.Source;

public class MediaBrowserService extends MediaBrowserServiceCompat
{
    private static final String MEDIA_ROOT_ID = "MEDIA_ROOT";

    private static MediaBrowserService instance;

    protected MediaSessionCompat mediaSession;
    private PlaybackStateCompat.Builder stateBuilder;

    protected List<Song> playlist;
    protected int index;
    protected Source.Player current;
    private boolean isStarted = false;
    protected PlayerNotification notification;

    protected void startIfNotStarted()
    {
        if(isStarted) return;

        //Start service if not started
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            startForegroundService(new Intent(this, MediaBrowserService.class));
        else
            startService(new Intent(this, MediaBrowserService.class));

        isStarted = true;
    }

    @Override
    public void onCreate()
    {
        super.onCreate();
        instance = this;

        mediaSession = new MediaSessionCompat(this, "BLADE-MEDIA");
        mediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_QUEUE_COMMANDS);

        //Set an initial PlaybackState
        //TODO for now we say we can do anything at any time ; maybe we should change that
        stateBuilder = new PlaybackStateCompat.Builder().setActions(PlaybackStateCompat.ACTION_PREPARE
                | PlaybackStateCompat.ACTION_PLAY | PlaybackStateCompat.ACTION_PAUSE
                | PlaybackStateCompat.ACTION_SKIP_TO_NEXT | PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
                | PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID | PlaybackStateCompat.ACTION_SEEK_TO
                | PlaybackStateCompat.ACTION_SET_REPEAT_MODE | PlaybackStateCompat.ACTION_SET_SHUFFLE_MODE);
        mediaSession.setPlaybackState(stateBuilder.build());

        //Set session callbacks
        mediaSession.setCallback(new MediaSessionCallback(this));

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

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        MediaButtonReceiver.handleIntent(mediaSession, intent);
        return super.onStartCommand(intent, flags, startId);
    }

    public void setPlaylist(List<Song> list)
    {
        this.playlist = list;
    }

    public void setIndex(int index)
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

    public static MediaBrowserService getInstance()
    {
        return instance;
    }
}
