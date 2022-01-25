package v.blade.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import v.blade.R;
import v.blade.databinding.FragmentExploreBinding;
import v.blade.sources.Source;

public class ExploreFragment extends Fragment
{
    private FragmentExploreBinding binding;

    private static class SourceAdapter extends RecyclerView.Adapter<SourceAdapter.ViewHolder>
    {
        static class ViewHolder extends RecyclerView.ViewHolder
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

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        binding = FragmentExploreBinding.inflate(inflater, container, false);

        binding.exploreSourcesListview.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.exploreSourcesListview.setAdapter(new SourceAdapter());
        return binding.getRoot();
    }

    public void onBackPressed()
    {
        requireActivity().finish();
    }
}