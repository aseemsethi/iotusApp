package com.aseemsethi.iotus.ui.gateway;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.aseemsethi.iotus.databinding.FragmentGatewayBinding;
import com.aseemsethi.iotus.myMqttService;
import com.aseemsethi.iotus.ui.settings.SettingsViewModel;

import androidx.lifecycle.ViewModelProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;

public class GatewayFragment extends Fragment {

    private GatewayViewModel gatewayViewModel;
    private FragmentGatewayBinding binding;
    final String TAG = "iotus gateway ";
    private SettingsViewModel settingsViewModel;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        gatewayViewModel =
                new ViewModelProvider(this).get(GatewayViewModel.class);

        binding = FragmentGatewayBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        final TextView textView = binding.textGallery;
        gatewayViewModel.getText().observe(getViewLifecycleOwner(), new Observer<String>() {
            @Override
            public void onChanged(@Nullable String s) {
                textView.setText(s);
            }
        });
        settingsViewModel = new ViewModelProvider(requireActivity()).
                get(SettingsViewModel.class);
        Log.d(TAG, "CID from Settings: " + settingsViewModel.cid);

        settingsViewModel.getText().observe(getViewLifecycleOwner(), new Observer<String>() {
            @Override
            public void onChanged(@Nullable String s) {
                Log.d(TAG, "CID from Settings - observed: " + s);
            }
        });
        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}