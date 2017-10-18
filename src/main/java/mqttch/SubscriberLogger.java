package mqttch;

import org.eclipse.paho.client.mqttv3.*;

import javax.net.ssl.*;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.security.*;
import java.security.cert.CertificateException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Created by kalistrat on 02.05.2017.
 */
public class SubscriberLogger implements MqttCallback {

    String TopicName;
    MqttClient client;
    String MqttHostName;
    String MesDataType;

    public SubscriberLogger(String topicName
            ,String mqttServerHost
                            ,String qDevLog
                            ,String qDevPass
                            ,String qMesDataType

    ) throws Throwable {

        try {
            TopicName = topicName;
            MqttHostName = mqttServerHost;
            MesDataType = qMesDataType;
            MqttConnectOptions options = new MqttConnectOptions();
            //options.setConnectionTimeout(0);
            options.setUserName(qDevLog);
            options.setPassword(qDevPass.toCharArray());

            if (mqttServerHost.contains("ssl://")) {
                SSLSocketFactory ssf = MessageHandling.configureSSLSocketFactory();
                options.setSocketFactory(ssf);
            }


            System.out.println("SubscriberLogger->topicName :" + topicName);
            System.out.println("SubscriberLogger->mqttServerHost :" + mqttServerHost);
            System.out.println("SubscriberLogger->qDevLog :" + qDevLog);
            System.out.println("SubscriberLogger->qDevPass :" + qDevPass);

            client = new MqttClient(MqttHostName, TopicName, null);

            client.connect(options);
            client.setCallback(this);
            client.subscribe(TopicName);
            //System.out.println("Я завершился");
        } catch (MqttException e1) {
            e1.printStackTrace();
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

//        List<String> TopicAttr = MessageHandling.GetListFromString(topic);
//        String iUserLog = TopicAttr.get(0);
//        Integer iDeviceId = Integer.parseInt(TopicAttr.get(1));
//        String iTopicType = TopicAttr.get(2);
//        String iStringMessage = message.toString();
//        Double iDoubleValue = MessageHandling.StrToDouble(iStringMessage);
//        if (iTopicType.equals("W")) {
//            MessageHandling.topicDataLog(iDeviceId, iStringMessage, iDoubleValue);
//        }
        try {

            String iStringMessage = message.toString().replace(" ","");
            List<String> MessAttr = MessageHandling.GetListFromStringDevider(iStringMessage + ":", ":");
            String messUnixTime = MessAttr.get(0);
            String messArrivedValue = MessAttr.get(1);
            //System.out.println("topic : " + topic);
            //java.util.Date MeasureDate = new java.util.Date(Long.valueOf(messUnixTime)*1000);
            java.sql.Timestamp MeasureDate = new java.sql.Timestamp(new Date(Long.valueOf(messUnixTime)*1000L).getTime());
            java.sql.Timestamp MeasureDateValue;
            Double messDoubleValue;

            if (MesDataType.equals("дата")) {
                String measDateTime = MessAttr.get(1);
                MeasureDateValue = new java.sql.Timestamp(new Date(Long.valueOf(measDateTime)*1000L).getTime());
                messDoubleValue = null;
            } else if (MesDataType.equals("число")) {
                messDoubleValue = MessageHandling.StrToDouble(messArrivedValue);
                MeasureDateValue = null;
            } else {
                MeasureDateValue = null;
                messDoubleValue = null;
            }

            //System.out.println("messDoubleValue : " + messDoubleValue);

            MessageHandling.logAction("Сообщение " + iStringMessage + " от устройства передано обработчику");
            //MessageHandling.topicDataLog(topic, MeasureDate, messArrivedValue ,messDoubleValue, MeasureDateValue);

            //System.out.println("topic : " + topic);
            //System.out.println("messUnixTime : " + Long.valueOf(messUnixTime));
            //System.out.println("MeasureDate : " + MeasureDate);
            //System.out.println("messArrivedValue : " + messArrivedValue);
        } catch (Exception e){
            e.printStackTrace();
        }

    }


    public void deliveryComplete(IMqttDeliveryToken token) {
        // TODO Auto-generated method stub

    }
}
