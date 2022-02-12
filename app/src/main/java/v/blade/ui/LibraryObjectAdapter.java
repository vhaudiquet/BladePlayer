package v.blade.ui;

import android.annotation.SuppressLint;
import android.database.DataSetObserver;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.RequestCreator;

import java.util.ArrayList;
import java.util.List;

import v.blade.R;
import v.blade.library.Album;
import v.blade.library.Artist;
import v.blade.library.LibraryObject;
import v.blade.library.Playlist;
import v.blade.library.Separator;
import v.blade.library.Song;

public class LibraryObjectAdapter extends RecyclerView.Adapter<LibraryObjectAdapter.ViewHolder> implements ListAdapter
{
    class ViewHolder extends RecyclerView.ViewHolder
    {
        TextView titleView;
        TextView subtitleView;
        ImageView imageView;
        ImageView moreView;
        ImageView subimageView;

        //cf. https://stackoverflow.com/questions/47107105/android-button-has-setontouchlistener-called-on-it-but-does-not-override-perform
        //for example for explication
        @SuppressLint("ClickableViewAccessibility")
        public ViewHolder(@NonNull View itemView)
        {
            super(itemView);

            titleView = itemView.findViewById(R.id.item_element_title);
            subtitleView = itemView.findViewById(R.id.item_element_subtitle);
            imageView = itemView.findViewById(R.id.item_element_image);
            moreView = itemView.findViewById(R.id.item_element_more);

            subimageView = itemView.findViewById(R.id.item_element_subimage);

            if(moreView != null && touchHelper != null)
            {
                moreView.setImageResource(R.drawable.ic_reorder_24px);
                moreView.setOnTouchListener((view1, motionEvent) ->
                {
                    touchHelper.startDrag(this);
                    return true;
                });
            }
            else if(moreView != null && moreClickListener != null)
            {
                moreView.setImageResource(R.drawable.ic_more_vert);
                moreView.setOnClickListener(moreClickListener);
            }
            else if(moreView != null) moreView.setVisibility(View.GONE);
        }
    }

    private final List<? extends LibraryObject> objects;
    private View.OnClickListener moreClickListener;
    private ItemTouchHelper touchHelper;
    private View.OnClickListener clickListener;

    private int selectedPosition = -1;

    public LibraryObjectAdapter(List<? extends LibraryObject> objects, View.OnClickListener clickListener)
    {
        this.objects = objects == null ? new ArrayList<>() : objects;
        this.clickListener = clickListener;
    }

    public LibraryObjectAdapter(List<? extends LibraryObject> objects, View.OnClickListener moreClickListener, View.OnClickListener clickListener)
    {
        this(objects, clickListener);
        this.moreClickListener = moreClickListener;
        this.touchHelper = null;
    }

    public LibraryObjectAdapter(List<? extends LibraryObject> objects, ItemTouchHelper touchHelper, View.OnClickListener clickListener)
    {
        this(objects, clickListener);
        this.touchHelper = touchHelper;
        this.moreClickListener = null;
    }

    public void setSelectedPosition(int position)
    {
        this.selectedPosition = position;
    }

    public void setClickListener(View.OnClickListener clickListener)
    {
        this.clickListener = clickListener;
    }

    @Override
    public boolean areAllItemsEnabled()
    {
        return true;
    }

    @Override
    public boolean isEnabled(int i)
    {
        return true;
    }

    @Override
    public void registerDataSetObserver(DataSetObserver dataSetObserver)
    {
    }

    @Override
    public void unregisterDataSetObserver(DataSetObserver dataSetObserver)
    {
    }

    @Override
    public int getCount()
    {
        return objects.size();
    }

    @Override
    public LibraryObject getItem(int i)
    {
        return objects.get(i);
    }

    @Override
    public View getView(int i, View convertView, ViewGroup parent)
    {
        ViewHolder viewHolder;
        if(convertView == null)
        {
            convertView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_layout, parent, false);
            viewHolder = new ViewHolder(convertView);
            viewHolder.itemView.setOnClickListener(clickListener);
            convertView.setTag(viewHolder);
        }
        else viewHolder = (ViewHolder) convertView.getTag();

