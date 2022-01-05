package v.blade.library;

import com.squareup.picasso.RequestCreator;

import java.util.ArrayList;
import java.util.List;

public class Artist extends LibraryObject
{
    List<Album> albums;
    int track_count;

    public Artist(String name, RequestCreator image)
    {
        this.albums = new ArrayList<>();
        this.name = name;
        this.imageRequest = image;
        this.track_count = 0;
    }

    protected void addAlbum(Album album)
    {
        this.albums.add(album);
    }

    public List<Album> getAlbums()
    {
        return null;
    }
}
