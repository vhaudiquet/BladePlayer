package v.blade.sources.spotify;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Build;

import com.spotify.connectstate.Connect;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Range;

import java.io.IOException;
import java.util.Locale;

import v.blade.library.Song;
import v.blade.sources.Source;
import v.blade.sources.SourceInformation;
import xyz.gianlu.librespot.core.Session;
import xyz.gianlu.librespot.player.Player;
import xyz.gianlu.librespot.player.PlayerConfiguration;
import xyz.gianlu.librespot.player.mixing.output.OutputAudioFormat;
import xyz.gianlu.librespot.player.mixing.output.SinkException;
import xyz.gianlu.librespot.player.mixing.output.SinkOutput;

public class SpotifyPlayer extends Source.Player
{
    private Player spotifyPlayer;
    protected Session playerSession;
    protected final Spotify current;

    public SpotifyPlayer(Spotify source)
    {
        this.current = source;
    }

    public boolean login(String username, String password)
    {
        Session.Configuration conf = new Session.Configuration.Builder()
                .setStoreCredentials(false)
                .setCacheEnabled(false)
                .build();

        Session.Builder sessionBuilder = new Session.Builder(conf);
        sessionBuilder.userPass(username, password)
                .setPreferredLocale(Locale.getDefault().getLanguage())
                .setDeviceType(Connect.DeviceType.SMARTPHONE)
                .setDeviceId(null).setDeviceName("Blade");

        try
        {
            playerSession = sessionBuilder.create();
            return true;
        }
        catch(Exception e)
        {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public void init()
    {
        //Configure the player
        PlayerConfiguration playerConfiguration = new PlayerConfiguration.Builder()
                .setOutput(PlayerConfiguration.AudioOutput.CUSTOM)
                .setOutputClass(BladeSinkOutput.class.getName())
                .setAutoplayEnabled(false)
                .build();

        //init the player
        spotifyPlayer = new Player(playerConfiguration, playerSession);
    }

    @Override
    public void play()
    {
        if(spotifyPlayer == null) return;
        spotifyPlayer.play();
    }

    @Override
    public void pause()
    {
        if(spotifyPlayer == null) return;
        spotifyPlayer.pause();
    }

    @Override
    public void playSong(Song song)
    {
        if(spotifyPlayer == null) return;

        SourceInformation current = null;
        for(int i = 0; i < song.getSources().size(); i++)
        {
            if(song.getSources().get(i).source instanceof Spotify)
            {
                current = song.getSources().get(i);
                break;
            }
        }
        if(current == null) return;

        spotifyPlayer.load("spotify:track:" + current.id, true, false);
    }

    @Override
    public void seekTo(int millis)
    {
        spotifyPlayer.seek(millis);
    }

    @Override
    public int getCurrentPosition()
    {
        return spotifyPlayer.time();
    }

    /*
     * Inspired by https://github.com/devgianlu/librespot-android/blob/master/librespot-android-sink/src/main/java/xyz/gianlu/librespot/android/sink/AndroidSinkOutput.java
     * This is using https://developer.android.com/reference/android/media/AudioTrack
     */
    public static class BladeSinkOutput implements SinkOutput
    {
        private float lastVolume = -1;
        private AudioTrack currentTrack;

        @Override
        public boolean start(@NotNull OutputAudioFormat format) throws SinkException
        {
            /* NOTE: i don't know why, but we only support 16-bit encoding ; maybe Android ?
             * Although ENCODING_PCM_16BIT is not the only one, 32, 8 and packed 24 are available...
             * TODO investigate more on that ? */
            if(format.getSampleSizeInBits() != 16)
                throw new SinkException("BLADE-SPOTIFY: Unsupported sample size : " + format.getSampleSizeInBits(), null);

            /* We only support 1 and 2 channels songs (MONO and STEREO)
             * TODO : Maybe allow 5.1, 7.1 and such ?? */
            if(format.getChannels() < 1 || format.getChannels() > 2)
                throw new SinkException("BLADE-SPOTIFY: Only 1 channel supported (format contains " + format.getChannels() + ")", null);

            int encoding = AudioFormat.ENCODING_PCM_16BIT;
            int sampleRate = (int) format.getSampleRate();
            AudioFormat audioFormat = new AudioFormat.Builder()
                    .setEncoding(encoding)
                    .setSampleRate(sampleRate)
                    .build();

            try
            {
                int channelConfig = format.getChannels() == 1 ? AudioFormat.CHANNEL_OUT_MONO : AudioFormat.CHANNEL_OUT_STEREO;
                int minBufferSize = AudioTrack.getMinBufferSize(sampleRate, channelConfig, encoding);

                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                {
                    currentTrack = new AudioTrack.Builder()
                            .setBufferSizeInBytes(minBufferSize)
                            .setAudioFormat(audioFormat)
                            .setTransferMode(AudioTrack.MODE_STREAM)
                            .build();
                }
                else
                {
                    currentTrack = new AudioTrack(AudioManager.STREAM_MUSIC, sampleRate, channelConfig,
                            encoding, minBufferSize, AudioTrack.MODE_STREAM);
                }
            }
            catch(UnsupportedOperationException e)
            {
                throw new SinkException("BLADE-SPOTIFY: AudioTrack creation failed : ", e.getCause());
            }

            if(lastVolume != -1) currentTrack.setVolume(lastVolume);
            currentTrack.play();
            return true;
        }

        @Override
        public void write(byte[] buffer, int offset, int len) throws IOException
        {
            int transferCount = currentTrack.write(buffer, offset, len);
            switch(transferCount)
            {
                case AudioTrack.ERROR:
                    throw new IOException("BLADE-SPOTIFY: AudioTrack write failure");
                case AudioTrack.ERROR_BAD_VALUE:
                    throw new IOException("BLADE-SPOTIFY: Invalid value used to write AudioTrack");
                case AudioTrack.ERROR_DEAD_OBJECT:
                    throw new IOException("BLADE-SPOTIFY: AudioTrack object died before write");
                case AudioTrack.ERROR_INVALID_OPERATION:
                    throw new IOException("BLADE-SPOTIFY: AudioTrack write invalid");
            }
        }

        @Override
        public boolean setVolume(@Range(from = 0L, to = 1L) float volume)
        {
            if(currentTrack == null) return false;

            lastVolume = volume;
            currentTrack.setVolume(volume);
            return true;
        }

        @Override
        public void release()
        {
            if(currentTrack != null) currentTrack.release();
        }

        @Override
        public void flush()
        {
            if(currentTrack != null) currentTrack.flush();
        }

        @Override
        public void stop()
        {
            if(currentTrack != null) currentTrack.stop();
        }

        @SuppressWarnings("RedundantThrows")
        @Override
        public void close() throws IOException
        {
            currentTrack = null;
        }
    }
}
