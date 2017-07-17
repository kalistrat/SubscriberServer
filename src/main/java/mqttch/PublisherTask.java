package mqttch;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created by kalistrat on 17.07.2017.
 */
public class PublisherTask {

final String taskTypeName;
int intervalDays;

    public PublisherTask(int qIntervalDays, String qTaskTypeName){
        taskTypeName = qTaskTypeName;
        intervalDays = qIntervalDays;

        ScheduledExecutorService ses =
                Executors.newScheduledThreadPool(1);
        Runnable pinger = new Runnable() {
            public void run() {
                //System.out.println("PING!");
                if (taskTypeName.equals("SYNCTIME")) {

                }
            }
        };
        ses.scheduleAtFixedRate(pinger, intervalDays, intervalDays, TimeUnit.SECONDS);
    }

    public void publishTimeValue(){
        try {
            String mqttServerHost = "localhost:8883";
            String topicName = "/garage1/temp1";
            long unixTime = System.currentTimeMillis() / 1000L;

            String MessCode = String.valueOf(unixTime) + " : " + "23.006";
            MqttClient client = new MqttClient("ssl://" + mqttServerHost, "PahoPublisher", null);

            //SSLSocketFactory ssf = configureSSLSocketFactory();

            MqttConnectOptions options = new MqttConnectOptions();


            MqttMessage message = new MqttMessage(MessCode.getBytes());

            //message.setQos(0);
            client.connect(options);
            client.publish(topicName, message);
            client.disconnect();
        } catch(MqttException me) {
            me.printStackTrace();
        }
    }
}
