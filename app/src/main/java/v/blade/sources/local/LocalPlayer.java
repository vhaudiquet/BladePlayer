package v.blade.sources.local;

import android.content.ContentUris;
import android.media.MediaPlayer;
import android.net.Uri;
import android.provider.MediaStore;

import androidx.core.content.ContextCompat;

import java.io.IOException;

import v.blade.BladeApplication;
import v.blade.library.Song;
import v.blade.player.MediaBrowserService;
import v.blade.sources.Source;
import v.blade.sources.SourceInformation;

public class LocalPlayer extends Source.Player
{
    private final Local local;

    private final MediaPlayer mediaPlayer;

    protected LocalPlayer(Local local)
    {
        this.local = local;
        mediaPlayer = new MediaPlayer();
        init();
    }

    @Override
    public void init()
    {
        mediaPlayer.setOnCompletionListener(mp ->
                ContextCompat.getMainExecutor(MediaBrowserService.getInstance())
                        .execute(MediaBrowserService.getInstance()::notifyPlaybackEnd));
    }

    @Override
    public void play()
    {
        mediaPlayer.start();
    }

    @Override
    public void pause()
    {
        mediaPlayer.pause();
    }

    @Override
    public void playSong(Song song)
    {
        //Obtain id
        SourceInformation current = null;
        for(SourceInformation si : song.getSources())
        {
            if(si.source == local)
            {
                current = si;
                break;
            }
        }
        if(current == null) return;

        Uri songUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, ((Number) current.id).longValue());

        try
        {
            mediaPlayer.reset();
            mediaPlayer.setDataSource(BladeApplication.appContext, songUri);
            mediaPlayer.prepare();
            play();
        }
        catch(IOException | RuntimeException e)
        {
            e.printStackTrace();
        }
    }

    @Override
    public void seekTo(long millis)
    {
        mediaPlayer.seekTo((int) millis);
    }

    @Override
    public long getCurrentPosition()
    {
        return mediaPlayer.getCurrentPosition();
    }

    @Override
    public long getDuration()
    {
        try
        {
            return mediaPlayer.getDuration();
        }
        catch(IllegalStateException e)
        {
            return 0;
        }
    }

    @Override
    public boolean isPaused()
    {
        return !mediaPlayer.isPlaying();
    }
}
