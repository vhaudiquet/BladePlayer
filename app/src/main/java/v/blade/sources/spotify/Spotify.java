package v.blade.sources.spotify;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import v.blade.R;
import v.blade.databinding.SettingsFragmentSpotifyBinding;
import v.blade.sources.Source;

/*
 * Spotify strategy :
 * - For access to library, we do Web API access using official AUTH lib + Retrofit
 * - For the player, we use librespot-java (the player part)
 * It would be nice to use librespot for everything, but i don't think it is possible to
 * use it 'as is' for web api access...
 */
public class Spotify extends Source
{
    public static int NAME_RESOURCE = R.string.spotify;
    public static int DESCRIPTION_RESOURCE = R.string.spotify_desc;
    public static int IMAGE_RESOURCE = R.drawable.ic_spotify;

    public static class SettingsFragment extends Fragment
    {
        private SettingsFragmentSpotifyBinding binding;

        private SettingsFragment()
        {
            super(R.layout.settings_fragment_spotify);
        }

        public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
        {
            binding = SettingsFragmentSpotifyBinding.inflate(inflater, container, false);
            return binding.getRoot();
        }
    }

    @Override
    public int getImageResource()
    {
        return IMAGE_RESOURCE;
    }

    @Override
    public void synchronizeLibrary()
    {

    }

    @Override
    public Fragment getSettingsFragment()
    {
        return new Spotify.SettingsFragment();
    }
}
