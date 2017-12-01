package mqttch;

import org.eclipse.paho.client.mqttv3.*;

import javax.net.ssl.SSLSocketFactory;
import java.util.Date;
import java.util.List;

/**
 * Created by kalistrat on 07.06.2017.
 */
public class ConditionVariable implements MqttCallback, VarChangeListenable {

    String TopicName;
    MqttClient client;
    String MqttHostName;
    Double VarValue;
    java.util.Date VarDate;
    String VarName;
    VarListener varListener;
    Integer deltaTimeSec;
    Integer iConditionId;

    public ConditionVariable(int conditionId
    ,String topicName
    ,String mqttServerHost
    ,String varName
    ,String wControlLog
    ,String wControlPass

    ) throws Throwable {

        try {
            TopicName = topicName;
            MqttHostName = mqttServerHost;
            VarName = varName;
            VarDate = null;
            VarValue = null;
            deltaTimeSec = null;
            iConditionId = conditionId;

//            System.out.println("TopicName :" + TopicName);
//            System.out.println("MqttHostName :" + MqttHostName);
//            System.out.println("VarName :" + VarName);
//            System.out.println("VarDate :" + VarDate);
//            System.out.println("VarValue :" + VarValue);
//            System.out.println("deltaTimeSec :" + deltaTimeSec);
//            System.out.println("iConditionId :" + iConditionId);


            //client = new MqttClient(mqttServerHost, String.valueOf(eConditonId) + varName, null);

            client = new MqttClient(mqttServerHost, MqttClient.generateClientId(), null);

            MqttConnectOptions options = new MqttConnectOptions();
            options.setUserName(wControlLog);
            options.setPassword(wControlPass.toCharArray());

            if (mqttServerHost.contains("ssl://")) {
                SSLSocketFactory ssf = MessageHandling.configureSSLSocketFactory();
                options.setSocketFactory(ssf);
            }

            options.setConnectionTimeout(0);

            client.connect(options);
            client.setCallback(this);
            client.subscribe(TopicName);
        } catch (MqttException e1) {
            //System.out.println("MqttException in ConditionVariable");
            throw  e1;
        }

    }

    public void connectionLost(Throwable cause) {
        // TODO Auto-generated method stub

    }


    public void messageArrived(String topic, MqttMessage message) {
         try {
             //System.out.println("AAAAAAAAA topic : " + topic + MessageHandling.safeLongToInt(((new Date()).getTime()) / 1000));
             setVariableValue(message.toString());
             varListener.afterValueChange(this);
             //System.out.println("topic : " + topic);
         } catch (Exception e) {
             e.printStackTrace();
         }
    }


    public void deliveryComplete(IMqttDeliveryToken token) {
        // TODO Auto-generated method stub

    }

    public void setVariableValue(String newVal){
        VarValue = MessageHandling.StrToDouble(newVal);
        VarDate = new Date();
        deltaTimeSec = MessageHandling.safeLongToInt(((new Date()).getTime()-VarDate.getTime())/1000);
    }

    public void setVarListener(VarListener listener) {
        this.varListener = listener;
    }

}
