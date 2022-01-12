package v.blade.library;

import com.squareup.picasso.Picasso;

import java.util.List;

import v.blade.sources.SourceInformation;

public class Playlist extends LibraryObject
{
    final List<Song> songs;
    private final SourceInformation sourceInformation;

    public Playlist(String name, List<Song> songList, String image, SourceInformation sourceInformation)
    {
        this.name = name;
        this.imageStr = image;
        this.imageRequest = (image == null || image.equals("")) ? null : Picasso.get().load(image);
        this.songs = songList;
        this.sourceInformation = sourceInformation;
    }

    public SourceInformation getSource()
    {
        return sourceInformation;
    }

    public List<Song> getSongs()
    {
        return songs;
    }
}
