package v.blade.player;

import android.media.MediaMetadata;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;

import v.blade.BladeApplication;
import v.blade.library.Song;

public class MediaSessionCallback extends MediaSessionCompat.Callback
{
    protected final MediaBrowserService service;

    protected MediaSessionCallback(MediaBrowserService service)
    {
        this.service = service;
    }

    @Override
    public void onPlay()
    {
        super.onPlay();

        /* Either we were paused and we resume play, or we play from playlist and we have to
         *   call playSong()
         */
        if(service.current != null && service.current.isPaused())
        {
            service.notification.update(true);
            service.current.play();
        }
        else
        {
            if(service.current != null) service.current.pause();

            if(service.playlist == null || service.playlist.size() <= service.index) return;
            Song song = service.playlist.get(service.index);
            if(song == null) return;

            //Start service if not started (i.e. this is the first time the user clicks)
            service.startIfNotStarted();
            service.notification.update(true);

            //Update mediaSession metadata
            service.mediaSession.setMetadata(new MediaMetadataCompat.Builder()
                    .putString(MediaMetadata.METADATA_KEY_TITLE, song.getName())
                    .putString(MediaMetadata.METADATA_KEY_ARTIST, song.getArtistsString())
                    .putString(MediaMetadata.METADATA_KEY_ALBUM, song.getAlbum().getName())
                    .putString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE, song.getName())
                    .putLong(MediaMetadata.METADATA_KEY_TRACK_NUMBER, song.getTrackNumber())
                    .build());

            service.current = song.getBestSource().source.getPlayer();
            BladeApplication.obtainExecutorService().execute(() ->
                    service.current.playSong(song));
        }
    }

    @Override
    public void onPause()
    {
        super.onPause();

        service.notification.update(false);
        service.current.pause();
    }
}
