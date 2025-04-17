package gay.nihil.lena.drm_panic_viewer;

import static gay.nihil.lena.drm_panic_viewer.databinding.FragmentSecondBinding.*;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import java.util.Objects;

import gay.nihil.lena.drm_panic_viewer.databinding.FragmentSecondBinding;

public class SecondFragment extends Fragment {

    private FragmentSecondBinding binding;

    PanicMessage panicMessage;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {

        binding = FragmentSecondBinding.inflate(inflater, container, false);

        return binding.getRoot();
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Bundle bundle = this.getArguments();
        assert bundle != null;
        Uri uri = bundle.getParcelable("uri", Uri.class);

        assert uri != null;

        try {
            panicMessage = new PanicMessage(uri);
        } catch (Exception e) {
            Toast.makeText(getContext(), R.string.not_a_crash_qr, Toast.LENGTH_LONG).show();
            NavHostFragment.findNavController(this).navigate(R.id.action_SecondFragment_to_FirstFragment);
            return;
        }

        binding.logView.setText(panicMessage.getLog());
        if (panicMessage.reason != null) {
            setActivityTitle(panicMessage.reason);
        }

        binding.logTypeLabel.setText(getString(R.string.log_type_label, getResources().getStringArray(R.array.log_types)[panicMessage.getLogType()]));

        if (panicMessage.getHardwareName() != null) {
            binding.hardwareNameLabel.setText(getString(R.string.hardware_name_label, panicMessage.getHardwareName()));
        } else {
            binding.hardwareNameLabel.setVisibility(View.GONE);
        }

        if (panicMessage.getDistribution() != null) {
            binding.distributionLabel.setText(getString(R.string.distribution_label, panicMessage.getDistribution()));
        } else {
            binding.distributionLabel.setVisibility(View.GONE);
        }

        if (panicMessage.getVersion() != null) {
            binding.versionLabel.setText(getString(R.string.version_label, panicMessage.getVersion()));
        } else {
            binding.versionLabel.setVisibility(View.GONE);
        }

        if (panicMessage.getArchitecture() != null) {
            binding.architectureLabel.setText(getString(R.string.architecture_label, panicMessage.getArchitecture()));
        } else {
            binding.architectureLabel.setVisibility(View.GONE);
        }

        // weird hack to remove cut/paste and other bad options
        if (binding.logView.getCustomSelectionActionModeCallback() == null) {
            binding.logView.setCustomSelectionActionModeCallback(new ActionMode.Callback() {
                @Override
                public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                    return true;
                }

                @Override
                public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                    menu.removeItem(android.R.id.paste);
                    menu.removeItem(android.R.id.cut);
                    menu.removeItem(android.R.id.pasteAsPlainText);
                    menu.removeItem(android.R.id.autofill);
                    menu.removeItem(android.R.id.edit);
                    menu.removeItem(android.R.id.empty);
                    menu.removeItem(android.R.id.replaceText);
                    menu.removeItem(android.R.id.extractArea);

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                        menu.removeItem(android.R.id.underline);
                        menu.removeItem(android.R.id.bold);
                        menu.removeItem(android.R.id.italic);
                    }

                    return true;
                }

                @Override
                public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                    return false;
                }

                @Override
                public void onDestroyActionMode(ActionMode mode) {
                }
            });
        } else {
            binding.logView.setCustomSelectionActionModeCallback(null);
        }

        binding.buttonSecond.setOnClickListener(v -> {
            if (panicMessage.reportUri != null) {
                startActivity(new Intent(Intent.ACTION_VIEW, panicMessage.reportUri));
            } else {
                Toast.makeText(getContext(), R.string.no_report_uri, Toast.LENGTH_LONG).show();
            }
        });
    }

    void setActivityTitle(String title) {
        ((AppCompatActivity) requireActivity()).getSupportActionBar().setTitle(title);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

}