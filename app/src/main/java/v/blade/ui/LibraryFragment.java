package v.blade.ui;

import android.os.Bundle;
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

public class LibraryFragment extends Fragment
{
    private FragmentLibraryBinding binding;
    private List<? extends LibraryObject> current;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState)
    {
        binding = FragmentLibraryBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        binding.mainListview.setLayoutManager(new LinearLayoutManager(getActivity()));
        updateContent(null);

        return root;
    }

    /*
     * Update content to list 'replacing', or to root directory
     */
    private void updateContent(List<? extends LibraryObject> replacing)
    {
        if(replacing == null)
        {
            /* we are going back to top directory : artists, albums, songs, playlists */
            if(requireActivity().getTitle().equals(getString(R.string.artists)))
                current = Library.getArtists();
            else if(requireActivity().getTitle().equals(getString(R.string.albums)))
                current = Library.getAlbums();
            else if(requireActivity().getTitle().equals(getString(R.string.songs)))
                current = Library.getSongs();
            else if(requireActivity().getTitle().equals(getString(R.string.playlists)))
                current = Library.getPlaylists();
        }
        else
        {
            current = replacing;
        }

        LibraryObjectAdapter adapter = new LibraryObjectAdapter(current, this::onViewClicked);
        binding.mainListview.setAdapter(adapter);
    }

    private void onViewClicked(View view)
    {
        int position = binding.mainListview.getChildLayoutPosition(view);
        LibraryObject clicked = current.get(position);
        onElementClicked(clicked);
    }

    private void onElementClicked(LibraryObject element)
    {
        if(element instanceof Artist)
            updateContent(((Artist) element).getAlbums());
        else if(element instanceof Album)
            updateContent(((Album) element).getSongs());
        else if(element instanceof Playlist)
            updateContent(((Playlist) element).getSongs());
        //TODO : if element is Song, change player playlist...
    }

    @Override
    public void onDestroyView()
    {
        super.onDestroyView();
        binding = null;
    }
}