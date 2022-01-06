package v.blade.ui;

import android.content.ComponentName;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Bundle;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.view.Menu;
import android.view.MenuItem;

import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
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

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.appBarMain.toolbar);
        DrawerLayout drawer = binding.drawerLayout;
        NavigationView navigationView = binding.navView;
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_artists, R.id.nav_albums, R.id.nav_songs, R.id.nav_playlists)
                .setOpenableLayout(drawer)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
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
                        //TODO : build transport controls
                    }

                    @Override
                    public void onConnectionSuspended()
                    {
                        super.onConnectionSuspended();
                        // The Service has crashed ; Disable transport controls until it automatically reconnects
                        //TODO Disable transport controls until it automatically reconnects
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

            //TODO : handle icon change
            Source.synchronizeSources();
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
        //TODO handle back presses here
        super.onBackPressed();
    }
}