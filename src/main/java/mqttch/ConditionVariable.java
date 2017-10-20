package mqttch;

import org.eclipse.paho.client.mqttv3.*;

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
            List<String> topicValues = MessageHandling.GetListFromString(topicName);
            System.out.println("topicValues.get(0) : " + topicValues.get(0));
            MqttConnectOptions options = new MqttConnectOptions();
            options.setConnectionTimeout(0);
            client = new MqttClient("tcp://" + MqttHostName, topicValues.get(0) + "/" +String.valueOf(conditionId) + "/" +VarName + "/", null);
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


    public void messageArrived(String topic, MqttMessage message)
            throws Exception {
        setVariableValue(message.toString());
        varListener.afterValueChange(VarName);
        //System.out.println("topic : " + topic);
    }


    public void deliveryComplete(IMqttDeliveryToken token) {
        // TODO Auto-generated method stub

    }

    public void setVariableValue(String newVal){
        VarValue = MessageHandling.StrToDouble(newVal);
        VarDate = new Date();
    }

    public void setVarListener(VarListener listener) {
        this.varListener = listener;
    }

}
