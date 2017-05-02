package mqttch;

import org.eclipse.paho.client.mqttv3.*;

/**
 * Created by kalistrat on 02.05.2017.
 */
public class Subscriber extends Thread implements MqttCallback {

    String TopicName;
    MqttClient client;

    public Subscriber(String topicName){

        try {
            TopicName = topicName;
            client = new MqttClient("tcp://localhost:1883", "Send");
            setDaemon(true);
            setPriority(NORM_PRIORITY);
            start();
        } catch (MqttException e) {
            e.printStackTrace();
        }

    }

    public void run() {
        try {

            client.connect();
            client.setCallback(this);
            client.subscribe(TopicName);

        } catch (MqttException e) {
            e.printStackTrace();
        }

    }

    public void connectionLost(Throwable cause) {
        // TODO Auto-generated method stub

    }


    public void messageArrived(String topic, MqttMessage message)
            throws Exception {
        System.out.println(topic + " : " + message);
    }


    public void deliveryComplete(IMqttDeliveryToken token) {
        // TODO Auto-generated method stub

    }
}
