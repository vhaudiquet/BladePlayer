package v.blade.sources.spotify;

import android.os.Process;
import android.support.v4.media.session.MediaControllerCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Picasso;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Response;
import v.blade.BladeApplication;
import v.blade.R;
import v.blade.library.Library;
import v.blade.library.Song;
import v.blade.player.MediaBrowserService;
import v.blade.ui.ExploreFragment;

public class SpotifyExploreAdapter extends RecyclerView.Adapter<SpotifyExploreAdapter.ViewHolder>
{
    private SpotifyService.PagingObject<? extends SpotifyService.SimplifiedTrackObject> currentTracks;
    private SpotifyService.PagingObject<? extends SpotifyService.SimplifiedAlbumObject> currentAlbums;
    private SpotifyService.PagingObject<? extends SpotifyService.SimplifiedArtistObject> currentArtists;

    private SpotifyService.SimplifiedAlbumObject currentAlbum;

    private SpotifyService.PagingObject<SpotifyService.PlaylistObject> currentPlaylists;

    private final ExploreFragment exploreFragment;
    public SpotifyExploreAdapter(ExploreFragment recyclerView)
    {
        this.exploreFragment = recyclerView;
    }

    public SpotifyExploreAdapter(SpotifyService.SearchResult body, ExploreFragment view)
    {
        this.exploreFragment = view;
        this.currentTracks = body.tracks;
        this.currentAlbums = body.albums;
        this.currentArtists = body.artists;
        this.currentAlbum = null;
    }

    static class ViewHolder extends RecyclerView.ViewHolder
    {
        TextView titleView;
        ImageView imageView;
        TextView subtitleView;
        TextView labelView;

