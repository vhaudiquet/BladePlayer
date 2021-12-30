package v.blade.ui;

import android.database.DataSetObserver;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import v.blade.R;
import v.blade.databinding.SettingsFragmentAboutBinding;
import v.blade.databinding.SettingsFragmentSourcesBinding;
import v.blade.sources.local.Local;
import v.blade.sources.spotify.Spotify;

public class SettingsActivity extends AppCompatActivity implements
        PreferenceFragmentCompat.OnPreferenceStartFragmentCallback
{

    private static final String TITLE_TAG = "settingsActivityTitle";

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);
        if(savedInstanceState == null)
        {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.settings, new HeaderFragment())
                    .commit();
        }
        else
        {
            setTitle(savedInstanceState.getCharSequence(TITLE_TAG));
        }
        getSupportFragmentManager().addOnBackStackChangedListener(
                () ->
                {
                    if(getSupportFragmentManager().getBackStackEntryCount() == 0)
                    {
                        setTitle(R.string.action_settings);
                    }
                });
        ActionBar actionBar = getSupportActionBar();
        if(actionBar != null)
        {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState)
    {
        super.onSaveInstanceState(outState);
        // Save current activity title so we can set it again after a configuration change
        outState.putCharSequence(TITLE_TAG, getTitle());
    }

    @Override
    public boolean onSupportNavigateUp()
    {
        if(getSupportFragmentManager().popBackStackImmediate())
        {
            return true;
        }
        return super.onSupportNavigateUp();
    }

    @Override
    public boolean onPreferenceStartFragment(PreferenceFragmentCompat caller, Preference pref)
    {
        // Instantiate the new Fragment
        final Bundle args = pref.getExtras();
        final Fragment fragment = getSupportFragmentManager().getFragmentFactory().instantiate(
                getClassLoader(),
                pref.getFragment());
        fragment.setArguments(args);
        getSupportFragmentManager().setFragmentResultListener("requestKey", fragment, (requestKey, result) ->
        {
            //Do nothing anyway, we dont have anything that returns result for now
            //Else we would : setFragmentResult("requestKey", bundle)
        });

        // Replace the existing Fragment with the new Fragment
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.settings, fragment)
                .addToBackStack(null)
                .commit();
        setTitle(pref.getTitle());
        return true;
    }

    public static class HeaderFragment extends PreferenceFragmentCompat
    {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey)
        {
            setPreferencesFromResource(R.xml.header_preferences, rootKey);
        }
    }

    public static class SourcesFragment extends Fragment
    {
        private SettingsFragmentSourcesBinding binding;

        public SourcesFragment()
        {
            super(R.layout.settings_fragment_sources);
        }

        public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
        {
            binding = SettingsFragmentSourcesBinding.inflate(inflater, container, false);
            View root = binding.getRoot();

            //Set list to current sources
            RecyclerView sourcesListView = binding.settingsSourcesListview;
            //TODO

            //Set button 'add' action
            FloatingActionButton floatingActionButton = binding.settingsSourceAdd;
            floatingActionButton.setOnClickListener(SourcesFragment::showAddSourceDialog);

            return root;
        }

        private static void showAddSourceDialog(View view)
        {
            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(view.getContext());
            builder.setTitle(R.string.add_source);
            builder.setAdapter(new ListAdapter()
            {
                class ViewHolder
                {
                    private final TextView titleView;
                    private final ImageView imageView;
                    private final TextView subtitleView;
                    private final ImageView moreView;

                    ViewHolder(View itemView)
                    {
                        titleView = itemView.findViewById(R.id.item_element_title);
                        imageView = itemView.findViewById(R.id.item_element_image);
                        subtitleView = itemView.findViewById(R.id.item_element_subtitle);
                        moreView = itemView.findViewById(R.id.item_element_more);
                    }
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
                    return 2;
                }

                @Override
                public Object getItem(int i)
                {
                    switch(i)
                    {
                        case 0:
                            return Local.class;
                        case 1:
                            return Spotify.class;
                    }

                    return null;
                }

                @Override
                public long getItemId(int i)
                {
                    return i;
                }

                @Override
                public boolean hasStableIds()
                {
                    return true;
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

                    //noinspection rawtypes
                    Class current = (Class) getItem(i);
                    if(current != null)
                    {
                        try
                        {
                            viewHolder.titleView.setText(current.getField("NAME_RESOURCE").getInt(null));
                            viewHolder.subtitleView.setText(current.getField("DESCRIPTION_RESOURCE").getInt(null));
                            viewHolder.imageView.setImageResource(current.getField("IMAGE_RESOURCE").getInt(null));
                        }
                        catch(IllegalAccessException | NoSuchFieldException ignored)
                        {
                        }
                    }

                    return convertView;
                }

                @Override
                public int getItemViewType(int i)
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
            }, (dialogInterface, i) ->
            {
                //Add source to active sources list
            });

            AlertDialog dialog = builder.create();
            dialog.show();
        }
    }

    public static class AboutFragment extends Fragment
    {
        private SettingsFragmentAboutBinding binding;

        public AboutFragment()
        {
            super(R.layout.settings_fragment_about);
        }

        public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
        {
            binding = SettingsFragmentAboutBinding.inflate(inflater, container, false);
            return binding.getRoot();
        }
    }
}