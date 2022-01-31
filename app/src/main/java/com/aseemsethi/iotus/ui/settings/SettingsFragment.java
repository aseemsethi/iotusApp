package com.aseemsethi.iotus.ui.settings;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.aseemsethi.iotus.databinding.FragmentSettingsBinding;
import com.aseemsethi.iotus.myMqttService;
import com.aseemsethi.iotus.ui.home.HomeViewModel;

public class SettingsFragment extends Fragment {
    final String TAG = "iotus settings";
    private SettingsViewModel settingsViewModel;
    private HomeViewModel homeViewModel;
    private FragmentSettingsBinding binding;
    private AlphaAnimation buttonClick = new AlphaAnimation(1F, 0.1F);

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        //settingsViewModel = new ViewModelProvider(this).get(SettingsViewModel.class);
        settingsViewModel = new ViewModelProvider(requireActivity()).
                get(SettingsViewModel.class);
        homeViewModel = new ViewModelProvider(this).get(HomeViewModel.class);
        if (homeViewModel.getLoggedin() == false) {
            Log.d(TAG, "Not logged in..");
            if (isAdded())
                Toast.makeText(getContext(), "Please login first...",
                        Toast.LENGTH_SHORT).show();
            return null;
        } else {
            Log.d(TAG, "logged in..");
        }

        binding = FragmentSettingsBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        /*SharedPreferences sharedPref = getActivity().
                getPreferences(Context.MODE_PRIVATE);
        String nm = sharedPref.getString("cid", "10000"); */

        SharedPreferences sharedPref = PreferenceManager.
                getDefaultSharedPreferences(getContext());
        String nm = sharedPref.getString("cid", "10000");
        Log.d(TAG, "CID from Shared Pref: " + nm);

        binding.customerId.setText(nm);
        settingsViewModel.cid = nm;
        settingsViewModel.setText(nm);
        Log.d(TAG, "Retrieved cid from settings: " + settingsViewModel.cid);

        final Button btn = binding.savePref;
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                v.startAnimation(buttonClick);
                String nm = binding.customerId.getText().toString();
                Log.d(TAG, "Save CustomerID: " + nm);
                settingsViewModel.cid = nm;

                SharedPreferences sharedPref = PreferenceManager.
                        getDefaultSharedPreferences(getContext());
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putString("cid", nm);
                editor.apply();

                String topic = "gurupada/" + nm +"/#";
                Intent serviceIntent = new Intent(getContext(),
                        myMqttService.class);
                serviceIntent.setAction("MQTTSUBSCRIBE_TOPIC");
                serviceIntent.putExtra("topic", topic);
                getContext().startService(serviceIntent);
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
