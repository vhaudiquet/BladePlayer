package v.blade.ui;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.PorterDuff;
import android.media.session.PlaybackState;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;
import android.widget.SeekBar;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Locale;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

import v.blade.R;
import v.blade.databinding.ActivityPlayBinding;
import v.blade.library.Song;
import v.blade.player.MediaBrowserService;

public class PlayActivity extends AppCompatActivity
{
    private ActivityPlayBinding binding;
    private MediaBrowserCompat mediaBrowser;
    private MediaControllerCompat.Callback mediaControllerCallback;
    private boolean showingPlaylist = false;

    //TODO : maybe fix that ? switch on something else ?
    @SuppressLint("NonConstantResourceId")
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        binding = ActivityPlayBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        //Set show playlist button action
        binding.playShowlist.setOnClickListener(view ->
        {
            if(!showingPlaylist)
            {
                binding.playAlbum.setVisibility(View.GONE);
                binding.playList.setVisibility(View.VISIBLE);
            }
            else
            {
                binding.playAlbum.setVisibility(View.VISIBLE);
                binding.playList.setVisibility(View.GONE);
            }

            showingPlaylist = !showingPlaylist;
        });

        //Set more button action
        binding.playMore.setOnClickListener(view ->
        {
            PopupMenu popupMenu = new PopupMenu(this, view);
            popupMenu.inflate(R.menu.currentplay_more);

            popupMenu.setOnMenuItemClickListener(item ->
            {
                Song current = MediaBrowserService.getInstance().getPlaylist().get(MediaBrowserService.getInstance().getIndex());

                switch(item.getItemId())
                {
                    case R.id.action_add_to_list:
                        Dialogs.openAddToPlaylistDialog(this, current);
                        return true;
                    case R.id.action_manage_libraries:
                        Dialogs.openManageLibrariesDialog(this, current);
                        return true;
                    case R.id.action_lyrics:
                        Intent intent = new Intent(this, LyricsActivity.class);
                        startActivity(intent);
                        return true;
                }
                return false;
            });
            popupMenu.show();
        });

        //Set play button action
        binding.playPlay.setOnClickListener(view ->
        {
            if(getMediaController().getPlaybackState().getState() == PlaybackState.STATE_PLAYING)
            {
                getMediaController().getTransportControls().pause();
            }
            else
            {
                getMediaController().getTransportControls().play();
            }
        });

        //Set skip buttons actions
        binding.playSkipprev.setOnClickListener(view -> getMediaController().getTransportControls().skipToPrevious());
        binding.playSkipnext.setOnClickListener(view -> getMediaController().getTransportControls().skipToNext());

        //Set shuffle and repeat actions
        binding.playShuffle.setOnClickListener(view ->
        {
            if(MediaControllerCompat.getMediaController(this).getShuffleMode() == PlaybackStateCompat.SHUFFLE_MODE_NONE)
                MediaControllerCompat.getMediaController(this).getTransportControls().setShuffleMode(PlaybackStateCompat.SHUFFLE_MODE_ALL);
            else
                MediaControllerCompat.getMediaController(this).getTransportControls().setShuffleMode(PlaybackStateCompat.SHUFFLE_MODE_NONE);
        });
        binding.playRepeat.setOnClickListener(view ->
        {
            if(MediaControllerCompat.getMediaController(this).getRepeatMode() == PlaybackStateCompat.REPEAT_MODE_NONE)
                MediaControllerCompat.getMediaController(this).getTransportControls().setRepeatMode(PlaybackStateCompat.REPEAT_MODE_ONE);
            else if(MediaControllerCompat.getMediaController(this).getRepeatMode() == PlaybackStateCompat.REPEAT_MODE_ONE)
                MediaControllerCompat.getMediaController(this).getTransportControls().setRepeatMode(PlaybackStateCompat.REPEAT_MODE_ALL);
            else
                MediaControllerCompat.getMediaController(this).getTransportControls().setRepeatMode(PlaybackStateCompat.REPEAT_MODE_NONE);
        });

