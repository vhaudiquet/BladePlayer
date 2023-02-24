package v.blade.ui;

import android.app.SearchManager;
import android.content.ComponentName;
import android.content.Context;
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
import android.widget.SearchView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.navigation.NavigationView;

import v.blade.BladeApplication;
import v.blade.R;
import v.blade.databinding.ActivityMainBinding;
import v.blade.player.MediaBrowserService;
import v.blade.sources.Source;

public class MainActivity extends AppCompatActivity
{
    private AppBarConfiguration mAppBarConfiguration;
    protected ActivityMainBinding binding;

    protected MediaBrowserCompat mediaBrowser;
    private MediaControllerCompat.Callback mediaControllerCallback;

    private NavHostFragment navHostFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        //First launch dialog
        if(BladeApplication.shouldDisplayFirstLaunchDialog)
        {
            BladeApplication.shouldDisplayFirstLaunchDialog = false;
            Dialogs.openFirstLaunchDialog(this);
        }

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        //Set 'currentPlay' actions
        binding.appBarMain.contentMain.currentplayLayout.setOnClickListener(view ->
        {
            //Open currentPlay
            Intent intent = new Intent(this, PlayActivity.class);
            startActivity(intent);
        });
        binding.appBarMain.contentMain.currentplayElementPlaypause.setOnClickListener(view ->
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
                R.id.nav_artists, R.id.nav_albums, R.id.nav_songs, R.id.nav_playlists, R.id.nav_explore_sources)
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

                        // Restore playlist if needed
                        if(MediaBrowserService.getInstance().getPlaylist() == null ||
                                MediaBrowserService.getInstance().getPlaylist().isEmpty())
                        {
                            System.out.println("BLADE: (MainActivity/onConnected()) Restoring playlist");
                            MediaBrowserService.getInstance().restorePlaylist();
                        }

                        MediaSessionCompat.Token token = mediaBrowser.getSessionToken();
                        MediaControllerCompat mediaController =
                                new MediaControllerCompat(MainActivity.this, token);
                        MediaControllerCompat.setMediaController(MainActivity.this,
                                mediaController);

                        mediaControllerCallback = new MediaControllerCompat.Callback()
                        {
                            @Override
                            public void onPlaybackStateChanged(PlaybackStateCompat state)
                            {
                                super.onPlaybackStateChanged(state);

                                if(state == null || state.getState() == PlaybackStateCompat.STATE_STOPPED)
                                {
                                    //binding.appBarMain.contentMain.currentplayLayout.setVisibility(View.GONE);

                                    // Try to restore playlist
                                    // TODO: Do that in a better way maybe ? (catch when it does not work etc)
                                    MediaBrowserService.getInstance().restorePlaylist();
                                    return;
                                }

                                //if not already displayed, display 'currentPlay'
                                if(binding.appBarMain.contentMain.currentplayLayout.getVisibility() != View.VISIBLE)
                                    binding.appBarMain.contentMain.currentplayLayout.setVisibility(View.VISIBLE);

                                if(state.getState() == PlaybackStateCompat.STATE_PLAYING)
                                    binding.appBarMain.contentMain.currentplayElementPlaypause.setImageResource(R.drawable.ic_pause);
                                else
                                    binding.appBarMain.contentMain.currentplayElementPlaypause.setImageResource(R.drawable.ic_play_arrow);
                            }

                            @Override
                            public void onMetadataChanged(MediaMetadataCompat metadata)
                            {
                                super.onMetadataChanged(metadata);

                                if(metadata == null) return;

                                binding.appBarMain.contentMain.currentplayElementTitle.setText(metadata.getString(MediaMetadataCompat.METADATA_KEY_TITLE));
                                String subtitle = metadata.getString(MediaMetadataCompat.METADATA_KEY_ARTIST) + " - " + metadata.getString(MediaMetadataCompat.METADATA_KEY_ALBUM);
                                binding.appBarMain.contentMain.currentplayElementSubtitle.setText(subtitle);

                                Bitmap art = metadata.getBitmap(MediaMetadataCompat.METADATA_KEY_ART);
                                if(art == null)
                                    binding.appBarMain.contentMain.currentplayElementImage.setImageResource(R.drawable.ic_album);
                                else
                                    binding.appBarMain.contentMain.currentplayElementImage.setImageBitmap(art);
                            }
                        };

                        //If mediaSession is playing/paused, display currentPlayLayout
                        if(mediaController.isSessionReady()
                                && (mediaController.getPlaybackState().getState() == PlaybackStateCompat.STATE_PLAYING
                                || mediaController.getPlaybackState().getState() == PlaybackStateCompat.STATE_PAUSED))
                        {
                            mediaControllerCallback.onPlaybackStateChanged(mediaController.getPlaybackState());
                            mediaControllerCallback.onMetadataChanged(mediaController.getMetadata());
                        }

                        //Register a callback so that 'currentPlay' UI stays in sync
                        mediaController.registerCallback(mediaControllerCallback);
                    }

                    @Override
                    public void onConnectionSuspended()
                    {
                        super.onConnectionSuspended();
                        System.out.println("STOPPED");

                        // Try to restore playlist
                        // TODO: Do that in a better way maybe ? (catch when it does not work etc)
                        MediaBrowserService.getInstance().restorePlaylist();
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

        if(MediaControllerCompat.getMediaController(this) != null)
            MediaControllerCompat.getMediaController(this).unregisterCallback(mediaControllerCallback);

        mediaBrowser.disconnect();
    }

    @Override
    protected void onResume()
    {
        super.onResume();

        MediaControllerCompat mediaController = MediaControllerCompat.getMediaController(this);
        if(mediaController != null)
        {
            mediaControllerCallback.onPlaybackStateChanged(mediaController.getPlaybackState());
            mediaControllerCallback.onMetadataChanged(mediaController.getMetadata());
        }

        setVolumeControlStream(AudioManager.STREAM_MUSIC);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);

        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        SearchView searchView = (SearchView) menu.findItem(R.id.action_search).getActionView();
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        searchView.setQueryHint(getString(R.string.search));

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
                    this.runOnUiThread(() -> item.setIcon(R.drawable.ic_sync_24px)));
            return true;
        }
        else return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onNewIntent(Intent intent)
    {
        super.onNewIntent(intent);

        if(intent != null && intent.getAction() != null && Intent.ACTION_SEARCH.equals(intent.getAction()))
        {
            String query = intent.getStringExtra(SearchManager.QUERY);

            if(navHostFragment.getChildFragmentManager().getFragments().size() != 0)
            {
                Fragment child = navHostFragment.getChildFragmentManager().getFragments().get(0);
                if(child instanceof LibraryFragment)
                    ((LibraryFragment) child).onSearch(query);
                else if(child instanceof ExploreFragment)
                    ((ExploreFragment) child).onSearch(query);
                else
                    Toast.makeText(this, getString(R.string.cant_search_here), Toast.LENGTH_SHORT).show();
            }
        }
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
            else if(child instanceof ExploreFragment)
            {
                ((ExploreFragment) child).onBackPressed();
                return;
            }
        }
        super.onBackPressed();
    }
}