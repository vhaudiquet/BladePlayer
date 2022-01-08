package v.blade.ui;

import android.content.ComponentName;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import v.blade.R;
import v.blade.databinding.ActivityPlayBinding;
import v.blade.player.MediaBrowserService;

public class PlayActivity extends AppCompatActivity
{
    private ActivityPlayBinding binding;
    private MediaBrowserCompat mediaBrowser;
    private boolean showingPlaylist = false;

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

                        MediaControllerCompat.Callback mediaControllerCallback = new MediaControllerCompat.Callback()
                        {
                            @Override
                            public void onPlaybackStateChanged(PlaybackStateCompat state)
                            {
                                super.onPlaybackStateChanged(state);

                            }

                            @Override
                            public void onMetadataChanged(MediaMetadataCompat metadata)
                            {
                                super.onMetadataChanged(metadata);

                                binding.playTitle.setText(metadata.getString(MediaMetadataCompat.METADATA_KEY_TITLE));
                                String subtitle = metadata.getString(MediaMetadataCompat.METADATA_KEY_ARTIST) + " \u00B7 " + metadata.getString(MediaMetadataCompat.METADATA_KEY_ALBUM);
                                binding.playSubtitle.setText(subtitle);

                                //Set art
                                Bitmap art = metadata.getBitmap(MediaMetadataCompat.METADATA_KEY_ART);
                                if(art != null) binding.playAlbum.setImageBitmap(art);
                                else binding.playAlbum.setImageResource(R.drawable.ic_album);

                                updatePlaylist();
                            }
                        };

                        //on connection, actualize UI
                        mediaControllerCallback.onMetadataChanged(mediaController.getMetadata());
                        mediaControllerCallback.onPlaybackStateChanged(mediaController.getPlaybackState());
                        updatePlaylist();

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
        binding.playList.getLayoutParams().height = binding.playAlbum.getHeight();

        //Set playlist adapter/touchHelper/clickListener
        binding.playList.setLayoutManager(new LinearLayoutManager(this));
        ItemTouchHelper touchHelper = new ItemTouchHelper(
                new TouchHelperCallback(MediaBrowserService.getInstance().getPlaylist())
                {
                    @Override
                    public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target)
                    {
                        //Recalculate the index of current in new list
                        int current = MediaBrowserService.getInstance().getIndex();
                        int from = viewHolder.getAdapterPosition();
                        int to = target.getAdapterPosition();

                        int dest = current;
                        if(current == from) dest = to;
                        else if(to > current && from < current) dest = current - 1;
                        else if(to <= current && from > current) dest = current + 1;

                        //Re-order playlist and update index
                        boolean tr = super.onMove(recyclerView, viewHolder, target);
                        MediaBrowserService.getInstance().updateIndexForReorder(dest);

                        return tr;
                    }
                });
        touchHelper.attachToRecyclerView(binding.playList);
        LibraryObjectAdapter adapter = new LibraryObjectAdapter(MediaBrowserService.getInstance().getPlaylist()
                , touchHelper, view ->
        {
            MediaBrowserService.getInstance().setIndex(binding.playList.getChildAdapterPosition(view));
            getMediaController().getTransportControls().play();
        });
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

        //TODO unregister callbacks...
        mediaBrowser.disconnect();
    }

}