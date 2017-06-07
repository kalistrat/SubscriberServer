package mqttch;

import org.eclipse.paho.client.mqttv3.*;

import java.util.Date;
import java.util.List;

/**
 * Created by kalistrat on 07.06.2017.
 */
public class ConditionVariable implements MqttCallback,Listenable {

    String TopicName;
    MqttClient client;
    String MqttHostName;
    Double VarValue;
    Date VarDate;
    String VarName;
    VarListener varListener;

    public ConditionVariable(int conditionId
            ,String topicName
            ,String mqttServerHost
            , String varName) throws Throwable {

        try {
            TopicName = topicName;
            MqttHostName = mqttServerHost;
            VarName = varName;
            VarDate = null;
            VarValue = null;
            MqttConnectOptions options = new MqttConnectOptions();
            options.setConnectionTimeout(0);
            client = new MqttClient("tcp://" + MqttHostName, VarName + String.valueOf(conditionId), null);
            client.connect(options);
            client.setCallback(this);
            client.subscribe(TopicName);
        } catch (MqttException e1) {
            throw  e1;
        }

    }

    public void connectionLost(Throwable cause) {
        // TODO Auto-generated method stub

    }


    public void messageArrived(String topic, MqttMessage message)
            throws Exception {
        setVariableValue(message.toString());
        varListener.afterValueChange(VarName);
        System.out.println("topic : " + topic);
    }


    public void deliveryComplete(IMqttDeliveryToken token) {
        // TODO Auto-generated method stub

    }

    public void setVariableValue(String newVal){
        VarValue = MessageHandling.StrToDouble(newVal);
        VarDate = new Date();
    }

    public void setListener(VarListener listener) {
        this.varListener = listener;
    }
}
