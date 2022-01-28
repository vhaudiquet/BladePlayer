package v.blade.player;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.media.MediaBrowserServiceCompat;
import androidx.media.session.MediaButtonReceiver;

import java.io.FileDescriptor;
import java.util.ArrayList;
import java.util.List;

import v.blade.library.Song;
import v.blade.sources.Source;
import v.blade.ui.PlayActivity;

public class MediaBrowserService extends MediaBrowserServiceCompat
{
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
        }

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
