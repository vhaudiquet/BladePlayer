package v.blade.player;

import android.content.Context;
import android.media.AudioManager;
import android.os.Process;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.widget.Toast;

import androidx.core.content.ContextCompat;
import androidx.media.AudioFocusRequestCompat;
import androidx.media.AudioManagerCompat;

import java.util.ArrayList;
import java.util.Collections;

import v.blade.BladeApplication;
import v.blade.R;
import v.blade.library.Song;
import v.blade.sources.SourceInformation;

public class MediaSessionCallback extends MediaSessionCompat.Callback
{
    protected MediaBrowserService service;

    private final AudioManager audioManager;
    private AudioFocusRequestCompat lastAudioFocusRequest;
    private boolean playOnAudioFocus = false;
    private final AudioManager.OnAudioFocusChangeListener audioFocusChangeListener = focusChange ->
    {
        switch(focusChange)
        {
            case AudioManager.AUDIOFOCUS_GAIN:
                if(service.mediaSession.getController().getPlaybackState().getState() != PlaybackStateCompat.STATE_PLAYING
                        && playOnAudioFocus)
                {
                    //if we gain audiofocus, check if we have to resume and resume if needed
                    onPlay();
                    playOnAudioFocus = false;
                }
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                if(service.mediaSession.getController().getPlaybackState().getState() == PlaybackStateCompat.STATE_PLAYING)
                {
                    //if we lose audiofocus for a little ammount of time, pause for this time and resume when we can
                    playOnAudioFocus = true;
                    onPause();
                }
            case AudioManager.AUDIOFOCUS_LOSS:
                if(service.mediaSession.getController().getPlaybackState().getState() == PlaybackStateCompat.STATE_PLAYING)
                {
                    onPause();
                }
        }
    };

    private long seekPosition = 0;

    protected MediaSessionCallback(MediaBrowserService service)
    {
        this.service = service;
        this.audioManager = (AudioManager) service.getSystemService(Context.AUDIO_SERVICE);
    }

    public void updatePlaybackState(boolean isPlaying)
    {
        PlaybackStateCompat.Builder stateBuilder = new PlaybackStateCompat.Builder().setActions(PlaybackStateCompat.ACTION_PREPARE
                | (isPlaying ? PlaybackStateCompat.ACTION_PAUSE : PlaybackStateCompat.ACTION_PLAY)
                | PlaybackStateCompat.ACTION_SKIP_TO_NEXT | PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
                | PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID | PlaybackStateCompat.ACTION_SEEK_TO
                | PlaybackStateCompat.ACTION_SET_REPEAT_MODE | PlaybackStateCompat.ACTION_SET_SHUFFLE_MODE);
        stateBuilder.setState(isPlaying ? PlaybackStateCompat.STATE_PLAYING : PlaybackStateCompat.STATE_PAUSED,
                service.current == null ? seekPosition : service.current.getCurrentPosition(), 1);
        service.mediaSession.setPlaybackState(stateBuilder.build());
    }

