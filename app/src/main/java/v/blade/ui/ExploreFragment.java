package v.blade.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Stack;

import v.blade.R;
import v.blade.databinding.FragmentExploreBinding;
import v.blade.sources.Source;

public class ExploreFragment extends Fragment
{
    private static class BackInformation
    {
        final RecyclerView.Adapter<?> adapter;
        final String title;

        BackInformation(String title, RecyclerView.Adapter<?> adapter)
        {
            this.adapter = adapter;
            this.title = title;
        }
    }

    public FragmentExploreBinding binding;
    private Stack<BackInformation> backStack;
    public Source current;

    private class SourceAdapter extends RecyclerView.Adapter<SourceAdapter.ViewHolder>
    {
        class ViewHolder extends RecyclerView.ViewHolder
        {
            ImageView elementImage;
            TextView elementTitle;

            public ViewHolder(@NonNull View itemView)
            {
                super(itemView);

                elementImage = itemView.findViewById(R.id.item_element_image);
                elementTitle = itemView.findViewById(R.id.item_element_title);
            }
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
        {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_layout, parent, false);
            ViewHolder viewHolder = new ViewHolder(view);

            view.setOnClickListener(v ->
            {
                int pos = binding.exploreSourcesListview.getChildAdapterPosition(v);
                Source current = Source.SOURCES.get(pos);

                ExploreFragment.this.current = current;
                current.explore(ExploreFragment.this);
            });

            return viewHolder;
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position)
        {
            Source current = Source.SOURCES.get(position);
            holder.elementTitle.setText(current.getName());
            holder.elementImage.setImageResource(current.getImageResource());
        }

        @Override
        public int getItemCount()
        {
            return Source.SOURCES.size();
        }
    }

    public String getTitle()
    {
        return ((MainActivity) requireActivity()).binding == null ? "" : ((MainActivity) requireActivity()).binding.appBarMain.toolbar.getTitle().toString();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        binding = FragmentExploreBinding.inflate(inflater, container, false);

        binding.exploreSourcesListview.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.exploreSourcesListview.setAdapter(new SourceAdapter());

        current = null;
        backStack = new Stack<>();

        return binding.getRoot();
    }

    boolean lastSearched = false;

    protected void onSearch(String query)
    {
        if(current == null)
        {
            Toast.makeText(requireContext(), getString(R.string.cant_search_here), Toast.LENGTH_SHORT).show();
            return;
        }
        else if(lastSearched)
        {
            //TODO : this temp fixes '2 intent receiving' ; find a better way
            lastSearched = false;
            return;
        }

        current.exploreSearch(query, this);
        lastSearched = true;
    }

    public void updateContent(RecyclerView.Adapter<?> adapter, String title, boolean shouldSaveBackInformation)
    {
        if(shouldSaveBackInformation)
            backStack.push(new BackInformation(getTitle(), binding.exploreSourcesListview.getAdapter()));

        binding.exploreSourcesListview.setAdapter(adapter);
        if(((MainActivity) requireActivity()).binding != null)
            ((MainActivity) requireActivity()).binding.appBarMain.toolbar.setTitle(title);
    }

    private void updateContent(BackInformation backInformation)
    {
        updateContent(backInformation.adapter, backInformation.title, false);
    }

    public void onBackPressed()
    {
        if(backStack.empty())
        {
            requireActivity().finish();
            return;
        }

        updateContent(backStack.pop());

        //if we went back to root, current is null
        if(backStack.empty()) current = null;
    }
}