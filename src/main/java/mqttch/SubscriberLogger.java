package mqttch;

import org.eclipse.paho.client.mqttv3.*;

import java.util.List;

/**
 * Created by kalistrat on 02.05.2017.
 */
public class SubscriberLogger implements MqttCallback {

    String TopicName;
    MqttClient client;
    String MqttHostName;

    public SubscriberLogger(String topicName,String mqttServerHost) throws Throwable {

        try {
            TopicName = topicName;
            MqttHostName = mqttServerHost;
            MqttConnectOptions options = new MqttConnectOptions();
            options.setConnectionTimeout(0);
            //options.setKeepAliveInterval(0);
            client = new MqttClient("tcp://" + MqttHostName, TopicName, null);
            client.connect(options);
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
        //System.out.println(topic + " : " + message);
        //TopicName: UserLog/DeviceId/W

        List<String> TopicAttr = MessageHandling.GetListFromString(topic);
        String iUserLog = TopicAttr.get(0);
        Integer iDeviceId = Integer.parseInt(TopicAttr.get(1));
        String iTopicType = TopicAttr.get(2);
        String iStringMessage = message.toString();
        Double iDoubleValue = MessageHandling.StrToDouble(iStringMessage);
        if (iTopicType.equals("W")) {
            MessageHandling.topicDataLog(iDeviceId, iStringMessage, iDoubleValue);
        }

    }


    public void deliveryComplete(IMqttDeliveryToken token) {
        // TODO Auto-generated method stub

    }
}
