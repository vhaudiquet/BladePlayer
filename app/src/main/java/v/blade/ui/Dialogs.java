package v.blade.ui;

import android.app.Activity;
import android.content.DialogInterface;
import android.database.DataSetObserver;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SwitchCompat;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;

import v.blade.BladeApplication;
import v.blade.R;
import v.blade.library.Library;
import v.blade.library.LibraryObject;
import v.blade.library.Playlist;
import v.blade.library.Song;
import v.blade.sources.Source;
import v.blade.sources.SourceInformation;

public class Dialogs
{
    public static void openAddToPlaylistDialog(Activity context, Song toAdd)
    {
        //Build the lists of playlists suitable to receive toAdd
        ArrayList<Playlist> playlists = new ArrayList<>();

        //Create the "new playlist" option
        Playlist dummyNew = new Playlist(context.getString(R.string.new_playlist), null, null, null, null);
        dummyNew.setImageRequest(Picasso.get().load(R.drawable.ic_playlist_add));
        playlists.add(dummyNew);

        for(Playlist playlist : Library.getPlaylists())
        {
            for(SourceInformation s : toAdd.getSources())
            {
                if(s.source == playlist.getSource().source)
                {
                    playlists.add(playlist);
                    break;
                }
            }
        }

        //Build adapter and dialog with clickListener
        LibraryObjectAdapter adapter = new LibraryObjectAdapter(playlists, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(context)
                .setTitle(context.getString(R.string.add_to_playlist))
                .setAdapter(adapter, null);
        final AlertDialog dialog = builder.create();

        adapter.setClickListener(view ->
        {
            int position = dialog.getListView().getPositionForView(view);

            if(position == 0)
            {
                openCreatePlaylistDialog(context, toAdd);
                dialog.dismiss();
                return;
            }

            Playlist current = (Playlist) adapter.getItem(position);
            Source source = current.getSource().source;

            source.addSongToPlaylist(toAdd, current, () -> context.runOnUiThread(() ->
                            Toast.makeText(context, context.getString(R.string.song_added_to_list, toAdd.getName(), current.getName()),
                                    Toast.LENGTH_SHORT).show()),
                    () -> context.runOnUiThread(() ->
                            Toast.makeText(context, context.getString(R.string.song_added_to_list_error, toAdd.getName(), current.getName()),
                                    Toast.LENGTH_SHORT).show()));

            dialog.dismiss();
        });

        dialog.show();
    }

    protected static void openCreatePlaylistDialog(Activity context, Song first)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(context)
                .setTitle(R.string.new_playlist)
                .setView(R.layout.dialog_create_playlist);

        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(dialogInterface ->
        {
            Spinner sourceList = dialog.findViewById(R.id.playlist_source);
            assert sourceList != null;
            sourceList.setAdapter(new SpinnerAdapter()
            {
                class ViewHolder
                {
                    ImageView image;
                    TextView title;
                }

                @Override
                public View getDropDownView(int position, View convertView, ViewGroup parent)
                {
                    return getView(position, convertView, parent);
                }

                @Override
                public void registerDataSetObserver(DataSetObserver observer)
                {

                }

                @Override
                public void unregisterDataSetObserver(DataSetObserver observer)
                {

                }

                @Override
                public int getCount()
                {
                    return first.getSources().size();
                }

                @Override
                public Source getItem(int position)
                {
                    return first.getSources().get(position).source;
                }

                @Override
                public long getItemId(int position)
                {
                    return position;
                }

                @Override
                public boolean hasStableIds()
                {
                    return true;
                }

                @Override
                public View getView(int position, View convertView, ViewGroup parent)
                {
                    ViewHolder viewHolder;

                    if(convertView == null)
                    {
                        viewHolder = new ViewHolder();
                        convertView = LayoutInflater.from(parent.getContext())
                                .inflate(R.layout.item_simple_layout, parent, false);

                        viewHolder.title = convertView.findViewById(R.id.item_element_title);
                        viewHolder.image = convertView.findViewById(R.id.item_element_image);

                        convertView.setTag(viewHolder);
                    }
                    else viewHolder = (ViewHolder) convertView.getTag();

                    Source current = getItem(position);
                    viewHolder.title.setText(current.getName());
                    viewHolder.image.setImageResource(current.getImageResource());

                    return convertView;
                }

                @Override
                public int getItemViewType(int position)
                {
                    return 0;
                }

                @Override
                public int getViewTypeCount()
                {
                    return 1;
                }

                @Override
                public boolean isEmpty()
                {
                    return false;
                }
            });
        });
        dialog.setButton(DialogInterface.BUTTON_POSITIVE, context.getString(R.string.ok), (dialogInterface, which) ->
        {
            Spinner spinner = dialog.findViewById(R.id.playlist_source);
            assert spinner != null;
            Source current = (Source) spinner.getSelectedItem();
            EditText name = dialog.findViewById(R.id.playlist_name);
            assert name != null;

            current.createPlaylist(name.getText().toString(), new BladeApplication.Callback<Playlist>()
            {
                @Override
                public void run(Playlist playlist)
                {
                    current.addSongToPlaylist(first, playlist, () -> context.runOnUiThread(() ->
                                    Toast.makeText(context, context.getString(R.string.song_added_to_list, first.getName(), playlist.getName()),
                                            Toast.LENGTH_SHORT).show()),
                            () -> context.runOnUiThread(() ->
                                    Toast.makeText(context, context.getString(R.string.song_added_to_list_error, first.getName(), playlist.getName()),
                                            Toast.LENGTH_SHORT).show()));
                }
            }, () -> context.runOnUiThread(() ->
                    Toast.makeText(context, context.getString(R.string.could_not_create_playlist, name.getText().toString()), Toast.LENGTH_SHORT).show()));

            dialog.dismiss();
        });
        dialog.show();
    }