        //Set seekBar action
        binding.playSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener()
        {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser)
            {
                if(fromUser) getMediaController().getTransportControls().seekTo(progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar)
            {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar)
            {

            }
        });

        //Bind MediaBrowserService to activity
        Intent serviceIntent = new Intent(this, MediaBrowserService.class);
        bindService(serviceIntent, new ServiceConnection()
        {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service)
            {

            }

            @Override
            public void onServiceDisconnected(ComponentName name)
            {

            }
        }, 0);

        mediaBrowser = new MediaBrowserCompat(this, new ComponentName(this, MediaBrowserService.class),
                new MediaBrowserCompat.ConnectionCallback()
                {
                    @Override
                    public void onConnected()
                    {
                        super.onConnected();
                        MediaSessionCompat.Token token = mediaBrowser.getSessionToken();
                        MediaControllerCompat mediaController =
                                new MediaControllerCompat(PlayActivity.this, token);
                        MediaControllerCompat.setMediaController(PlayActivity.this,
                                mediaController);

                        mediaControllerCallback = new MediaControllerCompat.Callback()
                        {
                            @Override
                            public void onPlaybackStateChanged(PlaybackStateCompat state)
                            {
                                super.onPlaybackStateChanged(state);

                                if(state == null || state.getState() == PlaybackStateCompat.STATE_STOPPED)
                                {
                                    //Go back, exit activity
                                    onBackPressed();
                                    return;
                                }

                                //Set play/pause button
                                if(state.getState() == PlaybackStateCompat.STATE_PLAYING)
                                    binding.playPlay.setImageResource(R.drawable.ic_pause_circle);
                                else
                                    binding.playPlay.setImageResource(R.drawable.ic_play_circle);

                                //Set playtime
                                long positionMillis = state.getPosition();
                                long positionMins = (positionMillis / 60000) % 60000;
                                long positionSecs = positionMillis % 60000 / 1000;
                                String positionString = String.format(Locale.getDefault(), "%02d:%02d", positionMins, positionSecs);
                                binding.playTime.setText(positionString);
                                binding.playSeekbar.setProgress((int) positionMillis);
                            }

                            @Override
                            public void onMetadataChanged(MediaMetadataCompat metadata)
                            {
                                super.onMetadataChanged(metadata);

                                if(metadata == null) return;

                                binding.playTitle.setText(metadata.getString(MediaMetadataCompat.METADATA_KEY_TITLE));
                                String subtitle = metadata.getString(MediaMetadataCompat.METADATA_KEY_ARTIST) + " \u00B7 " + metadata.getString(MediaMetadataCompat.METADATA_KEY_ALBUM);
                                binding.playSubtitle.setText(subtitle);

                                //Set duration
                                long durationMillis = metadata.getLong(MediaMetadataCompat.METADATA_KEY_DURATION);
                                long durationMins = (durationMillis / 60000) % 60000;
                                long durationSecs = durationMillis % 60000 / 1000;
                                String durationString = String.format(Locale.getDefault(), "%02d:%02d", durationMins, durationSecs);
                                binding.playDuration.setText(durationString);
                                binding.playSeekbar.setMax((int) durationMillis);

                                //Set art
                                Bitmap art = metadata.getBitmap(MediaMetadataCompat.METADATA_KEY_ART);
                                if(art != null) binding.playAlbum.setImageBitmap(art);
                                else binding.playAlbum.setImageResource(R.drawable.ic_album);

                                updatePlaylist();
                            }

                            @Override
                            public void onShuffleModeChanged(int shuffleMode)
                            {
                                super.onShuffleModeChanged(shuffleMode);

                                if(shuffleMode == PlaybackStateCompat.SHUFFLE_MODE_ALL
                                        || shuffleMode == PlaybackStateCompat.SHUFFLE_MODE_GROUP)
                                {
                                    binding.playShuffle.setColorFilter(ContextCompat.getColor(PlayActivity.this, R.color.enabledFilter),
                                            PorterDuff.Mode.SRC_ATOP);
                                }
                                else
                                {
                                    binding.playShuffle.setColorFilter(null);
                                }

                                updatePlaylist();
                            }

                            @Override
                            public void onRepeatModeChanged(int repeatMode)
                            {
                                super.onRepeatModeChanged(repeatMode);

                                switch(repeatMode)
                                {
                                    case PlaybackStateCompat.REPEAT_MODE_INVALID:
                                    case PlaybackStateCompat.REPEAT_MODE_NONE:
                                        binding.playRepeat.setImageResource(R.drawable.ic_repeat);
                                        binding.playRepeat.setColorFilter(null);
                                        break;
                                    case PlaybackStateCompat.REPEAT_MODE_ONE:
                                        binding.playRepeat.setImageResource(R.drawable.ic_repeat_one);
                                        binding.playRepeat.setColorFilter(ContextCompat.getColor(PlayActivity.this, R.color.enabledFilter),
                                                PorterDuff.Mode.SRC_ATOP);
                                        break;
                                    case PlaybackStateCompat.REPEAT_MODE_ALL:
                                    case PlaybackStateCompat.REPEAT_MODE_GROUP:
                                        binding.playRepeat.setImageResource(R.drawable.ic_repeat);
                                        binding.playRepeat.setColorFilter(ContextCompat.getColor(PlayActivity.this, R.color.enabledFilter),
                                                PorterDuff.Mode.SRC_ATOP);
                                        break;
                                }
                            }
                        };

                        //on connection, actualize UI
                        mediaControllerCallback.onPlaybackStateChanged(mediaController.getPlaybackState());
                        mediaControllerCallback.onMetadataChanged(mediaController.getMetadata());
                        mediaControllerCallback.onPlaybackStateChanged(mediaController.getPlaybackState()); //Call again to make sure progressbar updates after max
                        mediaControllerCallback.onShuffleModeChanged(mediaController.getShuffleMode());
                        mediaControllerCallback.onRepeatModeChanged(mediaController.getRepeatMode());

                        //Setup task that will update seekBar/position every second
                        Timer timer = new Timer();
                        timer.schedule(new TimerTask()
                        {
                            @Override
                            public void run()
                            {
                                runOnUiThread(() ->
                                {
                                    if(getMediaController() != null
                                            && getMediaController().getPlaybackState().getState() == PlaybackState.STATE_PLAYING
                                            && binding.playSeekbar.getMax() != 0)
                                    {
                                        int progress = (int) mediaController.getPlaybackState().getPosition();

                                        int positionMins = (progress / 60000) % 60000;
                                        int positionSecs = progress % 60000 / 1000;
                                        String positionString = String.format(Locale.getDefault(), "%02d:%02d", positionMins, positionSecs);
                                        binding.playTime.setText(positionString);
                                        binding.playSeekbar.setProgress(progress);
                                    }
                                });
                            }
                        }, 0, 1000);

                        //Register a callback so that UI stays in sync
                        mediaController.registerCallback(mediaControllerCallback);
                    }

                    @Override
                    public void onConnectionSuspended()
                    {
                        super.onConnectionSuspended();
                        // The Service has crashed ; Disable transport controls until it automatically reconnects
                        //TODO disable ui ? idk
                    }

                    @Override
                    public void onConnectionFailed()
                    {
                        super.onConnectionFailed();
                        System.err.println("MediaBrowser connection failed");
                    }
                }, null);
    }

    private void updatePlaylist()
    {
        //Set 'playList' layout height to image height
        binding.playList.getLayoutParams().height = binding.playAlbum.getHeight()
                + ((ViewGroup.MarginLayoutParams) binding.playAlbum.getLayoutParams()).topMargin
                + ((ViewGroup.MarginLayoutParams) binding.playAlbum.getLayoutParams()).bottomMargin;

        //Disable long clicks on the playList view
        binding.playList.setOnLongClickListener(v -> true);

        //Set playlist adapter/touchHelper/clickListener

        binding.playList.setLayoutManager(new LinearLayoutManager(this));
        ItemTouchHelper touchHelper = new ItemTouchHelper(
                new TouchHelperCallback(MediaBrowserService.getInstance().getPlaylist())
                {
                    @Override
                    public int getMovementFlags(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder)
                    {
                        int dragFlags = ItemTouchHelper.UP | ItemTouchHelper.DOWN;
                        //Enable swipe out, except on currently playing song
                        int swipeFlags = viewHolder.getAdapterPosition() == MediaBrowserService.getInstance().getIndex() ?
                                0 : ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT;
                        return makeMovementFlags(dragFlags, swipeFlags);
                    }

                    @Override
                    public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target)
                    {
                        //Recalculate the index of current in new list
                        int current = MediaBrowserService.getInstance().getIndex();
                        int from = viewHolder.getAdapterPosition();
                        int to = target.getAdapterPosition();

                        int dest = current;
                        if(current == from) dest = to;
                        else if(to >= current && from < current) dest = current - 1;
                        else if(to <= current && from > current) dest = current + 1;

                        //Re-order playlist and update index
                        boolean tr = super.onMove(recyclerView, viewHolder, target);
                        MediaBrowserService.getInstance().updateIndexForReorder(dest);

                        return tr;
                    }

                    @Override
                    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction)
                    {
                        super.onSwiped(viewHolder, direction);
                        int pos = viewHolder.getAdapterPosition();

                        //Update index
                        if(pos < MediaBrowserService.getInstance().getIndex())
                            MediaBrowserService.getInstance().updateIndexForReorder(MediaBrowserService.getInstance().getIndex() - 1);

                        //Notify adapter
                        Objects.requireNonNull(binding.playList.getAdapter()).notifyItemRemoved(pos);
                    }
                });
        LibraryObjectAdapter adapter = new LibraryObjectAdapter(MediaBrowserService.getInstance().getPlaylist()
                , touchHelper, view ->
        {
            MediaBrowserService.getInstance().setIndex(binding.playList.getChildAdapterPosition(view));
            getMediaController().getTransportControls().play();
        });
        touchHelper.attachToRecyclerView(binding.playList);
        binding.playList.setAdapter(adapter);

        adapter.setSelectedPosition(MediaBrowserService.getInstance().getIndex());
        binding.playList.scrollToPosition(MediaBrowserService.getInstance().getIndex());
    }

    @Override
    protected void onStart()
    {
        super.onStart();
        mediaBrowser.connect();
    }

    @Override
    protected void onStop()
    {
        super.onStop();

        if(MediaControllerCompat.getMediaController(this) != null)
            MediaControllerCompat.getMediaController(this).unregisterCallback(mediaControllerCallback);

        mediaBrowser.disconnect();
    }

}