        public ViewHolder(@NonNull View itemView)
        {
            super(itemView);
            titleView = itemView.findViewById(R.id.item_element_title);
            subtitleView = itemView.findViewById(R.id.item_element_subtitle);
            imageView = itemView.findViewById(R.id.item_element_image);
            labelView = itemView.findViewById(R.id.item_element_label);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
    {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_label_layout, parent, false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position)
    {
        //DataSet is songs, albums, artists, playlists ; separators on first item
        int currentTracksLen = currentTracks == null ? 0 : currentTracks.items.length;
        int currentAlbumsLen = currentAlbums == null ? 0 : currentAlbums.items.length;
        int currentArtistsLen = currentArtists == null ? 0 : currentArtists.items.length;

        //Songs
        if(currentTracksLen > position)
        {
            if(position == 0)
            {
                holder.labelView.setVisibility(View.VISIBLE);
                holder.labelView.setText(R.string.songs);
            }
            else
                holder.labelView.setVisibility(View.GONE);

            SpotifyService.SimplifiedTrackObject track = currentTracks.items[position];
            holder.titleView.setText(track.name);

            StringBuilder subtitle = new StringBuilder();
            for(int i = 0; i < track.artists.length; i++)
            {
                subtitle.append(track.artists[i].name);
                if(i != track.artists.length - 1)
                    subtitle.append(", ");
            }
            holder.subtitleView.setText(subtitle.toString());

            SpotifyService.SimplifiedAlbumObject album;
            if(track instanceof SpotifyService.TrackObject)
                album = ((SpotifyService.TrackObject) track).album;
            else
                album = currentAlbum;

            Picasso.get()
                    .load(album.images[album.images.length - 2].url)
                    .into(holder.imageView);

            //OnClick action : obtain handle and play song
            holder.itemView.setOnClickListener(v ->
            {
                String[] artists = new String[track.artists.length];
                String[] artistsImages = new String[track.artists.length];
                for(int i = 0; i < track.artists.length; i++) artists[i] = track.artists[i].name;

                String[] aartists = new String[album.artists.length];
                String[] aartistsImages = new String[album.artists.length];
                for(int i = 0; i < album.artists.length; i++) aartists[i] = album.artists[i].name;

                Song song = Library.addSongHandle(track.name, album.name, artists, exploreFragment.current, track.id, aartists,
                        album.images[album.images.length - 2].url, track.track_number,
                        artistsImages, aartistsImages, album.images[0].url, Spotify.SPOTIFY_IMAGE_LEVEL);

                ArrayList<Song> newList = new ArrayList<>();
                newList.add(song);
                MediaBrowserService.getInstance().setPlaylist(newList);
                MediaBrowserService.getInstance().setIndex(0);
                MediaControllerCompat.getMediaController(exploreFragment.requireActivity()).getTransportControls().play();
            });
        }
        //Albums
        else if(currentAlbumsLen + currentTracksLen > position)
        {
            if(position == currentTracksLen)
            {
                holder.labelView.setVisibility(View.VISIBLE);
                holder.labelView.setText(R.string.albums);
            }
            else
                holder.labelView.setVisibility(View.GONE);

            SpotifyService.SimplifiedAlbumObject currentAlbum = currentAlbums.items[position - currentTracksLen];
            holder.titleView.setText(currentAlbum.name);

            StringBuilder subtitle = new StringBuilder();
            for(int i = 0; i < currentAlbum.artists.length; i++)
            {
                subtitle.append(currentAlbum.artists[i].name);
                if(i != currentAlbum.artists.length - 1)
                    subtitle.append(", ");
            }
            holder.subtitleView.setText(subtitle.toString());

            Picasso.get().load(currentAlbum.images[currentAlbum.images.length - 2].url)
                    .into(holder.imageView);

            //OnClick action : browse album
            holder.itemView.setOnClickListener(v ->
            {
                if(currentAlbum instanceof SpotifyService.AlbumObject)
                {
                    SpotifyService.AlbumObject album = (SpotifyService.AlbumObject) currentAlbum;
                    SpotifyExploreAdapter adapter = new SpotifyExploreAdapter(exploreFragment);
                    adapter.currentTracks = album.tracks;
                    adapter.currentAlbum = album;
                    exploreFragment.updateContent(adapter, album.name, true);
                }
                else
                {
                    BladeApplication.obtainExecutorService().execute(() ->
                    {
                        Process.setThreadPriority(Process.THREAD_PRIORITY_FOREGROUND);

                        Spotify spotify = (Spotify) exploreFragment.current;
                        Call<SpotifyService.PagingObject<SpotifyService.SimplifiedTrackObject>> call =
                            spotify.service.getAlbumTracks(spotify.AUTH_STRING, currentAlbum.id, 50);

                        try
                        {
                            Response<SpotifyService.PagingObject<SpotifyService.SimplifiedTrackObject>> response =
                                    call.execute();

                            if(response.code() == 401)
                            {
                                //Expired token
                                spotify.refreshAccessTokenSync();
                                holder.itemView.callOnClick();
                                return;
                            }

                            SpotifyService.PagingObject<SpotifyService.SimplifiedTrackObject> r = response.body();
                            if(response.code() != 200 || r == null)
                            {
                                System.err.println("BLADE-SPOTIFY: Could not browse album " + currentAlbum.name);
                                exploreFragment.requireActivity().runOnUiThread(() ->
                                        Toast.makeText(exploreFragment.requireContext(),
                                                exploreFragment.getString(R.string.could_not_browse_album, currentAlbum.name),
                                                Toast.LENGTH_SHORT).show());
                                return;
                            }

                            SpotifyExploreAdapter adapter = new SpotifyExploreAdapter(exploreFragment);
                            adapter.currentTracks = r;
                            adapter.currentAlbum = currentAlbum;
                            exploreFragment.requireActivity().runOnUiThread(() ->
                                exploreFragment.updateContent(adapter, currentAlbum.name, true));
                        }
                        catch(IOException e)
                        {
                            exploreFragment.requireActivity().runOnUiThread(() ->
                                    Toast.makeText(exploreFragment.requireContext(),
                                            exploreFragment.getString(R.string.could_not_browse_album, currentAlbum.name),
                                            Toast.LENGTH_SHORT).show());
                        }
                    });
                }
            });
        }
        //Artists
        else if(currentArtistsLen + currentAlbumsLen + currentTracksLen > position)
        {
            if(position == currentTracksLen + currentAlbumsLen)
            {
                holder.labelView.setVisibility(View.VISIBLE);
                holder.labelView.setText(R.string.artists);
            }
            else
                holder.labelView.setVisibility(View.GONE);

            SpotifyService.SimplifiedArtistObject currentArtist = currentArtists.items[position - currentTracksLen - currentAlbumsLen];
            holder.titleView.setText(currentArtist.name);

            holder.subtitleView.setText("");

            holder.imageView.setImageResource(R.drawable.ic_artist);

            //OnClick action : browse artist
            holder.itemView.setOnClickListener(v ->
                    BladeApplication.obtainExecutorService().execute(() ->
                    {
                        Process.setThreadPriority(Process.THREAD_PRIORITY_FOREGROUND);

                        Spotify spotify = (Spotify) exploreFragment.current;
                        Call<SpotifyService.PagingObject<SpotifyService.SimplifiedAlbumObject>> call =
                                spotify.service.getArtistAlbums(spotify.AUTH_STRING, currentArtist.id, 50);

                        try
                        {
                            Response<SpotifyService.PagingObject<SpotifyService.SimplifiedAlbumObject>> response =
                                    call.execute();

                            if(response.code() == 401)
                            {
                                //Expired token
                                spotify.refreshAccessTokenSync();
                                holder.itemView.callOnClick();
                                return;
                            }

                            SpotifyService.PagingObject<SpotifyService.SimplifiedAlbumObject> r = response.body();
                            if(response.code() != 200 || r == null)
                            {
                                System.err.println("BLADE-SPOTIFY: Could not browse artist " + currentArtist.name);
                                exploreFragment.requireActivity().runOnUiThread(() ->
                                        Toast.makeText(exploreFragment.requireContext(),
                                                exploreFragment.getString(R.string.could_not_browse_artist, currentArtist.name),
                                                Toast.LENGTH_SHORT).show());
                                return;
                            }

                            SpotifyExploreAdapter adapter = new SpotifyExploreAdapter(exploreFragment);
                            adapter.currentAlbums = r;
                            exploreFragment.requireActivity().runOnUiThread(() ->
                                    exploreFragment.updateContent(adapter, currentArtist.name, true));
                        }
                        catch(IOException e)
                        {
                            exploreFragment.requireActivity().runOnUiThread(() ->
                                    Toast.makeText(exploreFragment.requireContext(),
                                            exploreFragment.getString(R.string.could_not_browse_artist, currentArtist.name),
                                            Toast.LENGTH_SHORT).show());
                        }
                    }));
        }
    }

    @Override
    public int getItemCount()
    {
        int tracks = currentTracks == null ? 0 : currentTracks.items.length;
        int albums = currentAlbums == null ? 0 : currentAlbums.items.length;
        int artists = currentArtists == null ? 0 : currentArtists.items.length;
        int playlists = currentPlaylists == null ? 0 : currentPlaylists.items.length;
        return tracks + albums + artists + playlists;
    }
}