        onBindViewHolder(viewHolder, i);

        return convertView;
    }

    @Override
    public boolean isEmpty()
    {
        return objects.isEmpty();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
    {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(viewType == 0 ? R.layout.item_layout : R.layout.item_label_layout, parent, false);

        ViewHolder viewHolder = new ViewHolder(view);
        if(viewType == 0)
            viewHolder.itemView.setOnClickListener(clickListener);

        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder viewHolder, int i)
    {
        LibraryObject current = getItem(i);

        viewHolder.titleView.setText(current.getName());

        RequestCreator image = current.getImageRequest();
        if(viewHolder.imageView != null)
        {
            if(image != null)
                image.into(viewHolder.imageView);
            else if(current instanceof Artist)
                viewHolder.imageView.setImageResource(R.drawable.ic_artist);
            else if(current instanceof Album || current instanceof Song)
                viewHolder.imageView.setImageResource(R.drawable.ic_album);
            else if(current instanceof Playlist)
                viewHolder.imageView.setImageResource(R.drawable.ic_playlist);
        }

        //Clear subimageview
        if(viewHolder.subimageView != null)
            viewHolder.subimageView.setVisibility(View.GONE);

        if(viewHolder.subtitleView != null)
        {
            if(current instanceof Song)
            {
                viewHolder.subtitleView.setText(((Song) current).getArtistsString());
            }
            else if(current instanceof Album)
            {
                viewHolder.subtitleView.setText(((Album) current).getArtistsString());
            }
            else if(current instanceof Artist)
            {
                String artistTrackCount = ((Artist) current).getTrackCount() + " "
                        + viewHolder.itemView.getContext().getString(R.string.songs).toLowerCase();
                viewHolder.subtitleView.setText(artistTrackCount);
            }
            else if(current instanceof Playlist)
            {
                if(((Playlist) current).getSongs() != null)
                {
                    String subtitle = ((Playlist) current).getSongs().size() + " " +
                            viewHolder.itemView.getContext().getString(R.string.songs).toLowerCase();
                    if(((Playlist) current).getSubtitle() != null && !((Playlist) current).getSubtitle().equals(""))
                        subtitle += (" \u00B7 " + ((Playlist) current).getSubtitle());

                    viewHolder.subtitleView.setText(subtitle);
                }
                else viewHolder.subtitleView.setText("");

                //Set subimage view
                if(((Playlist) current).getSource() != null)
                {
                    viewHolder.subimageView.setVisibility(View.VISIBLE);
                    viewHolder.subimageView.setImageResource(((Playlist) current).getSource().source.getImageResource());
                }
            }
        }

        //If 'moreClickListener', put object as more view tag
        if(viewHolder.moreView != null && moreClickListener != null)
            viewHolder.moreView.setTag(current);

        //Change background if position is selected
        if(i == selectedPosition)
        {
            TypedValue colorControlActivated = new TypedValue();
            viewHolder.itemView.getContext().getTheme().resolveAttribute(R.attr.colorControlActivated, colorControlActivated, true);
            viewHolder.itemView.setBackground(AppCompatResources.getDrawable(viewHolder.itemView.getContext(), colorControlActivated.resourceId));
        }
        else
        {
            TypedValue colorControl = new TypedValue();
            viewHolder.itemView.getContext().getTheme().resolveAttribute(R.attr.selectableItemBackground, colorControl, true);
            viewHolder.itemView.setBackground(AppCompatResources.getDrawable(viewHolder.itemView.getContext(), colorControl.resourceId));
        }
    }

    @Override
    public int getItemCount()
    {
        return objects.size();
    }


    @Override
    public int getViewTypeCount()
    {
        //There are 2 view types : libraryobject and label
        return 2;
    }

    @Override
    public int getItemViewType(int i)
    {
        if(getItem(i) instanceof Separator) return 1;

        return 0;
    }
}
