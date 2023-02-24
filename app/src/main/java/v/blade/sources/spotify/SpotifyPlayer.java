package v.blade.sources.spotify;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Build;
import android.os.Process;
import android.provider.Settings;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import com.spotify.connectstate.Connect;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Formatter;
import java.util.Locale;
import java.util.concurrent.RejectedExecutionException;

import v.blade.BladeApplication;
import v.blade.R;
import v.blade.library.Song;
import v.blade.player.MediaBrowserService;
import v.blade.sources.Source;
import v.blade.sources.SourceInformation;
import xyz.gianlu.librespot.audio.MetadataWrapper;
import xyz.gianlu.librespot.core.Session;
import xyz.gianlu.librespot.metadata.PlayableId;
import xyz.gianlu.librespot.player.Player;
import xyz.gianlu.librespot.player.PlayerConfiguration;
import xyz.gianlu.librespot.player.mixing.output.OutputAudioFormat;
import xyz.gianlu.librespot.player.mixing.output.SinkException;
import xyz.gianlu.librespot.player.mixing.output.SinkOutput;

public class SpotifyPlayer extends Source.Player
{
    private WeakReference<Player> spotifyPlayer;
    private volatile boolean isLoggingIn;
    protected WeakReference<Session> playerSession;
    private int trackChanges;
    protected final Spotify current;
    private volatile boolean isPaused;

    //TODO : this 'temp' fixes the 'multiple load() -> fatal error' bug ; find a better fix ?
    private volatile boolean isLoading = false;

    //TODO : remove that, temp bugfix librespot seek
    private int tempSeekPos = -1;

    private int errorRetryCount = 0;

    public SpotifyPlayer(Spotify source)
    {
        this.current = source;
    }

    public boolean login(String username, String password)
    {
        if(isLoggingIn) return false;

        isLoggingIn = true;

        Session.Configuration conf = new Session.Configuration.Builder()

                //NOTE : this seems to fix https://github.com/librespot-org/librespot-java/issues/447,
                // but it is a bad fix ; TODO find a better fix catching the interrupted timeout exception ?
                //.setConnectionTimeout(-1)
                .setStoreCredentials(false)
                .setCacheEnabled(true) //TODO : what cache does this exactly controls ? song cache ?
                .setCacheDir(new File(BladeApplication.appContext.getCacheDir().getAbsolutePath() + "/spotify-librespot-cache"))
                .build();

        String deviceName = null;
        if(android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N_MR1)
            deviceName = Settings.Global.getString(BladeApplication.appContext.getContentResolver(), Settings.Global.DEVICE_NAME);
        if(deviceName == null)
            deviceName = Build.MANUFACTURER + " " + Build.MODEL;

        //Generate a 40-char string 'device id' from 'BLADE' + deviceName, hexadecimal SHA1
        String deviceId = null;
        try
        {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] sha = md.digest(("BLADE" + deviceName).getBytes(StandardCharsets.UTF_8));
            Formatter formatter = new Formatter();
            for(byte b : sha)
            {
                formatter.format("%02x", b);
            }
            deviceId = formatter.toString();
        }
        catch(NoSuchAlgorithmException e)
        {
            System.err.println("BLADE-SPOTIFY: login() failed: NoSuchAlgorithmException");
            e.printStackTrace();
        }

        //TODO : we are storing plain passwords locally
        // this is not a critical security issue, as technically since a certain android version,
        // only Blade can access Blade file directory. However, we could store 'blobs', it
        // would improve security without any penalty
        Session.Builder sessionBuilder = new Session.Builder(conf);
        sessionBuilder.userPass(username, password)
                .setPreferredLocale(Locale.getDefault().getLanguage())
                .setDeviceType(Connect.DeviceType.SMARTPHONE)
                .setDeviceId(deviceId).setDeviceName("Blade (" + deviceName + ")");

