package v.blade.ui;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.media.session.MediaControllerCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

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
    //We keep a global instance of LibraryFragment to be able to update lists on Library update (launch, sync)
    public static LibraryFragment instance;

    public enum CURRENT_TYPE
    {
        LIBRARY, // We are inside a library item
        PLAYLIST, // We are inside a playlist
        SEARCH // We are in a search result
    }

    protected FragmentLibraryBinding binding;
    private List<? extends LibraryObject> current;
    private CURRENT_TYPE currentType;
    private LibraryObject currentObject;

    private static class BackInformation
    {
        private final String title;
        private final List<? extends LibraryObject> list;
        private final CURRENT_TYPE type;
        private final LibraryObject object;

        private final Parcelable recylerViewState;

        private BackInformation(String title, List<? extends LibraryObject> list, CURRENT_TYPE type, LibraryObject object, Parcelable recylerViewState)
        {
            this.title = title;
            this.list = list;
            this.type = type;
            this.object = object;
            this.recylerViewState = recylerViewState;
        }
    }

    private Stack<BackInformation> backStack = new Stack<>();

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState)
    {
        binding = FragmentLibraryBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getActivity());
        linearLayoutManager.setSmoothScrollbarEnabled(true);
        binding.mainListview.setLayoutManager(linearLayoutManager);
        updateContent(getTitle(), null, CURRENT_TYPE.LIBRARY, null);

        instance = this;

        return root;
    }

    public String getTitle()
    {
        return ((MainActivity) requireActivity()).binding == null ? getString(R.string.artists) : ((MainActivity) requireActivity()).binding.appBarMain.toolbar.getTitle().toString();
    }

    /*
     * Update content to list 'replacing', or to root directory
     * If we are updating because going back, we should not push to back : shouldPushToBack is false
     */
    private void updateContent(String title, List<? extends LibraryObject> replacing, CURRENT_TYPE type, LibraryObject object, boolean shouldPushToBack, Parcelable viewState)
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

            currentType = CURRENT_TYPE.LIBRARY;
            currentObject = null;
            //Reset backstack
            backStack = new Stack<>();
        }
        else
        {
            //Push previous state to backStack
            if(shouldPushToBack)
                backStack.push(new BackInformation(getTitle(), current, currentType, currentObject,
                        (binding.mainListview.getLayoutManager() == null ? null :
                                binding.mainListview.getLayoutManager().onSaveInstanceState())));

            current = replacing;
            currentType = type;
            currentObject = object;
        }

        LibraryObjectAdapter adapter = new LibraryObjectAdapter(current, this::onMoreClicked, this::onViewClicked);
        binding.mainListview.setAdapter(adapter);

        if(viewState != null && binding.mainListview.getLayoutManager() != null)
            binding.mainListview.getLayoutManager().onRestoreInstanceState(viewState);

        if(((MainActivity) requireActivity()).binding != null)
            ((MainActivity) requireActivity()).binding.appBarMain.toolbar.setTitle(title);
    }

    public void updateContent(String title, List<? extends LibraryObject> replacing, CURRENT_TYPE type, LibraryObject currentObject)
    {
        updateContent(title, replacing, type, currentObject, true, null);
    }

    private void updateContent(BackInformation backInformation)
    {
        updateContent(backInformation.title, backInformation.list, backInformation.type, backInformation.object, false, backInformation.recylerViewState);
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
            updateContent(element.getName(), ((Artist) element).getAlbums(), CURRENT_TYPE.LIBRARY, element);
        else if(element instanceof Album)
            updateContent(element.getName(), ((Album) element).getSongs(), CURRENT_TYPE.LIBRARY, element);
        else if(element instanceof Playlist)
            updateContent(element.getName(), ((Playlist) element).getSongs(), CURRENT_TYPE.PLAYLIST, element);
        else if(element instanceof Song)
        {
            if(currentType == CURRENT_TYPE.SEARCH)
            {
                //On search, we only play current song
                ArrayList<Song> list = new ArrayList<>();
                list.add((Song) element);
                MediaBrowserService.getInstance().setPlaylist(list);
                MediaBrowserService.getInstance().setIndex(0);
                MediaControllerCompat.getMediaController(requireActivity()).getTransportControls().play();
                return;
            }

            //noinspection unchecked
            MediaBrowserService.getInstance().setPlaylist(new ArrayList<>((List<Song>) current));
            MediaBrowserService.getInstance().setIndex(position);
            MediaControllerCompat.getMediaController(requireActivity()).getTransportControls().play();
        }
    }

    //TODO : maybe fix that ? switch on something else ?
    @SuppressLint("NonConstantResourceId")
    private void onMoreClicked(View view)
    {
        //Obtain object and menu
        LibraryObject element = (LibraryObject) view.getTag();
        PopupMenu popupMenu = new PopupMenu(requireContext(), view);
        popupMenu.inflate(R.menu.item_more);

        //Set element visibility depending on context
        if(element instanceof Song)
        {
            popupMenu.getMenu().getItem(3).setVisible(true);
            popupMenu.getMenu().getItem(4).setVisible(true);
            if(currentType == CURRENT_TYPE.PLAYLIST)
                popupMenu.getMenu().getItem(5).setVisible(true);
        }
        else if(element instanceof Playlist)
        {
            popupMenu.getMenu().getItem(6).setVisible(true);
        }

        //Set actions
        popupMenu.setOnMenuItemClickListener(item ->
        {
            switch(item.getItemId())
            {
                case R.id.action_play:
                    ArrayList<Song> playlist = new ArrayList<>();
                    if(element instanceof Song) playlist.add((Song) element);
                    else if(element instanceof Album) playlist.addAll(((Album) element).getSongs());
                    else if(element instanceof Artist)
                        for(Album a : ((Artist) element).getAlbums()) playlist.addAll(a.getSongs());
                    else if(element instanceof Playlist)
                        playlist.addAll(((Playlist) element).getSongs());
                    MediaBrowserService.getInstance().setPlaylist(playlist);
                    MediaBrowserService.getInstance().setIndex(0);
                    LibraryFragment.this.requireActivity().getMediaController().getTransportControls().play();
                    return true;
                case R.id.action_play_next:
                    ArrayList<Song> playlistAddNext = new ArrayList<>();
                    if(element instanceof Song) playlistAddNext.add((Song) element);
                    else if(element instanceof Album)
                        playlistAddNext.addAll(((Album) element).getSongs());
                    else if(element instanceof Artist) for(Album a : ((Artist) element).getAlbums())
                        playlistAddNext.addAll(a.getSongs());
                    else if(element instanceof Playlist)
                        playlistAddNext.addAll(((Playlist) element).getSongs());
                    if(MediaBrowserService.getInstance().getPlaylist() != null && !MediaBrowserService.getInstance().getPlaylist().isEmpty())
                        MediaBrowserService.getInstance().getPlaylist().addAll(MediaBrowserService.getInstance().getIndex() + 1, playlistAddNext);
                    else
                    {
                        MediaBrowserService.getInstance().setPlaylist(playlistAddNext);
                        MediaBrowserService.getInstance().setIndex(0);
                        LibraryFragment.this.requireActivity().getMediaController().getTransportControls().play();
                    }
                    return true;
                case R.id.action_add_to_playlist:
                    ArrayList<Song> playlistAdd = new ArrayList<>();
                    if(element instanceof Song) playlistAdd.add((Song) element);
                    else if(element instanceof Album)
                        playlistAdd.addAll(((Album) element).getSongs());
                    else if(element instanceof Artist) for(Album a : ((Artist) element).getAlbums())
                        playlistAdd.addAll(a.getSongs());
                    else if(element instanceof Playlist)
                        playlistAdd.addAll(((Playlist) element).getSongs());
                    if(MediaBrowserService.getInstance().getPlaylist() != null && !MediaBrowserService.getInstance().getPlaylist().isEmpty())
                        MediaBrowserService.getInstance().getPlaylist().addAll(playlistAdd);
                    else
                    {
                        MediaBrowserService.getInstance().setPlaylist(playlistAdd);
                        MediaBrowserService.getInstance().setIndex(0);
                        LibraryFragment.this.requireActivity().getMediaController().getTransportControls().play();
                    }
                    return true;
                case R.id.action_add_to_list:
                    assert element instanceof Song;
                    Dialogs.openAddToPlaylistDialog(requireActivity(), (Song) element);
                    return true;
                case R.id.action_remove_from_library:
                    assert element instanceof Playlist;
                    Dialogs.openDeletePlaylistDialog(requireActivity(), (Playlist) element);
                    return true;
                case R.id.action_manage_libraries:
                    assert element instanceof Song;
                    Dialogs.openManageLibrariesDialog(requireActivity(), (Song) element);
                    return true;
                case R.id.action_remove_from_list:
                    assert element instanceof Song;
                    Dialogs.openRemoveFromPlaylistDialog(requireActivity(), (Song) element, (Playlist) currentObject);
                    return true;
                case R.id.action_explore:
                    Dialogs.openExploreDialog(requireActivity(), element);
                    return true;
            }
            return false;
        });
        popupMenu.show();
    }

    @Override
    public void onDestroyView()
    {
        super.onDestroyView();
        binding = null;
    }

    public void onBackPressed()
    {
        if(backStack.isEmpty())
            requireActivity().finish();
        else
            updateContent(backStack.pop());
    }

    protected void onSearch(String query)
    {
        updateContent(getString(R.string.search), Library.search(query), CURRENT_TYPE.SEARCH, null);
    }
}