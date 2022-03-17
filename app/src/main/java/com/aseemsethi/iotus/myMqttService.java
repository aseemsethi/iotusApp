package com.aseemsethi.iotus;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

import androidx.annotation.RequiresApi;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;

import static android.app.PendingIntent.FLAG_IMMUTABLE;
import static android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE;
import static android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC;

/*
In manifest file, ensure that the service name starts with lower case -
If the name assigned to this attribute begins with a colon (':'), a new process,
private to the application, is created when it's needed and the service runs in
that process. If the process name begins with a lowercase character, the service
will run in a global process of that name, provided that it has permission to
do so. This allows components in different applications to share a process,
reducing resource usage.
*/
public class myMqttService extends Service {
    final String TAG = "iotus: MQTT";
    String CHANNEL_ID = "default";
    //String CHANNEL_URG = "urgent";
    NotificationManager mNotificationManager;
    Notification notification;
    int incr = 100;
    int counter = 1;
    MqttHelper mqttHelper;
    final static String MQTTSUBSCRIBE_ACTION = "MQTTSUBSCRIBE_ACTION";
    final static String MQTTSUBSCRIBE_TOPIC = "MQTTSUBSCRIBE_TOPIC";
    final static String MQTT_SEND_LOC = "MQTT_SEND_LOC";
    public final static String MQTT_SEND_NAME = "MQTT_SEND_NAME";
    boolean running = false;
    String name = null;
    String lineSeparator;
    String topic;
    String filename = "mylocation.txt";
    int colorIndex = 30;
    Map<String, Integer> colorMap = new HashMap<String, Integer>();
    Uri ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

