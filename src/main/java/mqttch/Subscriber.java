package mqttch;

import org.eclipse.paho.client.mqttv3.*;

/**
 * Created by kalistrat on 02.05.2017.
 */
public class Subscriber implements MqttCallback {

    String TopicName;
    MqttClient client;

    public Subscriber(String topicName) throws Throwable {

        try {
            TopicName = topicName;
            client = new MqttClient("tcp://localhost:1883", TopicName);
            client.connect();
            client.setCallback(this);
            client.subscribe(TopicName);
            //System.out.println("Я завершился");
        } catch (MqttException e1) {
            //e1.printStackTrace();
            throw  e1;
        }

    }

//    @Override
//    public void run() {
//        try {
//            TopicName = topicName;
//            client = new MqttClient("tcp://localhost:1883", "Send");
//            client.connect();
//            client.setCallback(this);
//            client.subscribe(TopicName);
//            System.out.println("Я завершился");
//        } catch (MqttException e1) {
//            e1.printStackTrace();
//        }
//    }

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
