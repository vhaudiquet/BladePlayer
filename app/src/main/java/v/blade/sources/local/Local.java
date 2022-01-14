package v.blade.sources.local;

import android.content.ContentResolver;
import android.database.Cursor;
import android.provider.MediaStore;

import androidx.core.content.ContentResolverCompat;
import androidx.fragment.app.Fragment;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import v.blade.BladeApplication;
import v.blade.R;
import v.blade.library.Library;
import v.blade.library.Playlist;
import v.blade.library.Song;
import v.blade.sources.Source;

public class Local extends Source
{
    public static int NAME_RESOURCE = R.string.local;
    public static int DESCRIPTION_RESOURCE = R.string.local_desc;
    public static int IMAGE_RESOURCE = R.drawable.ic_local;

    public Local()
    {
        super();
        this.name = BladeApplication.appContext.getString(NAME_RESOURCE);
    }

    @Override
    public int getImageResource()
    {
        return IMAGE_RESOURCE;
    }

    @Override
    public void synchronizeLibrary()
    {
        ContentResolver contentResolver = BladeApplication.appContext.getContentResolver();
        Cursor musicCursor = ContentResolverCompat.query(contentResolver,
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, null, null, null,
                null, null);
        if(musicCursor != null && musicCursor.moveToFirst())
        {
            int titleColumn = musicCursor.getColumnIndex(MediaStore.MediaColumns.TITLE);
            int idColumn = musicCursor.getColumnIndex(MediaStore.MediaColumns._ID);
            int artistColumn = musicCursor.getColumnIndex(MediaStore.Audio.Artists.ARTIST);
            int albumColumn = musicCursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM);
            int trackNumberColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media.TRACK);

            do
            {
                String title = musicCursor.getString(titleColumn);

                //MediaStore only allows one artist String
                //We will split this string on ',' for songs with multiple artists
                //NOTE : we could split on ' & ', but
                // for example this allows 'Lomepal & Stwo' but splits 'Bigflo & Oli', so no
                //TODO : maybe find a better solution ?
                String artist = musicCursor.getString(artistColumn);
                String[] artists = artist.split(", ");
                //if(artists.length == 1) artists = artist.split(" & ");

                String album = musicCursor.getString(albumColumn);
                long id = musicCursor.getLong(idColumn);
                int track_number = musicCursor.getInt(trackNumberColumn);
                Library.addSong(title, album, artists, this, id, artists, null, track_number, null, null, null);
            }
            while(musicCursor.moveToNext());

            musicCursor.close();
        }
    }

    @Override
    public Fragment getSettingsFragment()
    {
        return null;
    }

    @Override
    public JsonObject saveToJSON()
    {
        JsonObject jsonObject = new JsonObject();
        Gson gson = new Gson();

        jsonObject.add("class", gson.toJsonTree(Local.class.getName(), String.class));

        return jsonObject;
    }

    @Override
    public void restoreFromJSON(JsonObject jsonObject)
    {

    }

    @Override
    public void addSongToPlaylist(Song song, Playlist playlist, Runnable callback, Runnable failureCallback)
    {
        failureCallback.run();
    }

    @Override
    public void createPlaylist(String name, BladeApplication.Callback<Playlist> callback, Runnable failureCallback)
    {
        failureCallback.run();
    }

    @Override
    public void removePlaylist(Playlist playlist, Runnable callback, Runnable failureCallback)
    {
        failureCallback.run();
    }

    @Override
    public void addToLibrary(Song song, Runnable callback, Runnable failureCallback)
    {
        failureCallback.run();
    }

    @Override
    public void removeFromLibrary(Song song, Runnable callback, Runnable failureCallback)
    {
        failureCallback.run();
    }
}