    public myMqttService() {
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

    @RequiresApi(api = Build.VERSION_CODES.Q)
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = null;
        Log.d(TAG, "onStartCommand mqttService");

        lineSeparator = System.getProperty("line.separator");
        if (intent == null) {
            Log.d(TAG, "Intent is null..possible due to app restart");
        } else {
            action = intent.getAction();
        }
        Log.d(TAG, "ACTION: " + action);
        if (action == "MQTTSUBSCRIBE_ACTION") {
            Bundle extras = intent.getExtras();
            if(extras == null) {
                Log.d(TAG,"null MQTTSUBSCRIBE_ACTION");
                SharedPreferences preferences = PreferenceManager.
                        getDefaultSharedPreferences(getApplicationContext());
                String nm = preferences.getString("cid", "10000");
                Log.d(TAG, "OnStart: CID from Shared: " + nm);
                topic = "gurupada/" + nm + "/#";
            } else {
                topic = extras.getString("topic");
                Log.d(TAG, "MQTT_SUBSCRIBE - topic:" + topic);
            }
        }
        if (action == "MQTTSUBSCRIBE_TOPIC") {
            Bundle extras = intent.getExtras();
            if(extras == null) {
                Log.d(TAG,"null MQTTSUBSCRIBE_TOPIC");
                SharedPreferences preferences = PreferenceManager.
                        getDefaultSharedPreferences(getApplicationContext());
                String nm = preferences.getString("cid", "10000");
                Log.d(TAG, "MQTTSUBSCRIBE_TOPIC: CID from Shared: " + nm);
                topic = "gurupada/" + nm + "/#";
            } else {
                if (mqttHelper != null) {
                    mqttHelper.unsubscribeToTopic(topic);
                }
                topic = extras.getString("topic");
                if (mqttHelper != null) {
                    mqttHelper.subscribeToTopic(topic);
                    Log.d(TAG, "MQTTSUBSCRIBE_TOPIC topic:" + topic);
                }
            }
        }
        if (action == "MQTT_SEND_LOC") {
            Log.d(TAG, "Recvd MQTT Send LOC message.....................");
            Bundle extras = intent.getExtras();
            if(extras == null) {
                Log.d(TAG,"null MQTT_SEND_LOC");
            } else {
                Float lat = (Float) extras.getFloat("lat");
                Float lon = (Float) extras.getFloat("lon");
                publish(topic, name + ":" + lat + ":" + lon);
            }
            return START_STICKY;
        }

        AudioAttributes attributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .build();
        mNotificationManager = (NotificationManager) this.getSystemService(
                Context.NOTIFICATION_SERVICE);
        NotificationChannel mChannel = new NotificationChannel(CHANNEL_ID,
                "my_channel",
                NotificationManager.IMPORTANCE_HIGH);
        mChannel.enableLights(true);
        mChannel.setLightColor(Color.GREEN);
        mChannel.setSound(ringtoneUri, attributes); // This is IMPORTANT

        //mChannel.setSound(null, null);
        //mChannel.setVibrationPattern(new long[] { 0, 400, 200, 400});
        mNotificationManager.createNotificationChannel(mChannel);

        if (mqttHelper != null) {
            if ((running == true) && mqttHelper.isConnected()) {
                Log.d(TAG, "MQTT Service is already connected");
                return START_STICKY;
            }
        } else {
            Log.d(TAG, "mqtthelper is null");
        }
        Log.d(TAG, "restarting MQTT Service");
        try {
            startMqtt(topic);
            running = true;
        } catch (MqttException e) {
            e.printStackTrace();
        }

        // The following "startForeground" with a notification is what makes
        // the service run in the background and not get killed, when the app gets
        // killed by the user.
        String currentTime = new SimpleDateFormat("HH-mm",
                Locale.getDefault()).format(new Date());

        long msTime = System.currentTimeMillis();
        Date curDateTime = new Date(msTime);
        SimpleDateFormat formatter = new SimpleDateFormat("dd'/'MM hh:mm");
        //SimpleDateFormat formatter = new SimpleDateFormat("MM'/'dd'/'y hh:mm");
        currentTime = formatter.format(curDateTime);
        Log.d(TAG, "Current Time: " + currentTime);

        intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                0, intent, FLAG_IMMUTABLE);
        Notification noti = new Notification.Builder(this, CHANNEL_ID)
                //.setContentTitle("MQTT:")
                .setContentText("Start IOTUS Svc: " + currentTime)
                .setSmallIcon(R.drawable.ic_launcher_background)
                .setContentIntent(pendingIntent)
                //.setVibrate(new long[]{1000, 1000, 1000, 1000, 1000})
                .build();
        startForeground(1, noti,
                FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE |
                        FOREGROUND_SERVICE_TYPE_DATA_SYNC);
        // this is the noti that is shown when running in background
        return START_STICKY;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void sendNotification(String msg) {
        Notification noti;
        Log.d(TAG, "Send Notification...");

        // Create an explicit intent for an Activity in your app
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                0, intent, FLAG_IMMUTABLE);
        noti = new Notification.Builder(this, CHANNEL_ID)
                //.setContentTitle(title + " : ")
                .setContentText(msg)
                .setSmallIcon(R.drawable.ic_launcher_background)
                .setContentIntent(pendingIntent)
                .setSound(ringtoneUri)
                //.setDefaults(Notification.DEFAULT_ALL)
                //.setVibrate(new long[] { 1000, 1000, 1000, 1000, 1000 })
                .build();
        //noti.sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        mNotificationManager.notify(incr++, noti);
    }

    private void startMqtt(final String topic) throws MqttException {
        Log.d(TAG, "startMqtt: topic: " + topic);
        mqttHelper = new MqttHelper(getApplicationContext(), topic);
        mqttHelper.mqttAndroidClient.setCallback(new MqttCallback() {
            @Override
            public void connectionLost(Throwable throwable) {
                Log.d(TAG, "MQTT connection lost !!");
                if (topic == null) {
                    SharedPreferences preferences = PreferenceManager.
                            getDefaultSharedPreferences(getApplicationContext());
                    String temp = preferences.getString("cid", "10000");
                    Log.d(TAG, "connectionLost: CID from Shared: " + temp);
                    String temp1 = "gurupada/" + temp + "/#";
                    mqttHelper.connect(temp1);
                } else {
                    mqttHelper.connect(topic);
                }
            }

            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void messageArrived(String topic, MqttMessage mqttMessage) throws Exception {
                Map<String, String> map = new HashMap<String, String>();
                String msg = mqttMessage.toString();
                String currentTime = new SimpleDateFormat("HH-mm",
                        Locale.getDefault()).format(new Date());
                Log.d(TAG, "MQTT Msg recvd: " + msg);
                String[] arrOfStr = msg.split("[{:,]", 8);
                if (topic.contains("alarm")) {
                    // The msg is not JSON format, and so we skip the
                    // String Logging. We only do it for other topics
                } else {
                    ObjectMapper mapper = new ObjectMapper();
                    try {
                        map = mapper.readValue(msg,
                                new TypeReference<HashMap<String, String>>() {});
                        Log.d(TAG, "MQTT Msg recvd - " + "gw:" + map.get("gwid") +
                                ":" + map.get("sensorid"));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                if (topic.contains("gw")) {
                    Log.d(TAG, "Recvd GW Add mqtt data");
                    boolean found = searchFile(getApplicationContext(),
                            "gw.txt", map.get("gwid"));
                    if (found) {
                        removeLineFromFile("gw.txt",
                                map.get("gwid"));
                    }
                    writeToFile(msg, getApplicationContext(), "gw.txt");
                    Intent intent = new Intent();
                    intent.setAction("com.aseemsethi.iotus.gw");
                    intent.putExtra("msg", msg);
                    sendBroadcast(intent);
                } else if (topic.contains("temperature")) {
                    Log.d(TAG, "Recvd Temerature mqtt data");
                    boolean found = searchFile(getApplicationContext(),
                            "temperature.txt", map.get("sensorid"));
                    if (found) {
                        removeLineFromFile("temperature.txt",
                                map.get("sensorid"));
                    }
                    writeToFile(msg, getApplicationContext(), "temperature.txt");
                    Intent intent = new Intent();
                    intent.setAction("com.aseemsethi.iotus.temp");
                    intent.putExtra("msg", msg);
                    sendBroadcast(intent);
                } else if (topic.contains("door")) {
                    Log.d(TAG, "Recvd Door mqtt data");
                    boolean found = searchFile(getApplicationContext(),
                            "door.txt", map.get("sensorid"));
                    if (found) {
                        removeLineFromFile("door.txt", map.get("sensorid"));
                    }
                    writeToFile(msg, getApplicationContext(), "door.txt");
                    Intent intent = new Intent();
                    intent.setAction("com.aseemsethi.iotus.door");
                    intent.putExtra("msg", msg);
                    sendBroadcast(intent);
                } else if (topic.contains("alarm")) {
                    Log.d(TAG, "Recvd Alarm");
                    sendNotification(msg + ": " + currentTime);
                } else {
                    Log.d(TAG, "Not writing to file for topic: " + topic);
                }
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {
                //Log.d(TAG, "msg delivered");
            }
        });
    }

    private void writeToFile(String data, Context context, String filename) {
        data = data.replaceAll("\n", "");
        Log.d(TAG, "New: " + data);
        try {
            try (OutputStreamWriter outputStreamWriter =
                         new OutputStreamWriter(context.openFileOutput
                                 (filename, Context.MODE_APPEND))) {
                outputStreamWriter.write("\n" + data);
                Log.d(TAG, "Wrote to file");
                outputStreamWriter.close();
            }
        } catch (IOException e) {
            Log.e("Exception", "File write failed: " + e.toString());
        }
    }

    private boolean searchFile(Context context, String filename, String txt) {
        String ret = "";
        File file = context.getFileStreamPath(filename);
        if(file == null || !file.exists()) {
            Log.d(TAG, "File not found: " + filename);
            return false;
        }
        try {
            InputStream inputStream = context.openFileInput(filename);
            if ( inputStream != null ) {
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                String receiveString = "";
                while ( (receiveString = bufferedReader.readLine()) != null ) {
                    Log.d(TAG, "Read: " + receiveString);
                    if (receiveString.contains(txt)) {
                        Log.d(TAG, "Found it !!!!! - " + txt);
                        return true;
                    }
                }
                inputStream.close();
            }
        }
        catch (FileNotFoundException e) {
            Log.e(TAG, "File not found: " + e.toString());
        } catch (IOException e) {
            Log.e(TAG, "Can not read file: " + e.toString());
        }
        return false;
    }

    public void publish(String topic, String info)
    {
        byte[] encodedInfo = new byte[0];
        String currentTime = new SimpleDateFormat("HH-mm",
                Locale.getDefault()).format(new Date());
        try {
            encodedInfo = info.getBytes("UTF-8");
            MqttMessage message = new MqttMessage(encodedInfo);
            mqttHelper.mqttAndroidClient.publish(topic, message);
            Log.d (TAG, "publish done from: on:" + topic);
            sendNotification("Sent GPS: " + name + "/" + currentTime);
        } catch (UnsupportedEncodingException | MqttException e) {
            e.printStackTrace();
            Log.e (TAG, e.getMessage());
            //sendNotification("Failed1: " + "/" + e.getMessage());
        }catch (Exception e) {
            Log.e (TAG, "general exception "+ e.getMessage());
            //sendNotification("Failed2:" + "/" + e.getMessage());
        }
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        Log.d(TAG, "Mqtt Service task removed");
        super.onTaskRemoved(rootIntent);
        running = false;
        if (mqttHelper != null)
            mqttHelper.unsubscribeToTopic(topic);
        sendBroadcast(new Intent("RestartMqtt"));
        Context context = getApplicationContext();
        Intent serviceIntent = new Intent(context, myMqttService.class);
        serviceIntent.putExtra("topic", topic);
        serviceIntent.setAction(myMqttService.MQTTSUBSCRIBE_ACTION);
        startService(serviceIntent);
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "Mqtt Service task destroyed");
        running = false;
        if (mqttHelper != null)
            mqttHelper.unsubscribeToTopic(topic);
        sendBroadcast(new Intent("RestartMqtt"));
        // The service is no longer used and is being destroyed
        Context context = getApplicationContext();
        Intent serviceIntent = new Intent(context, myMqttService.class);
        serviceIntent.putExtra("topic", topic);
        serviceIntent.setAction(myMqttService.MQTTSUBSCRIBE_ACTION);
        startService(serviceIntent);
    }

    public void removeLineFromFile(String filename, String lineToRemove) {
        File inputFile = getApplicationContext().getFileStreamPath(filename);
        File tempFile = getApplicationContext().getFileStreamPath("myTempFile.txt");
        BufferedReader reader = null;
        BufferedWriter writer = null;
        try {
            reader = new BufferedReader(new FileReader(inputFile));
            writer = new BufferedWriter(new FileWriter(tempFile));
        } catch(IOException ex){
            Log.d(TAG, ex.getMessage()); 
        }

        String currentLine;

        try {
            while ((currentLine = reader.readLine()) != null) {
                if (currentLine.contains(lineToRemove)) {
                    Log.d(TAG, "Removing " + lineToRemove);
                    continue;
                }
                writer.write(currentLine); // System.getProperty("line.separator"));
            }
            writer.close();
            reader.close();
        } catch (IOException ex){
            Log.d(TAG, ex.getMessage());
        }
        tempFile.renameTo(inputFile);
    }
}