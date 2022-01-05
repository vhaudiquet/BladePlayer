package v.blade.library;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

public class Artist extends LibraryObject
{
    List<Album> albums;
    int track_count;

    public Artist(String name, String image)
    {
        this.albums = new ArrayList<>();
        this.name = name;
        this.imageRequest = (image == null || image.equals("")) ? null : Picasso.get().load(image);
        this.imageStr = image;
        this.track_count = 0;
    }

    protected void addAlbum(Album album)
    {
        this.albums.add(album);
    }

    public int getTrackCount()
    {
        return track_count;
    }

    public List<Album> getAlbums()
    {
        return albums;
    }
}
