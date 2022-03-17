package com.aseemsethi.iotus;

import android.content.Context;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

// here is the OnRevieve methode which will be called when boot completed
public class BootCaptureIntentReceiver extends BroadcastReceiver{
    final String TAG = "iotus BootRcv";

    @Override
    public void onReceive(Context context, Intent intent) {
        //we double check here for only boot complete event
        if(intent.getAction().equalsIgnoreCase(Intent.ACTION_BOOT_COMPLETED))
        {
            String topic, num;
            //here we start the service  again.
            Log.d(TAG, "Starting MQTT after boot");
            SharedPreferences preferences = PreferenceManager.
                    getDefaultSharedPreferences(context);
            num = preferences.getString("cid", "10000");
            topic = "gurupada/" + num + "/#";
            Log.d(TAG, "BootRcv: CID from Shared: " + topic);

            Intent serviceIntent = new Intent(context, myMqttService.class);
            serviceIntent.setAction(myMqttService.MQTTSUBSCRIBE_ACTION);
            serviceIntent.putExtra("topic", topic);
            context.startService(serviceIntent);
        }
    }
}