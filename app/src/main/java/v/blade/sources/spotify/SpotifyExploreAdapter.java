package v.blade.sources.spotify;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Picasso;

import v.blade.R;
import v.blade.ui.ExploreFragment;

public class SpotifyExploreAdapter extends RecyclerView.Adapter<SpotifyExploreAdapter.ViewHolder>
{
    private SpotifyService.PagingObject<SpotifyService.TrackObject> currentTracks;
    private SpotifyService.PagingObject<SpotifyService.AlbumObject> currentAlbums;
    private SpotifyService.PagingObject<SpotifyService.ArtistObject> currentArtists;
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

            holder.titleView.setText(currentTracks.items[position].name);

            StringBuilder subtitle = new StringBuilder();
            for(int i = 0; i < currentTracks.items[position].artists.length; i++)
            {
                subtitle.append(currentTracks.items[position].artists[i].name);
                if(i != currentTracks.items[position].artists.length - 1)
                    subtitle.append(", ");
            }
            holder.subtitleView.setText(subtitle.toString());

            Picasso.get()
                    .load(currentTracks.items[position].album.images[currentTracks.items[position].album.images.length - 2].url)
                    .into(holder.imageView);
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

            SpotifyService.AlbumObject currentAlbum = currentAlbums.items[position - currentTracksLen];
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
        }
        else if(currentArtistsLen + currentAlbumsLen + currentTracksLen > position)
        {
            if(position == currentTracksLen + currentAlbumsLen)
            {
                holder.labelView.setVisibility(View.VISIBLE);
                holder.labelView.setText(R.string.artists);
            }
            else
                holder.labelView.setVisibility(View.GONE);

            SpotifyService.ArtistObject currentArtist = currentArtists.items[position - currentTracksLen - currentAlbumsLen];
            holder.titleView.setText(currentArtist.name);

            holder.subtitleView.setText("");

            holder.imageView.setImageResource(R.drawable.ic_artist);
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
