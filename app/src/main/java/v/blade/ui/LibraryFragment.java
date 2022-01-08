package v.blade.ui;

import android.os.Bundle;
import android.support.v4.media.session.MediaControllerCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import java.util.List;

import v.blade.R;
import v.blade.databinding.FragmentLibraryBinding;
import v.blade.library.Album;
import v.blade.library.Artist;
import v.blade.library.Library;
import v.blade.library.LibraryObject;
import v.blade.library.Playlist;
import v.blade.library.Song;
import v.blade.player.MediaBrowserService;

public class LibraryFragment extends Fragment
{
    public static LibraryFragment instance;

    private FragmentLibraryBinding binding;
    public List<? extends LibraryObject> current;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState)
    {
        binding = FragmentLibraryBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        binding.mainListview.setLayoutManager(new LinearLayoutManager(getActivity()));
        updateContent(getTitle(), null);

        instance = this;

        return root;
    }

    public String getTitle()
    {
        return ((MainActivity) requireActivity()).binding == null ? getString(R.string.artists) : ((MainActivity) requireActivity()).binding.appBarMain.toolbar.getTitle().toString();
    }

    /*
     * Update content to list 'replacing', or to root directory
     */
    public void updateContent(String title, List<? extends LibraryObject> replacing)
    {
        if(replacing == null)
        {
            /* we are going back to top directory : artists, albums, songs, playlists */
            if(title.equals(getString(R.string.artists)))
                current = Library.getArtists();
            else if(title.equals(getString(R.string.albums)))
                current = Library.getAlbums();
            else if(title.equals(getString(R.string.songs)))
                current = Library.getSongs();
            else if(title.equals(getString(R.string.playlists)))
                current = Library.getPlaylists();
            else return;
        }
        else
        {
            current = replacing;
        }

        LibraryObjectAdapter adapter = new LibraryObjectAdapter(current, this::onMoreClicked, this::onViewClicked);
        binding.mainListview.setAdapter(adapter);
        if(((MainActivity) requireActivity()).binding != null)
            ((MainActivity) requireActivity()).binding.appBarMain.toolbar.setTitle(title);
    }

    private void onViewClicked(View view)
    {
        int position = binding.mainListview.getChildLayoutPosition(view);
        LibraryObject clicked = current.get(position);
        onElementClicked(clicked, position);
    }

    private void onElementClicked(LibraryObject element, int position)
    {
        if(element instanceof Artist)
            updateContent(element.getName(), ((Artist) element).getAlbums());
        else if(element instanceof Album)
            updateContent(element.getName(), ((Album) element).getSongs());
        else if(element instanceof Playlist)
            updateContent(element.getName(), ((Playlist) element).getSongs());
        else if(element instanceof Song)
        {
            //noinspection unchecked
            MediaBrowserService.getInstance().setPlaylist((List<Song>) current);
            MediaBrowserService.getInstance().setIndex(position);
            MediaControllerCompat.getMediaController(requireActivity()).getTransportControls().play();
        }
    }

    private void onMoreClicked(View view)
    {

    }

    @Override
    public void onDestroyView()
    {
        super.onDestroyView();
        binding = null;
    }
}