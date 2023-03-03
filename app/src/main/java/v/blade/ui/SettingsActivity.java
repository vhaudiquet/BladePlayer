package v.blade.ui;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.Objects;

import v.blade.BladeApplication;
import v.blade.BuildConfig;
import v.blade.R;
import v.blade.databinding.SettingsFragmentAboutBinding;
import v.blade.databinding.SettingsFragmentSourcesBinding;
import v.blade.sources.Source;
import v.blade.sources.deezer.Deezer;
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
    public boolean onPreferenceStartFragment(@NonNull PreferenceFragmentCompat caller, Preference pref)
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);
        for(Fragment f : getSupportFragmentManager().getFragments())
        {
            //noinspection deprecation
            f.onActivityResult(requestCode, resultCode, data);
        }
    }

    public static class HeaderFragment extends PreferenceFragmentCompat
    {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey)
        {
            setPreferencesFromResource(R.xml.header_preferences, rootKey);

            //Set onPreferenceChange listener (to act)
            Preference pref = findPreference("dark_theme");
            assert pref != null;
            pref.setOnPreferenceChangeListener((preference, newValue) ->
            {
                String nv = (String) newValue;
                switch(nv)
                {
                    case "system_default":
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                        break;
                    case "dark_theme":
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                        break;
                    case "light_theme":
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                        break;
                }

                return true;
            });
        }
    }

    public static class SourcesFragment extends Fragment
    {
        private static class AddSourceAdapter implements ListAdapter
        {
            static class ViewHolder
            {
                private final TextView titleView;
                private final ImageView imageView;
                private final TextView subtitleView;

                ViewHolder(View itemView)
                {
                    titleView = itemView.findViewById(R.id.item_element_title);
                    imageView = itemView.findViewById(R.id.item_element_image);
                    subtitleView = itemView.findViewById(R.id.item_element_subtitle);
                    ImageView moreView = itemView.findViewById(R.id.item_element_more);

                    moreView.setVisibility(View.GONE);
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
                return 3;
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
                    case 2:
                        return Deezer.class;
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
                    catch(IllegalAccessException | NoSuchFieldException e)
                    {
                        e.printStackTrace();
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
        }

        private static class SourceAdapter extends RecyclerView.Adapter<SourceAdapter.ViewHolder>
        {
            static class ViewHolder extends RecyclerView.ViewHolder
            {
                private final TextView titleView;
                private final ImageView imageView;
                private final TextView subtitleView;
                private final ImageView moreView;

                public ViewHolder(@NonNull View itemView)
                {
                    super(itemView);

                    titleView = itemView.findViewById(R.id.item_element_title);
                    imageView = itemView.findViewById(R.id.item_element_image);
                    subtitleView = itemView.findViewById(R.id.item_element_subtitle);
                    moreView = itemView.findViewById(R.id.item_element_more);
                }
            }

            private final ItemTouchHelper touchHelper;
            private final View.OnClickListener clickListener;

            public SourceAdapter(ItemTouchHelper touchHelper, View.OnClickListener clickListener)
            {
                this.touchHelper = touchHelper;
                this.clickListener = clickListener;
            }

            @SuppressLint("ClickableViewAccessibility")
            @NonNull
            @Override
            public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
            {
                View view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_layout, parent, false);
                ViewHolder viewHolder = new ViewHolder(view);

                viewHolder.moreView.setImageResource(R.drawable.ic_reorder_24px);
                viewHolder.moreView.setOnTouchListener((view1, motionEvent) ->
                {
                    touchHelper.startDrag(viewHolder);
                    return true;
                });

                viewHolder.itemView.setOnClickListener(clickListener);

                return viewHolder;
            }

            @Override
            public void onBindViewHolder(@NonNull ViewHolder holder, int position)
            {
                Source current = Source.SOURCES.get(position);
                holder.titleView.setText(current.getName());
                holder.imageView.setImageResource(current.getImageResource());

                switch(current.getStatus())
                {
                    case STATUS_DOWN:
                        holder.subtitleView.setText(R.string.source_down_desc);
                        break;
                    case STATUS_NEED_INIT:
                        holder.subtitleView.setText(R.string.source_need_init_desc);
                        break;
                    case STATUS_CONNECTING:
                        holder.subtitleView.setText(R.string.source_connecting_desc);
                        break;
                    case STATUS_READY:
                        holder.subtitleView.setText(R.string.source_ready_desc);
                        break;
                }
            }

            @Override
            public int getItemCount()
            {
                return Source.SOURCES.size();
            }
        }

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
            updateSourcesView();

            //Set button 'add' action
            FloatingActionButton floatingActionButton = binding.settingsSourceAdd;
            floatingActionButton.setOnClickListener(this::showAddSourceDialog);

            return root;
        }

        public void updateSourcesView()
        {
            RecyclerView sourcesListView = binding.settingsSourcesListview;
            sourcesListView.setLayoutManager(new LinearLayoutManager(getActivity()));
            ItemTouchHelper touchHelper = new ItemTouchHelper(new TouchHelperCallback(Source.SOURCES)
            {
                @Override
                public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target)
                {
                    boolean tr = super.onMove(recyclerView, viewHolder, target);

                    //this is 'scheduleSave' : save changes after library sync
                    Toast.makeText(BladeApplication.appContext, R.string.please_sync_to_apply, Toast.LENGTH_LONG).show();

                    return tr;
                }
            });
            touchHelper.attachToRecyclerView(sourcesListView);

            SourceAdapter sourceAdapter = new SourceAdapter(touchHelper, view ->
            {
                int position = sourcesListView.getChildLayoutPosition(view);
                Source current = Source.SOURCES.get(position);
                Fragment sf = current.getSettingsFragment();
                if(sf != null)
                {
                    requireActivity().getSupportFragmentManager().beginTransaction()
                            .replace(R.id.settings, sf)
                            .addToBackStack(null)
                            .commit();
                }
            });
            sourcesListView.setAdapter(sourceAdapter);
        }

        //Here we can use NotifyDataSetChanged, as the list of sources will be small anyway
        @SuppressLint("NotifyDataSetChanged")
        private void showAddSourceDialog(View view)
        {
            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(view.getContext());
            builder.setTitle(R.string.add_source);
            final AddSourceAdapter adapter = new AddSourceAdapter();
            builder.setAdapter(adapter, (dialogInterface, i) ->
            {
                //Check if the source is already present if needed,
                //and add source to active sources list

                //noinspection rawtypes
                Class c = (Class) adapter.getItem(i);

                //Sources that can only be added once (only Local for now...)
                if(Local.class.equals(c))
                {
                    boolean alreadyHasOne = false;
                    for(Source s : Source.SOURCES)
                    {
                        if(s.getClass().equals(Local.class))
                        {
                            alreadyHasOne = true;
                            break;
                        }
                    }

                    if(alreadyHasOne) return;
                }

                try
                {
                    assert c != null;
                    Source toAdd = (Source) c.newInstance();

                    //Set source default status
                    toAdd.setStatus(Source.SourceStatus.STATUS_DOWN);

                    Source.SOURCES.add(toAdd);
                    toAdd.setIndex(Source.SOURCES.size() - 1);
                    Objects.requireNonNull(binding.settingsSourcesListview.getAdapter()).notifyDataSetChanged();
                }
                catch(IllegalAccessException | java.lang.InstantiationException ignored)
                {
                }
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

            try
            {
                PackageInfo packageInfo = requireContext().getPackageManager().getPackageInfo(requireContext().getPackageName(), 0);
                String versionText = getText(R.string.version) + " " + packageInfo.versionName + " (" + packageInfo.versionCode + ")";
                binding.aboutVersionText.setText(versionText);
                String buildTypeText = getString(R.string.build_type) + " : " + BuildConfig.BUILD_TYPE;
                binding.aboutBuildtypeText.setText(buildTypeText);
            }
            catch(Exception ignored)
            {
            }

            return binding.getRoot();
        }
    }
}