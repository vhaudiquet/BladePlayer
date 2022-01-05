package v.blade.library;

import com.squareup.picasso.RequestCreator;

import java.util.ArrayList;
import java.util.List;

public class Playlist extends LibraryObject
{
    List<Song> songs;

    public Playlist(String name, RequestCreator image)
    {
        this.name = name;
        this.imageRequest = image;
        this.songs = new ArrayList<>();
    }

    public List<Song> getSongs()
    {
        return null;
    }
}
