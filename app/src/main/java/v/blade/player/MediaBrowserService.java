package v.blade.player;

import android.os.Bundle;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.media.MediaBrowserServiceCompat;

import java.util.List;

import v.blade.library.Song;
import v.blade.sources.Source;

public class MediaBrowserService extends MediaBrowserServiceCompat
{
    private static final String MEDIA_ROOT_ID = "MEDIA_ROOT";

    private MediaSessionCompat mediaSession;
    private PlaybackStateCompat.Builder stateBuilder;

    protected List<Song> playlist;
    protected int index;
    protected Source.Player current;

    @Override
    public void onCreate()
    {
        super.onCreate();

        mediaSession = new MediaSessionCompat(this, "BLADE-MEDIA");
        //mediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_QUEUE_COMMANDS);

        //Set an initial PlaybackState : we can do nothing
        stateBuilder = new PlaybackStateCompat.Builder().setActions(PlaybackStateCompat.ACTION_PREPARE);
        mediaSession.setPlaybackState(stateBuilder.build());

        //Set session callbacks
        mediaSession.setCallback(new MediaSessionCallback(this));

        setSessionToken(mediaSession.getSessionToken());
    }

    /*
     * onGetRoot(), onLoadChildren() allows external to browse our media
     * TODO maybe implement that, either allowing direct library song browsing, or with
     *  root being artists, or with root being 4 dummy items : artists, albums, songs, playlists
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
}
