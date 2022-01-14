package v.blade.library;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.RequestCreator;

import java.util.ArrayList;
import java.util.List;

public class Album extends LibraryObject
{
    Artist[] artists;
    List<Song> songList;
    String imageBigStr;
    RequestCreator imageBig;

    public Album(String name, Artist[] artists, String imageMiniature, String imageBig)
    {
        this.name = name;
        this.songList = new ArrayList<>();
        this.artists = artists;
        this.imageBigStr = imageBig;
        this.imageRequest = (imageMiniature == null || imageMiniature.equals("")) ? null : Picasso.get().load(imageMiniature);
        this.imageBig = (imageBig == null || imageBig.equals("")) ? null : Picasso.get().load(imageBig);
        this.imageStr = imageMiniature;
    }

    protected void addSong(Song s)
    {
        this.songList.add(s);
    }

    public Artist[] getArtists()
    {
        return artists;
    }

    public String getArtistsString()
    {
        StringBuilder sb = new StringBuilder();
        for(int i = 0; i < artists.length; i++)
        {
            sb = sb.append(artists[i].name);
            if(i != artists.length - 1) sb = sb.append(", ");
        }
        return sb.toString();
    }

    public List<Song> getSongs()
    {
        return songList;
    }

    public String getImageBigStr()
    {
        return imageBigStr;
    }
}
