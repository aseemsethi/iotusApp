package com.aseemsethi.iotus;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.RequiresApi;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
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
            } else {
                topic = extras.getString("topic");
                Log.d(TAG, "MQTT_SUBSCRIBE - topic:" + topic);
            }
        }
        if (action == "MQTTSUBSCRIBE_TOPIC") {
            Bundle extras = intent.getExtras();
            if(extras == null) {
                Log.d(TAG,"null MQTTSUBSCRIBE_ACTION");
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

        mNotificationManager = (NotificationManager) this.getSystemService(
                Context.NOTIFICATION_SERVICE);
        NotificationChannel mChannel = new NotificationChannel(CHANNEL_ID,
                "my_channel",
                NotificationManager.IMPORTANCE_LOW);
        mChannel.enableLights(true);
        mChannel.setLightColor(Color.GREEN);
        mChannel.setSound(null, null);
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
        intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                0, intent, FLAG_IMMUTABLE);
        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        Notification noti = new Notification.Builder(this, CHANNEL_ID)
                //.setContentTitle("MQTT:")
                .setContentText("Start Svc at: " + currentTime)
                .setSmallIcon(R.drawable.ic_launcher_background)
                .setContentIntent(pendingIntent)
                .setVibrate(new long[]{1000, 1000, 1000, 1000, 1000})
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
        Uri ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
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
                //.setVibrate(new long[] { 1000, 1000, 1000, 1000, 1000 })
                //.setSound(defaultSoundUri)
                .build();
        mNotificationManager.notify(incr++, noti);
    }

    private void startMqtt(String topic) throws MqttException {
        Log.d(TAG, "startMqtt: topic: " + topic);
        mqttHelper = new MqttHelper(getApplicationContext(), topic);
        mqttHelper.mqttAndroidClient.setCallback(new MqttCallback() {
            @Override
            public void connectionLost(Throwable throwable) {
                Log.d(TAG, "MQTT connection lost !!");
                mqttHelper.connect();
            }

            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void messageArrived(String topic, MqttMessage mqttMessage) throws Exception {
                String msg = mqttMessage.toString();
                String currentTime = new SimpleDateFormat("HH-mm",
                        Locale.getDefault()).format(new Date());
                Log.d(TAG, "MQTT Msg recvd: " + msg);
                String[] arrOfStr = msg.split("[{:,]", 8);
                Log.d(TAG, "MQTT Msg recvd " +
                        "1st- " + arrOfStr[0] + " 2nd - " + arrOfStr[1] +
                        "3rd - " + arrOfStr[2] + " 4th - " + arrOfStr[3] +
                        "4th - " + arrOfStr[4]);
                if (topic.contains("gw")) {
                    Log.d(TAG, "Recvd GW Add mqtt data");
                    boolean found = searchFile(getApplicationContext(),
                            "gw.txt", arrOfStr[2]);
                    if (found == false)
                        writeToFile(msg, getApplicationContext(), "gw.txt");
                } else if (topic.contains("temperature")) {
                    Log.d(TAG, "Recvd Temerature mqtt data");
                    boolean found = searchFile(getApplicationContext(),
                            "temperature.txt", arrOfStr[4]); // sensorId
                    if (found == false)
                        writeToFile(msg, getApplicationContext(), "temperature.txt");
                } else if (topic.contains("door")) {
                    Log.d(TAG, "Recvd Door mqtt data");
                    boolean found = searchFile(getApplicationContext(),
                            "door.txt", arrOfStr[4]); // sensorId
                    if (found == false)
                        writeToFile(msg, getApplicationContext(), "door.txt");
                } else {
                    Log.d(TAG, "Not writing to file for topic: " + topic);
                }
                Intent intent = new Intent();
                intent.setAction("com.aseemsethi.iotus.msg");
                intent.putExtra("msg", msg);
                sendBroadcast(intent);
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
        serviceIntent.setAction(myMqttService.MQTTSUBSCRIBE_ACTION);
        startService(serviceIntent);
    }
}