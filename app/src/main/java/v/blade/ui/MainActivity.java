package v.blade.ui;

import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.media.session.MediaController;
import android.media.session.PlaybackState;
import android.os.Bundle;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.navigation.NavigationView;

import v.blade.R;
import v.blade.databinding.ActivityMainBinding;
import v.blade.player.MediaBrowserService;
import v.blade.sources.Source;

public class MainActivity extends AppCompatActivity
{
    private AppBarConfiguration mAppBarConfiguration;
    protected ActivityMainBinding binding;

    protected MediaBrowserCompat mediaBrowser;

    private NavHostFragment navHostFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        //Set 'currentPlay' actions
        binding.currentplayLayout.setOnClickListener(view ->
        {
            //Open currentPlay
            Intent intent = new Intent(this, PlayActivity.class);
            startActivity(intent);
        });
        binding.currentplayElementPlaypause.setOnClickListener(view ->
        {
            //Play/pause action
            MediaController mediaController = getMediaController();
            if(mediaController == null) return;

            if(mediaController.getPlaybackState().getState() == PlaybackState.STATE_PLAYING)
                mediaController.getTransportControls().pause();
            else
                mediaController.getTransportControls().play();
        });

        setSupportActionBar(binding.appBarMain.toolbar);
        DrawerLayout drawer = binding.drawerLayout;
        NavigationView navigationView = binding.navView;
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_artists, R.id.nav_albums, R.id.nav_songs, R.id.nav_playlists)
                .setOpenableLayout(drawer)
                .build();
        navHostFragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment_content_main);
        assert navHostFragment != null;
        NavController navController = navHostFragment.getNavController();
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);

        mediaBrowser = new MediaBrowserCompat(this,
                new ComponentName(this, MediaBrowserService.class),
                new MediaBrowserCompat.ConnectionCallback()
                {
                    @Override
                    public void onConnected()
                    {
                        super.onConnected();
                        MediaSessionCompat.Token token = mediaBrowser.getSessionToken();
                        MediaControllerCompat mediaController =
                                new MediaControllerCompat(MainActivity.this, token);
                        MediaControllerCompat.setMediaController(MainActivity.this,
                                mediaController);

                        MediaControllerCompat.Callback mediaControllerCallback = new MediaControllerCompat.Callback()
                        {
                            @Override
                            public void onPlaybackStateChanged(PlaybackStateCompat state)
                            {
                                super.onPlaybackStateChanged(state);

                                //if not already displayed, display 'currentPlay'
                                binding.currentplayLayout.setVisibility(View.VISIBLE);
                                //TODO : resize mainListView layout

                                if(state.getState() == PlaybackStateCompat.STATE_PLAYING)
                                    binding.currentplayElementPlaypause.setImageResource(R.drawable.ic_pause);
                                else
                                    binding.currentplayElementPlaypause.setImageResource(R.drawable.ic_play_arrow);
                            }

                            @Override
                            public void onMetadataChanged(MediaMetadataCompat metadata)
                            {
                                super.onMetadataChanged(metadata);

                                binding.currentplayElementTitle.setText(metadata.getString(MediaMetadataCompat.METADATA_KEY_TITLE));
                                String subtitle = metadata.getString(MediaMetadataCompat.METADATA_KEY_ARTIST) + " - " + metadata.getString(MediaMetadataCompat.METADATA_KEY_ALBUM);
                                binding.currentplayElementSubtitle.setText(subtitle);

                                Bitmap art = metadata.getBitmap(MediaMetadataCompat.METADATA_KEY_ART);
                                if(art == null)
                                    binding.currentplayElementImage.setImageResource(R.drawable.ic_album);
                                else binding.currentplayElementImage.setImageBitmap(art);
                            }
                        };

                        //If mediaSession is playing/paused, display currentPlayLayout
                        if(mediaController.isSessionReady()
                                && (mediaController.getPlaybackState().getState() == PlaybackStateCompat.STATE_PLAYING
                                || mediaController.getPlaybackState().getState() == PlaybackStateCompat.STATE_PAUSED))
                        {
                            mediaControllerCallback.onMetadataChanged(mediaController.getMetadata());
                            mediaControllerCallback.onPlaybackStateChanged(mediaController.getPlaybackState());
                        }

                        //Register a callback so that 'currentPlay' UI stays in sync
                        mediaController.registerCallback(mediaControllerCallback);
                    }

                    @Override
                    public void onConnectionSuspended()
                    {
                        super.onConnectionSuspended();
                        // The Service has crashed ; Disable transport controls until it automatically reconnects
                        binding.currentplayLayout.setVisibility(View.GONE);
                    }

                    @Override
                    public void onConnectionFailed()
                    {
                        super.onConnectionFailed();
                        System.err.println("MediaBrowser connection failed");
                    }
                }, null);
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

    @Override
    protected void onResume()
    {
        super.onResume();
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        int id = item.getItemId();
        if(id == R.id.action_settings)
        {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            return true;
        }
        else if(id == R.id.action_sync)
        {
            if(Source.isSyncing) return false;

            item.setIcon(R.drawable.ic_hourglass);
            Source.synchronizeSources(() ->
                    this.runOnUiThread(() -> item.setIcon(R.drawable.ic_sync)));
            return true;
        }
        else return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onSupportNavigateUp()
    {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }

    @Override
    public void onBackPressed()
    {
        //NOTE : there should be only one child fragment (i dont even know how multiple fragments would be possible)
        if(navHostFragment.getChildFragmentManager().getFragments().size() != 0)
        {
            Fragment child = navHostFragment.getChildFragmentManager().getFragments().get(0);
            if(child instanceof LibraryFragment)
            {
                ((LibraryFragment) child).onBackPressed();
                //We handle backPresses directly in LibraryFragment, so we want to intercept the main back processing
                return;
            }
        }
        super.onBackPressed();
    }
}