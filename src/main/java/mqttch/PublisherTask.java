package mqttch;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import javax.net.ssl.SSLContext;
import java.security.GeneralSecurityException;
import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created by kalistrat on 17.07.2017.
 */
public class PublisherTask {

        final String iTaskTypeName;
        int iIntervalValue;
        String iWriteTopicName;

    public PublisherTask(
            String qTaskTypeName
            ,int qTaskInterval
            ,String qIntervalType
            ,String qWriteTopicName
            ,final String qServerIp
            ,final String qControlLog
            ,final String qControlPass
            ,final String qMessageValue
    ){

        iTaskTypeName = qTaskTypeName;
        iIntervalValue = qTaskInterval;
        iWriteTopicName = qWriteTopicName;

        ScheduledExecutorService ses =
                Executors.newScheduledThreadPool(1);
        Runnable pinger = new Runnable() {
            public void run() {
                //System.out.println("PING!");
                if (iTaskTypeName.equals("SYNCTIME")) {
                    publishTimeValue(
                            iWriteTopicName
                            ,qServerIp
                            ,qControlLog
                            ,qControlPass
                            ,qMessageValue
                    );
                }
            }
        };
        if (qIntervalType.equals("DAYS")) {
            ses.scheduleAtFixedRate(pinger, iIntervalValue, iIntervalValue, TimeUnit.DAYS);
        } else if (qIntervalType.equals("HOURS")){
            ses.scheduleAtFixedRate(pinger, iIntervalValue, iIntervalValue, TimeUnit.HOURS);
        } else if (qIntervalType.equals("MINUTES")){
            ses.scheduleAtFixedRate(pinger, iIntervalValue, iIntervalValue, TimeUnit.MINUTES);
        }
    }

    public void publishTimeValue(
            String wWriteTopicName
            ,String wServerIp
            ,String wControlLog
            ,String wControlPass
            ,String wMessageValue
    ){
        try {

            Date syncDate = MessageHandling.redefineSyncDate(wMessageValue);
            long unixSyncDate = syncDate.getTime() / 1000L;
            String MessCode = String.valueOf(unixSyncDate) + " : " + String.valueOf(unixSyncDate);
            MqttClient client = new MqttClient(wServerIp, wControlLog, null);
            MqttConnectOptions options = new MqttConnectOptions();

            if (wServerIp.contains("ssl://")) {
                SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
                sslContext.init(null, null, null);
                options.setSocketFactory(sslContext.getSocketFactory());
            }


            MqttMessage message = new MqttMessage(MessCode.getBytes());

            client.connect(options);
            client.publish(wWriteTopicName, message);
            client.disconnect();
        } catch(MqttException | GeneralSecurityException me) {
            me.printStackTrace();
        }
    }
}
