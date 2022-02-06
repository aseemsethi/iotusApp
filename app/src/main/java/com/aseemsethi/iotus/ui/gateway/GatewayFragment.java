package com.aseemsethi.iotus.ui.gateway;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.aseemsethi.iotus.R;
import com.aseemsethi.iotus.databinding.FragmentGatewayBinding;
import com.aseemsethi.iotus.myMqttService;
import com.aseemsethi.iotus.ui.settings.SettingsViewModel;

import androidx.lifecycle.ViewModelProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class GatewayFragment extends Fragment {

    private GatewayViewModel gatewayViewModel;
    private FragmentGatewayBinding binding;
    final String TAG = "iotus gateway ";
    private SettingsViewModel settingsViewModel;
    private boolean MgrBroacastRegistred = false;
    BroadcastReceiver myRecv = null;
    TableLayout stk;
    Integer rowNum;

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
        String cl = readLogsFromFile(getContext(), "gw.txt");

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
        IntentFilter filter2 = new IntentFilter("com.aseemsethi.iotus.gw");
        myRecv = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String currentTime = new SimpleDateFormat("HH-mm",
                        Locale.getDefault()).format(new Date());
                Log.d(TAG, "registerServices: msg:" +
                        intent.getStringExtra("name"));
                //binding.logs.append(intent.getStringExtra("msg"));
                binding.logs.setText("Last Update: " + currentTime);
                String cl = readLogsFromFile(getContext(), "gw.txt");
            }
        };
        getContext().registerReceiver(myRecv, filter2);
    }

    private String readLogsFromFile(Context context, String filename) {
        String ret = "";
        int number = 0;
        File file = context.getFileStreamPath(filename);
        binding.tableD.removeAllViews();
        rowNum = 1;
        stk = binding.tableD;
        setupTable();
        if(file == null || !file.exists()) {
            Log.d(TAG, "File not found: " + filename);
            return "\nGW logs not created..";
        }
        try {
            InputStream inputStream = context.openFileInput(filename);
            if ( inputStream != null ) {
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                String receiveString = "";
                while ( (receiveString = bufferedReader.readLine()) != null ) {
                    Log.d(TAG, "Read: " + receiveString);
                    if (receiveString.length() < 10) {
                        Log.d(TAG, "Read String is too short");
                        continue;
                    }
                    try {
                        JSONObject jObject = new JSONObject(receiveString);
                        addToTable(jObject.getString("gwid"),jObject.getString("type"),
                                jObject.getString("ip"));
                        rowNum++;
                        number++;
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                Log.d(TAG, "Total GWs Read: " + number);
                inputStream.close();
            }
        }
        catch (FileNotFoundException e) {
            Log.e(TAG, "File not found: " + e.toString());
        } catch (IOException e) {
            Log.e(TAG, "Can not read file: " + e.toString());
        }
        return ret;
    }

    public void addToTable(String gwid, String type, String ip) {
        TableRow tbrow = new TableRow(getContext());
        TableLayout.LayoutParams tableRowParams= new TableLayout.LayoutParams
                        (TableLayout.LayoutParams.WRAP_CONTENT,
                                TableLayout.LayoutParams.WRAP_CONTENT);
        int leftMargin=5;
        int topMargin=2;
        int rightMargin=5;
        int bottomMargin=2;
        tableRowParams.setMargins(leftMargin, topMargin, rightMargin, bottomMargin);
        tbrow.setLayoutParams(tableRowParams);

        TextView t1v = new TextView(getContext());
        t1v.setText(rowNum.toString());
        t1v.setTextColor(Color.BLUE);
        t1v.setBackgroundColor(Color.parseColor("#f0f0f0"));
        t1v.setGravity(Gravity.LEFT);
        t1v.setPadding(5, 15, 0, 15);
        t1v.setLayoutParams(new
                TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT,
                TableRow.LayoutParams.WRAP_CONTENT, 1));
        tbrow.addView(t1v);

        TextView t2v = new TextView(getActivity());
        t2v.setText(gwid);
        t2v.setTextColor(Color.WHITE);
        t2v.setGravity(Gravity.LEFT);
        t2v.setPadding(5, 15, 15, 15);
        t2v.setLayoutParams(new
                TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT,
                TableRow.LayoutParams.WRAP_CONTENT, 3));
        tbrow.addView(t2v);

        TextView t3v = new TextView(getActivity());
        t3v.setText(type);
        t3v.setTextColor(Color.WHITE);
        t3v.setGravity(Gravity.RIGHT);
        t3v.setPadding(5, 15, 15, 15);
        t3v.setLayoutParams(new
                TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT,
                TableRow.LayoutParams.WRAP_CONTENT, 3));
        tbrow.addView(t3v);

        TextView t4v = new TextView(getActivity());
        t4v.setText(ip);
        t4v.setTextColor(Color.WHITE);
        t4v.setGravity(Gravity.CENTER);
        t4v.setPadding(5, 15, 15, 15);
        t4v.setLayoutParams(new
                TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT,
                TableRow.LayoutParams.WRAP_CONTENT, 3));
        tbrow.addView(t4v);

        stk.addView(tbrow, tableRowParams);
    }

    public void setupTable() {
        TableRow tbrow0;
        tbrow0 = new TableRow(getContext());

        TableLayout.LayoutParams tableRowParams= new TableLayout.LayoutParams
                (TableLayout.LayoutParams.FILL_PARENT,
                        TableLayout.LayoutParams.WRAP_CONTENT);
        int leftMargin=5;
        int topMargin=2;
        int rightMargin=5;
        int bottomMargin=2;
        tableRowParams.setMargins(leftMargin, topMargin, rightMargin, bottomMargin);
        tbrow0.setLayoutParams(tableRowParams);

        TextView tv0 = new TextView(getActivity());
        tv0.setText(" No ");
        tv0.setTextColor(Color.BLUE);
        tv0.setBackgroundColor(Color.parseColor("#f0f0f0"));
        tv0.setGravity(Gravity.CENTER);
        tbrow0.addView(tv0);

        TextView tv1 = new TextView(getActivity());
        tv1.setText(" GWID ");
        tv1.setTextColor(Color.BLUE);
        tv1.setBackgroundColor(Color.parseColor("#f0f0f0"));
        tv1.setGravity(Gravity.CENTER);
        tbrow0.addView(tv1);

        TextView tv2 = new TextView(getActivity());
        tv2.setText(" Type ");
        tv2.setTextColor(Color.BLUE);
        tv2.setBackgroundColor(Color.parseColor("#f0f0f0"));
        tv2.setGravity(Gravity.CENTER);
        tbrow0.addView(tv2);

        TextView tv3 = new TextView(getActivity());
        tv3.setText(" IP ");
        tv3.setTextColor(Color.BLUE);
        tv3.setBackgroundColor(Color.parseColor("#f0f0f0"));
        tv3.setGravity(Gravity.CENTER);
        tbrow0.addView(tv3);

        stk.addView(tbrow0, tableRowParams);
    }
}