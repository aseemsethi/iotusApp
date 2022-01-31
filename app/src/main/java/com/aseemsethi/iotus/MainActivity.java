package com.aseemsethi.iotus;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.Menu;

import com.aseemsethi.iotus.ui.settings.SettingsViewModel;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.navigation.NavigationView;

import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.AppCompatActivity;

import com.aseemsethi.iotus.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration mAppBarConfiguration;
    private ActivityMainBinding binding;
    final String TAG = "iotus main";
    BroadcastReceiver myReceiverMqtt = null;
    BroadcastReceiver myReceiverMqttStatus = null;
    String topic;
    private SettingsViewModel settingsViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.appBarMain.toolbar);

        DrawerLayout drawer = binding.drawerLayout;
        NavigationView navigationView = binding.navView;
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_home, R.id.nav_gateway, R.id.nav_temp, R.id.nav_settings)
                .setDrawerLayout(drawer)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);

        settingsViewModel = new ViewModelProvider(this).get(SettingsViewModel.class);

        SharedPreferences preferences = PreferenceManager.
                getDefaultSharedPreferences(getApplicationContext());
        String nm = preferences.getString("cid", "10000");
        Log.d(TAG, "CID from Shared: " + nm);
        settingsViewModel.cid = nm;
        Log.d(TAG, "Saved CID into Settings: " + settingsViewModel.cid);
        topic = "gurupada/" + settingsViewModel.cid + "/#";

        Log.d(TAG, "Starting MQTT");
        Intent serviceIntent = new Intent(getApplicationContext(),
                myMqttService.class);
        serviceIntent.setAction(myMqttService.MQTTSUBSCRIBE_ACTION);
        serviceIntent.putExtra("topic", topic);
        startService(serviceIntent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "OnStart - Register BroadcastReceiver");
        registerServices();
    }

    void registerServices() {
        Log.d(TAG, "registerServices called filter1");
        IntentFilter filter1 = new IntentFilter("RestartMqtt");
        myReceiverMqtt = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (isMyServiceRunning(myMqttService.class)) {
                    Log.d(TAG, "registerServices: svc is already running");
                    return;
                }
                Log.d(TAG, "registerServices: restart mqttService");
                Intent serviceIntent = new Intent(context, myMqttService.class);
                serviceIntent.setAction(myMqttService.MQTTSUBSCRIBE_ACTION);
                serviceIntent.putExtra("topic", topic);
                startService(serviceIntent);
            }
        };
        registerReceiver(myReceiverMqtt, filter1);
    }

    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
}