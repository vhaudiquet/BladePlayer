package v.blade.ui;

import android.database.DataSetObserver;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.RequestCreator;

import java.util.ArrayList;

import v.blade.R;
import v.blade.library.LibraryObject;

public class LibraryObjectAdapter extends RecyclerView.Adapter<LibraryObjectAdapter.ViewHolder> implements ListAdapter
{
    static class ViewHolder extends RecyclerView.ViewHolder
    {
        TextView titleView;
        TextView subtitleView;
        ImageView imageView;
        ImageView moreView;

        public ViewHolder(@NonNull View itemView)
        {
            super(itemView);
            titleView = itemView.findViewById(R.id.item_element_title);
            subtitleView = itemView.findViewById(R.id.item_element_subtitle);
            imageView = itemView.findViewById(R.id.item_element_image);
            moreView = itemView.findViewById(R.id.item_element_more);
        }
    }

    private ArrayList<LibraryObject> objects;

    public LibraryObjectAdapter(ArrayList<LibraryObject> objects)
    {
        this.objects = objects;
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
            convertView.setTag(viewHolder);
        }
        else viewHolder = (ViewHolder) convertView.getTag();

        onBindViewHolder(viewHolder, i);

        return convertView;
    }

    @Override
    public int getViewTypeCount()
    {
        return 1;
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
                .inflate(R.layout.item_layout, parent, false);
        ViewHolder viewHolder = new ViewHolder(view);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder viewHolder, int i)
    {
        LibraryObject current = getItem(i);
        viewHolder.titleView.setText(current.getName());
        RequestCreator image = current.getImageRequest();
        if(image != null) image.into(viewHolder.imageView);
    }

    @Override
    public int getItemCount()
    {
        return objects.size();
    }
}
