package v.blade.player;

import android.os.Bundle;
import android.support.v4.media.session.MediaSessionCompat;

import v.blade.BladeApplication;
import v.blade.library.Song;
import v.blade.sources.Source;
import v.blade.sources.SourceInformation;

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

        //Play current if not playing
        Song current = service.playlist.get(service.index);

        SourceInformation best = current.getBestSource();
        best.source.getPlayer().playSong(current);
    }

    @Override
    public void onPlayFromMediaId(String mediaId, Bundle extras)
    {
        //TODO REMOVE TEST
        super.onPlayFromMediaId(mediaId, extras);
        Song test = new Song("test", null, null, 0);
        test.getSources().add(new SourceInformation(Source.SOURCES.get(0), mediaId));

        BladeApplication.obtainExecutorService().execute(() ->
                Source.SOURCES.get(0).getPlayer().playSong(test));
    }
}