    protected static void openDeletePlaylistDialog(Activity context, Playlist playlist)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(context)
                .setTitle(R.string.delete_playlist)
                .setMessage(context.getString(R.string.are_you_sure_delete_playlist, playlist.getName()))
                .setPositiveButton(R.string.yes, (dialog, which) ->
                {
                    playlist.getSource().source.removePlaylist(playlist, () ->
                                    context.runOnUiThread(() ->
                                            Toast.makeText(context, context.getString(R.string.playlist_removed, playlist.getName()), Toast.LENGTH_SHORT).show()),
                            () -> context.runOnUiThread(() ->
                                    Toast.makeText(context, context.getString(R.string.playlist_could_not_remove, playlist.getName()), Toast.LENGTH_SHORT).show()));

                    dialog.dismiss();
                })
                .setNegativeButton(R.string.no, (dialog, which) -> dialog.dismiss());
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    public static void openManageLibrariesDialog(Activity context, Song song)
    {
        BaseAdapter adapter = new BaseAdapter()
        {
            class ViewHolder
            {
                ImageView imageView;
                SwitchCompat switchView;
            }

            @Override
            public int getCount()
            {
                return song.getSources().size();
            }

            @Override
            public Source getItem(int position)
            {
                return song.getSources().get(position).source;
            }

            @Override
            public long getItemId(int position)
            {
                return position;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent)
            {
                ViewHolder viewHolder;
                if(convertView == null)
                {
                    convertView = LayoutInflater.from(parent.getContext())
                            .inflate(R.layout.item_switch, parent, false);
                    viewHolder = new ViewHolder();
                    viewHolder.imageView = convertView.findViewById(R.id.item_element_image);
                    viewHolder.switchView = convertView.findViewById(R.id.item_element_switch);
                    convertView.setTag(viewHolder);
                }
                else viewHolder = (ViewHolder) convertView.getTag();

                Source current = getItem(position);
                viewHolder.imageView.setImageResource(current.getImageResource());
                viewHolder.switchView.setText(current.getName());

                //Enabled :
                //For each source, we are either 'handled' or 'in library'
                viewHolder.switchView.setChecked(!song.getSources().get(position).handled);

                //TODO : disable switch for sources that does not support adding ?

                viewHolder.switchView.setOnClickListener(view ->
                {
                    //Check status and either add or remove
                    if(song.getSources().get(position).handled)
                    {
                        song.getSources().get(position).source.addToLibrary(song, () ->
                                        context.runOnUiThread(() ->
                                                Toast.makeText(view.getContext(), context.getString(R.string.song_added_to_library, song.getName()), Toast.LENGTH_SHORT).show()),
                                () -> context.runOnUiThread(() ->
                                {
                                    Toast.makeText(view.getContext(), context.getString(R.string.song_added_to_library_error, song.getName()), Toast.LENGTH_SHORT).show();
                                    viewHolder.switchView.setChecked(false);
                                }));
                    }
                    else
                    {
                        song.getSources().get(position).source.removeFromLibrary(song, () ->
                                        context.runOnUiThread(() ->
                                                Toast.makeText(view.getContext(), context.getString(R.string.song_removed_from_library, song.getName()), Toast.LENGTH_SHORT).show()),
                                () -> context.runOnUiThread(() ->
                                {
                                    Toast.makeText(view.getContext(), context.getString(R.string.song_removed_from_library_error, song.getName()), Toast.LENGTH_SHORT).show();
                                    viewHolder.switchView.setChecked(true);
                                }));
                    }
                });

                return convertView;
            }
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(context)
                .setTitle(R.string.manage_libraries)
                .setAdapter(adapter, ((dialog, which) ->
                {
                }));
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    protected static void openRemoveFromPlaylistDialog(Activity context, Song song, Playlist playlist)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(context)
                .setTitle(R.string.remove_from_list)
                .setMessage(context.getString(R.string.are_you_sure_remove_from_list, song.getName(), playlist.getName()))
                .setPositiveButton(R.string.yes, (dialog, which) ->
                {
                    playlist.getSource().source.removeFromPlaylist(song, playlist, () ->
                                    context.runOnUiThread(() ->
                                            Toast.makeText(context, context.getString(R.string.song_removed_from_list, song.getName(), playlist.getName()), Toast.LENGTH_SHORT).show()),
                            () -> context.runOnUiThread(() ->
                                    Toast.makeText(context, context.getString(R.string.song_removed_from_list_error, song.getName(), playlist.getName()), Toast.LENGTH_SHORT).show()));

                    dialog.dismiss();
                })
                .setNegativeButton(R.string.no, (dialog, which) -> dialog.dismiss());
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    protected static void openFirstLaunchDialog(Activity context)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(context)
                .setTitle(R.string.welcome_to_blade)
                .setMessage(R.string.welcome_message)
                .setPositiveButton(R.string.ok, ((dialog, which) -> dialog.dismiss()));
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    public static void openExploreDialog(Activity context, LibraryObject current)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(context)
                .setTitle(R.string.explore);

        AlertDialog dialog = builder.create();
        dialog.show();
    }
}
