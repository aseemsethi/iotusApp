package com.aseemsethi.iotus.ui.gateway;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
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
    private boolean MgrBroacastRegistred = false;
    BroadcastReceiver myRecv = null;


    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        gatewayViewModel =
                new ViewModelProvider(this).get(GatewayViewModel.class);

        binding = FragmentGatewayBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        settingsViewModel = new ViewModelProvider(requireActivity()).
                get(SettingsViewModel.class);
        Log.d(TAG, "CID from Settings: " + settingsViewModel.cid);

        settingsViewModel.getText().observe(getViewLifecycleOwner(), new Observer<String>() {
            @Override
            public void onChanged(@Nullable String s) {
                Log.d(TAG, "CID from Settings - observed: " + s);
            }
        });
        binding.logs.setMovementMethod(new ScrollingMovementMethod());

        return root;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "OnCreate");
        super.onCreate(savedInstanceState);
        registerServices();
    }

    @Override
    public void onResume() {
        super.onResume();
        registerServices();
    }

    @Override
    public void onStart() {
        Log.d(TAG, "OnStart..");
        super.onStart();
        registerServices();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.d(TAG, "onDestroy: Unregister recv");
        try {
            getContext().unregisterReceiver(myRecv);
            MgrBroacastRegistred = false;
        } catch (Exception e){
            Log.d(TAG, "onPause: Already Unregistered recv");
        }
        binding = null;
    }

    public void onPause() {
        super.onPause();
        Log.d(TAG, "onPause: Unregister recv");
        try {
            getContext().unregisterReceiver(myRecv);
            MgrBroacastRegistred = false;
        } catch (Exception e){
            Log.d(TAG, "onPause: Already Unregistered recv");
        }
    }
    void registerServices() {
        Log.d(TAG, "registerServices called filter2");
        if (MgrBroacastRegistred == false) {
            MgrBroacastRegistred = true;
        } else {
            Log.d(TAG, "Recevier already registered");
            return;
        }
        IntentFilter filter2 = new IntentFilter("com.aseemsethi.iotus.msg");
        myRecv = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.d(TAG, "registerServices: msg:" +
                        intent.getStringExtra("name"));
                binding.logs.append(intent.getStringExtra("msg"));
            }
        };
        getContext().registerReceiver(myRecv, filter2);
    }
}