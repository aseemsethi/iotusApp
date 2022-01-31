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
                        (TableLayout.LayoutParams.FILL_PARENT,
                                TableLayout.LayoutParams.WRAP_CONTENT);
        int leftMargin=5;
        int topMargin=2;
        int rightMargin=5;
        int bottomMargin=2;
        tableRowParams.setMargins(leftMargin, topMargin, rightMargin, bottomMargin);
        tbrow.setLayoutParams(tableRowParams);

        TableRow.LayoutParams cellParams = new TableRow.LayoutParams(0,
                TableRow.LayoutParams.WRAP_CONTENT);

        TextView t1v = new TextView(getContext());
        t1v.setText(rowNum.toString());
        t1v.setTextColor(Color.WHITE);
        t1v.setGravity(Gravity.CENTER);
        //cellParams.weight = 1;
        //t1v.setLayoutParams(cellParams);
        tbrow.addView(t1v);

        TextView t2v = new TextView(getActivity());
        t2v.setText(gwid);
        t2v.setTextColor(Color.WHITE);
        t2v.setGravity(Gravity.CENTER);
        cellParams.rightMargin=5;
        //cellParams.weight = 2;
        t2v.setLayoutParams(cellParams);
        tbrow.addView(t2v);

        TextView t3v = new TextView(getActivity());
        t3v.setText(type);
        t3v.setTextColor(Color.WHITE);
        t3v.setGravity(Gravity.CENTER);
        //cellParams.weight = 2;
        //t3v.setLayoutParams(cellParams);
        tbrow.addView(t3v);

        TextView t4v = new TextView(getActivity());
        t4v.setText(ip);
        t4v.setTextColor(Color.WHITE);
        t4v.setGravity(Gravity.CENTER);
        //cellParams.weight = 5;
        //t4v.setLayoutParams(cellParams);
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

        //TableRow.LayoutParams cellParams = new TableRow.LayoutParams(0,
        //        TableRow.LayoutParams.MATCH_PARENT);


        TextView tv0 = new TextView(getActivity());
        tv0.setText(" Sl.No ");
        tv0.setTextColor(Color.WHITE);
        tv0.setGravity(Gravity.CENTER);
        //cellParams.weight = 1;
        //tv0.setLayoutParams(cellParams);
        tbrow0.addView(tv0);

        TextView tv1 = new TextView(getActivity());
        tv1.setText(" GWID ");
        tv1.setTextColor(Color.WHITE);
        tv1.setGravity(Gravity.CENTER);
        //cellParams.weight = 3;
        //tv1.setLayoutParams(cellParams);
        tbrow0.addView(tv1);

        TextView tv2 = new TextView(getActivity());
        tv2.setText(" Type ");
        tv2.setTextColor(Color.WHITE);
        tv2.setGravity(Gravity.CENTER);
        //cellParams.weight = 3;
        //tv2.setLayoutParams(cellParams);
        tbrow0.addView(tv2);

        TextView tv3 = new TextView(getActivity());
        tv3.setText(" IP ");
        tv3.setTextColor(Color.WHITE);
        tv3.setGravity(Gravity.CENTER);
        //cellParams.weight = 3;
        //tv3.setLayoutParams(cellParams);
        tbrow0.addView(tv3);

        stk.addView(tbrow0, tableRowParams);
    }
}