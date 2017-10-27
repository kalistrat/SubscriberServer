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

    public ConditionVariable(int conditionId
    ,String topicName
    ,String mqttServerHost
    ,String varName
    ,String wControlLog
    ,String wControlPass
                             ,int eConditonId
    ) throws Throwable {

        try {
            TopicName = topicName;
            MqttHostName = mqttServerHost;
            VarName = varName;
            VarDate = null;
            VarValue = null;
            deltaTimeSec = null;


            client = new MqttClient(mqttServerHost, String.valueOf(eConditonId) + varName, null);
            MqttConnectOptions options = new MqttConnectOptions();
            options.setUserName(wControlLog);
            options.setPassword(wControlPass.toCharArray());

            if (mqttServerHost.contains("ssl://")) {
                SSLSocketFactory ssf = MessageHandling.configureSSLSocketFactory();
                options.setSocketFactory(ssf);
            }

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
        varListener.afterValueChange(this);
        //System.out.println("topic : " + topic);
    }


    public void deliveryComplete(IMqttDeliveryToken token) {
        // TODO Auto-generated method stub

    }

    public void setVariableValue(String newVal){
        VarValue = MessageHandling.StrToDouble(newVal);
        deltaTimeSec = MessageHandling.safeLongToInt(((new Date()).getTime()-VarDate.getTime())/1000);
        VarDate = new Date();
    }

    public void setVarListener(VarListener listener) {
        this.varListener = listener;
    }

}
