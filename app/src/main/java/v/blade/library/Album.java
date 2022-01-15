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
    int imageLevel;

    public Album(String name, Artist[] artists, String imageMiniature, String imageBig, int imageLevel)
    {
        this.name = name;
        this.songList = new ArrayList<>();
        this.artists = artists;
        setImage(imageMiniature, imageBig, imageLevel);
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

    public void setImage(String imageMiniature, String imageBig, int imageLevel)
    {
        if(this.imageLevel > imageLevel) return;

        this.imageStr = imageMiniature;
        this.imageBigStr = imageBig;
        this.imageLevel = imageLevel;
        this.imageBig = (imageBig == null || imageBig.equals("")) ? null : Picasso.get().load(imageBig);
        this.imageRequest = (imageMiniature == null || imageMiniature.equals("")) ? null : Picasso.get().load(imageMiniature);
    }
}
