package mqttch;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttMessage;

/**
 * Created by kalistrat on 02.05.2017.
 */
public class Subscriber extends Thread implements MqttCallback {

    String TopicName;
    MqttClient client;

    public Subscriber(String topicName){

        TopicName = topicName;

        setDaemon(true);
        setPriority(NORM_PRIORITY);
        start();

    }

    public void run() {
        try  {



        }
        catch(Exception e){
            System.out.println("ошибка: "+e);
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