    @Override
    public void onPlay()
    {
        super.onPlay();

        //Ask for audio focus
        lastAudioFocusRequest = new AudioFocusRequestCompat.Builder(AudioManagerCompat.AUDIOFOCUS_GAIN)
                .setOnAudioFocusChangeListener(audioFocusChangeListener)
                .build();
        int audioFocus = AudioManagerCompat.requestAudioFocus(audioManager, lastAudioFocusRequest);
        if(audioFocus != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) return;

        /* Either we were paused and we resume play, or we play from playlist and we have to
         *   call playSong()
         */
        if(service.current != null && service.current.isPaused())
        {
            updatePlaybackState(true);
            service.notification.update();

            BladeApplication.obtainExecutorService().execute(() ->
            {
                //Give thread audio priority
                Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO);

                service.current.play();
            });
        }
        else
        {
            if(service.current != null) service.current.pause();

            if(service.playlist == null || service.playlist.size() <= service.index) return;
            Song song = service.playlist.get(service.index);
            if(song == null) return;
            SourceInformation bestSource = song.getBestSource();
            if(bestSource == null)
            {
                Toast.makeText(service, service.getString(R.string.song_no_source_error), Toast.LENGTH_LONG).show();

                System.out.println("BLADE: Song with no ready source : " + song.getName() + " ; sources are :");
                for(SourceInformation si : song.getSources())
                    System.out.println("BLADE: " + si.source.getName() + " (id " + si.id + "), source index " + si.source.getIndex() + ", status " + si.source.getStatus());

                return;
            }

            //Start service if not started (i.e. this is the first time the user clicks)
            service.startIfNotStarted();
            updatePlaybackState(true);

            System.out.println("BLADE: onPlay(" + service.playlist.get(service.index).getName() + ") from " + bestSource.source.getName());
            service.current = bestSource.source.getPlayer();
            BladeApplication.obtainExecutorService().execute(() ->
            {
                //Give thread Audio Priority
                Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO);

                service.current.playSong(song);
                ContextCompat.getMainExecutor(service).execute(() ->
                        service.notification.update());

                if(seekPosition != 0)
                {
                    service.current.seekTo(seekPosition);
                    seekPosition = 0;
                }
            });

        }
    }

    @Override
    public void onPause()
    {
        super.onPause();

        //Handle audiofocus
        if(!playOnAudioFocus && lastAudioFocusRequest != null)
        {
            AudioManagerCompat.abandonAudioFocusRequest(audioManager, lastAudioFocusRequest);
            lastAudioFocusRequest = null;
        }

        updatePlaybackState(false);
        service.notification.update();

        if(service.current != null)
            service.current.pause();
    }

    @Override
    public void onSeekTo(long pos)
    {
        super.onSeekTo(pos);

        if(service.current != null)
        {
            service.current.seekTo(pos);
            updatePlaybackState(!service.current.isPaused());
        }
        else seekPosition = pos;
    }

    @Override
    public void onSkipToNext()
    {
        super.onSkipToNext();

        if(service.playlist.size() - 1 == service.index) service.setIndex(0);
        else service.setIndex(service.index + 1);
        onPlay();
    }

    @Override
    public void onSkipToPrevious()
    {
        super.onSkipToPrevious();

        if(service.index == 0) service.index = service.playlist.size() - 1;
        else service.setIndex(service.index - 1);
        onPlay();
    }

    @Override
    public void onSetShuffleMode(int shuffleMode)
    {
        super.onSetShuffleMode(shuffleMode);

        if(shuffleMode == PlaybackStateCompat.SHUFFLE_MODE_NONE
                && service.mediaSession.getController().getShuffleMode() != PlaybackStateCompat.SHUFFLE_MODE_NONE)
        {
            //Retreive index
            int index = service.shuffleBackupList.indexOf(service.playlist.get(service.index));

            //Restore playlist and index
            service.playlist = service.shuffleBackupList;
            service.index = index;

            //Disable shuffle mode
            service.mediaSession.setShuffleMode(PlaybackStateCompat.SHUFFLE_MODE_NONE);
        }
        else if(service.mediaSession.getController().getShuffleMode() == PlaybackStateCompat.SHUFFLE_MODE_NONE)
        {
            //Generate shuffled list
            ArrayList<Song> shuffled = new ArrayList<>(service.playlist);
            Collections.shuffle(shuffled);
            shuffled.remove(service.playlist.get(service.index));
            shuffled.add(0, service.playlist.get(service.index));

            //Backup list
            service.shuffleBackupList = service.playlist;

            //Set list
            service.playlist = shuffled;
            service.index = 0;

            //Enable shuffle mode
            service.mediaSession.setShuffleMode(shuffleMode);
        }
    }

    @Override
    public void onSetRepeatMode(int repeatMode)
    {
        super.onSetRepeatMode(repeatMode);

        service.mediaSession.setRepeatMode(repeatMode);
    }
}
