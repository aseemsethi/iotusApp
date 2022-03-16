package com.aseemsethi.iotus;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static java.lang.Thread.sleep;

/**
 * Created by wildan on 3/19/2017.
 */
public class MqttHelper {
    public MqttAndroidClient mqttAndroidClient;
    final String TAG = "iotus MQTTHelper";
    //final String serverUri = "tcp://mqtt.eclipseprojects.io:1883";
    final String serverUri = "tcp://52.66.70.168:1883";
    final String clientId = UUID.randomUUID().toString();
    String subscriptionTopic;

    public MqttHelper(Context context, String topic){
        Log.d(TAG, "MQTT Helper called with topic: " + topic);
        subscriptionTopic = topic;
        mqttAndroidClient = new MqttAndroidClient(context, serverUri, clientId);
        mqttAndroidClient.setCallback(new MqttCallback() {
            @Override
            public void connectionLost(Throwable throwable) {
                //Log.d(TAG, "22 MQTT connection lost");
            }

            @Override
            public void messageArrived(String topic, MqttMessage mqttMessage) throws Exception {
                Log.d(TAG, "22 MQTT Msg recvd: " + mqttMessage.toString());
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {

            }
        });
        if (topic == null) {
            SharedPreferences preferences = PreferenceManager.
                    getDefaultSharedPreferences(context);
            topic = preferences.getString("cid", "10000");
            Log.d(TAG, "MqttHelper: CID from Shared: " + topic);
        }
        connect(topic);
    }

    public boolean isConnected() {
        if (mqttAndroidClient.isConnected() == true) {
            return true;
        } else {
            return false;
        }
    }

    public void connect(String topic){
        subscriptionTopic = topic;
        MqttConnectOptions mqttConnectOptions = new MqttConnectOptions();
        mqttConnectOptions.setCleanSession(true); // was false
        mqttConnectOptions.setKeepAliveInterval(20);
        mqttConnectOptions.setConnectionTimeout(0);
        mqttConnectOptions.setUserName("draadmin");
        mqttConnectOptions.setPassword("DRAAdmin@123".toCharArray());
        try {
            mqttAndroidClient.connect(mqttConnectOptions,
                    null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    subscribeToTopic(subscriptionTopic);
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Log.d(TAG, "Failed to connect to: " + serverUri + exception.toString());
                    try {
                        TimeUnit.SECONDS.sleep(2);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    connect(topic);
                }
            });
        } catch (MqttException ex){
            ex.printStackTrace();
        }
    }

    public void subscribeToTopic(final String subscriptionTopic) {
        Log.d(TAG, "subscribeToTopic: " + subscriptionTopic);
        try {
            mqttAndroidClient.subscribe(subscriptionTopic, 0, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Log.d(TAG,"Subscribed to: " + subscriptionTopic);
                }
                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Log.d(TAG, "Subscribed fail!");
                }
            });
        } catch (MqttException ex) {
            System.err.println("Exception whilst subscribing");
            ex.printStackTrace();
        }
    }

    public void unsubscribeToTopic(final String subscriptionTopic) {
        Log.d(TAG, "Unsubscribe: " + subscriptionTopic);
        try {
            mqttAndroidClient.unsubscribe(subscriptionTopic);
        } catch (MqttException ex) {
            System.err.println("Exception while unsubscribing");
            ex.printStackTrace();
        }
    }
}
