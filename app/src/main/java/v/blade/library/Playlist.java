package v.blade.library;

import com.squareup.picasso.Picasso;

import java.util.List;

public class Playlist extends LibraryObject
{
    final List<Song> songs;

    public Playlist(String name, List<Song> songList, String image)
    {
        this.name = name;
        this.imageStr = image;
        this.imageRequest = (image == null || image.equals("")) ? null : Picasso.get().load(image);
        this.songs = songList;
    }

    public List<Song> getSongs()
    {
        return songs;
    }
}
