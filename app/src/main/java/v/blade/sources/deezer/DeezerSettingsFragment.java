package v.blade.sources.deezer;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import v.blade.BladeApplication;
import v.blade.R;
import v.blade.databinding.SettingsFragmentDeezerBinding;
import v.blade.sources.Source;

public class DeezerSettingsFragment extends Fragment
{
    private final Deezer deezer;
    private SettingsFragmentDeezerBinding binding;

    public DeezerSettingsFragment(Deezer deezer)
    {
        this.deezer = deezer;
    }

    public void refreshStatus()
    {
        Source.SourceStatus status = deezer.getStatus();

        // Set status label
        switch(status)
        {
            case STATUS_DOWN:
                binding.settingsDeezerStatus.setText(R.string.source_down_desc);
                break;
            case STATUS_NEED_INIT:
                binding.settingsDeezerStatus.setText(R.string.source_need_init_desc);
                break;
            case STATUS_CONNECTING:
                binding.settingsDeezerStatus.setText(R.string.source_connecting_desc);
                break;
            case STATUS_READY:
                binding.settingsDeezerStatus.setText(R.string.source_ready_desc);
                break;
        }

        // Set account text
        if(status == Source.SourceStatus.STATUS_DOWN)
        {
            binding.settingsDeezerAccount.setText(R.string.disconnected);
            binding.settingsDeezerAccount.setTextColor(getResources().getColor(R.color.errorRed));

            // Hide audio quality and force init buttons
            binding.settingsDeezerInit.setVisibility(View.GONE);
        }
        else
        {
            binding.settingsDeezerAccount.setText(deezer.account_name);
            binding.settingsDeezerAccount.setTextColor(getResources().getColor(R.color.okGreen));

            // Hide login buttons (we are already connected)
            binding.settingsDeezerUser.setVisibility(View.GONE);
            binding.settingsDeezerPassword.setVisibility(View.GONE);
            binding.settingsDeezerSignIn.setVisibility(View.GONE);
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        binding = SettingsFragmentDeezerBinding.inflate(inflater, container, false);

        refreshStatus();

        // Set 'sign in' button action
        binding.settingsDeezerSignIn.setOnClickListener(v ->
        {
            // Set deezer parameters
            deezer.account_login = binding.settingsDeezerUser.getText().toString();
            deezer.account_password = binding.settingsDeezerPassword.getText().toString();

            deezer.setStatus(Source.SourceStatus.STATUS_NEED_INIT);
            deezer.initSource();
        });

        // Set 'force init' button action
        binding.settingsDeezerInit.setOnClickListener(v ->
        {
            //tODO CHANGE
            BladeApplication.obtainExecutorService().execute(deezer::synchronizeLibrary);
            //deezer.setStatus(Source.SourceStatus.STATUS_NEED_INIT);
            //deezer.initSource();
        });

        return binding.getRoot();
    }
}