        try
        {
            playerSession = new WeakReference<>(sessionBuilder.create());
            isLoggingIn = false;
            return true;
        }
        catch(Exception e)
        {
            System.err.println("BLADE-SPOTIFY: login() failed: " + e.getMessage());
            e.printStackTrace();
            isLoggingIn = false;
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
                .setPreferredQuality(current.spotifyAudioQuality)
                //.setAutoplayEnabled(false)
                // NOTE : when i set autoplay disabled, the player crashes at the end of play
                //  and i can't seem to be able to intercept that crash ; so i just set autoplay
                //  enabled and pause the player on track end
                .build();

        //init the player
        spotifyPlayer = new WeakReference<>(new Player(playerConfiguration, playerSession.get()));
        spotifyPlayer.get().addEventsListener(new Player.EventsListener()
        {
            @Override
            public void onContextChanged(@NotNull Player player, @NotNull String newUri)
            {
                //trackChanges = 0;
            }

            @Override
            public void onTrackChanged(@NotNull Player player, @NotNull PlayableId id, @Nullable MetadataWrapper metadata, boolean userInitiated)
            {
                if(trackChanges >= 1)
                {
                    player.pause();
                    ContextCompat.getMainExecutor(MediaBrowserService.getInstance())
                            .execute(MediaBrowserService.getInstance()::notifyPlaybackEnd);
                }
                trackChanges++;
                isPaused = false;
            }

            @Override
            public void onPlaybackEnded(@NotNull Player player)
            {
                //TODO :  we must notifyPlaybackEnd() here, but only in some cases (not on track end but if queue fails...)
                // it is complicated
                System.out.println("BLADE-SPOTIFY: Playback ended");
                isPaused = false;
                //player.pause();
                //ContextCompat.getMainExecutor(MediaBrowserService.getInstance())
                //        .execute(MediaBrowserService.getInstance()::notifyPlaybackEnd);
            }

            @Override
            public void onPlaybackPaused(@NotNull Player player, long trackTime)
            {
                isPaused = true;
            }

            @Override
            public void onPlaybackResumed(@NotNull Player player, long trackTime)
            {
                isPaused = false;
            }

            @Override
            public void onPlaybackFailed(@NotNull Player player, @NotNull Exception e)
            {
                // TODO use this ; we got librespot new version :))
                isPaused = true;
            }

            @Override
            public void onTrackSeeked(@NotNull Player player, long trackTime)
            {

            }

            @Override
            public void onMetadataAvailable(@NotNull Player player, @NotNull MetadataWrapper metadata)
            {
                ContextCompat.getMainExecutor(MediaBrowserService.getInstance()).execute(
                        MediaBrowserService.getInstance().notification::update);
            }

            @Override
            public void onPlaybackHaltStateChanged(@NotNull Player player, boolean halted, long trackTime)
            {

            }

            @Override
            public void onInactiveSession(@NotNull Player player, boolean timeout)
            {
                isLoading = false;
            }

            @Override
            public void onVolumeChanged(@NotNull Player player, float volume)
            {

            }

            @Override
            public void onPanicState(@NotNull Player player)
            {
                System.out.println("BLADE-SPOTIFY: Player panic");
                errorRetryCount++;
                isLoading = false;
                isPaused = false;
                if(errorRetryCount == 3)
                {
                    ContextCompat.getMainExecutor(MediaBrowserService.getInstance()).execute(() ->
                    {
                        MediaBrowserService.getInstance().mediaSessionCallback.updatePlaybackState(false);
                        Toast.makeText(MediaBrowserService.getInstance(), R.string.spotify_player_panic, Toast.LENGTH_SHORT).show();
                        errorRetryCount = 0;
                    });
                }
                else
                {
                    BladeApplication.obtainExecutorService().execute(() ->
                    {
                        Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);

                        try
                        {
                            Thread.sleep(200);
                        }
                        catch(InterruptedException e)
                        {
                            e.printStackTrace();
                        }


                        ContextCompat.getMainExecutor(MediaBrowserService.getInstance()).execute(() ->
                                MediaBrowserService.getInstance().mediaSessionCallback.onPlay());
                    });
                }
            }

            @Override
            public void onStartedLoading(@NotNull Player player)
            {
                isLoading = true;
            }

            @Override
            public void onFinishedLoading(@NotNull Player player)
            {
                errorRetryCount = 0;
                System.out.println("BLADE-SPOTIFY: Player finished loading");
                isLoading = false;
            }
        });
    }

    @Override
    public void play()
    {
        if(spotifyPlayer == null) return;
        if(spotifyPlayer.get() == null) init();

        spotifyPlayer.get().play();
    }

    @Override
    public void pause()
    {
        if(spotifyPlayer == null) return;
        if(spotifyPlayer.get() == null) init();
        if(isPaused()) return;

        spotifyPlayer.get().pause();
    }

    @Override
    public void playSong(Song song)
    {
        if(spotifyPlayer == null) return;

        //This is spamclick issue 'tryfix' ; it allows to click faster but leads to another player error
        //cf https://github.com/devgianlu/librespot-android/issues/16
        //noinspection StatementWithEmptyBody
        while(isLoading) ;
        isLoading = true;

        if(spotifyPlayer.get() == null) init();

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

        trackChanges = 0;

        System.out.println("BLADE-SPOTIFY: playSong(" + song.getName() + ")");
        if(spotifyPlayer.get().isReady())
        {
            try
            {
                spotifyPlayer.get().load("spotify:track:" + current.id, true, false);
            }
            catch(IllegalStateException exception)
            {
                isLoading = false;
                isPaused = false;
                System.err.println("BLADE-SPOTIFY: Player should have been ready, but effectively was not");
                MediaBrowserService.getInstance().mediaSessionCallback.updatePlaybackState(false);
            }
            catch(RejectedExecutionException exception)
            {
                isLoading = false;
                isPaused = false;
                System.err.println("BLADE-SPOTIFY: Too much tasks, skipping");
                MediaBrowserService.getInstance().mediaSessionCallback.updatePlaybackState(false);
            }
        }
        else
        {
            isLoading = false;
            isPaused = false;
            System.err.println("BLADE-SPOTIFY: Player was not ready");
            ContextCompat.getMainExecutor(MediaBrowserService.getInstance()).execute(() ->
                    Toast.makeText(MediaBrowserService.getInstance(),
                            MediaBrowserService.getInstance().getString(R.string.player_not_ready,
                                    MediaBrowserService.getInstance().getString(Spotify.NAME_RESOURCE)), Toast.LENGTH_SHORT).show());
            MediaBrowserService.getInstance().mediaSessionCallback.updatePlaybackState(false);

            //ready ; does that 'try to make ready' the player ?
            BladeApplication.obtainExecutorService().execute(() ->
            {
                Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
                spotifyPlayer.get().ready();
            });
        }
    }

    @Override
    public void seekTo(long millis)
    {
        if(spotifyPlayer == null) return;
        if(spotifyPlayer.get() == null) return;

        spotifyPlayer.get().seek((int) millis);
        //TODO : seek does not seem to happen immediately, maybe use onTrackSeeked() instead
        // cf https://github.com/librespot-org/librespot-java/issues/448
        tempSeekPos = (int) millis;
    }

    @Override
    public long getCurrentPosition()
    {
        if(spotifyPlayer == null) return 0;
        if(spotifyPlayer.get() == null) return 0;

        //TODO : find a better way to wait for seek (cf seekTo)
        if(tempSeekPos != -1)
        {
            int tr = tempSeekPos;
            tempSeekPos = -1;
            return tr;
        }

        return spotifyPlayer.get().time();
    }

    @Override
    public long getDuration()
    {
        if(spotifyPlayer == null) return 0;
        if(spotifyPlayer.get() == null) return 0;
        if(spotifyPlayer.get().currentMetadata() == null) return 0;

        //noinspection ConstantConditions
        return spotifyPlayer.get().currentMetadata().duration();
    }

    @Override
    public boolean isPaused()
    {
        if(spotifyPlayer == null) return false;
        if(spotifyPlayer.get() == null) return false;

        return isPaused;
    }

    /*
     * Inspired by https://github.com/devgianlu/librespot-android/blob/master/librespot-android-sink/src/main/java/xyz/gianlu/librespot/android/sink/AndroidSinkOutput.java
     * This is using https://developer.android.com/reference/android/media/AudioTrack
     * TODO : Maybe implement other outputs, using android native decoder or libtremolo
     *  According to https://github.com/devgianlu/librespot-android/pull/9#issuecomment-832923010, i guess that this output is the
     *  worst one performance-wise, not sure tho
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
        public boolean setVolume(float volume)
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
            if(currentTrack != null && currentTrack.getPlayState() != AudioTrack.PLAYSTATE_STOPPED)
                currentTrack.stop();
        }

        @SuppressWarnings("RedundantThrows")
        @Override
        public void close() throws IOException
        {
            currentTrack = null;
        }
    }
}
