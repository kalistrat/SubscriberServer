package mqttch;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import javax.net.ssl.*;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.*;
import java.security.cert.CertificateException;
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
    )throws Throwable {

        try {

        iTaskTypeName = qTaskTypeName;
        iIntervalValue = qTaskInterval;
        iWriteTopicName = qWriteTopicName;

        mqttServerConnectionTest(
                qServerIp
                ,qControlLog
                ,qControlPass
        );


        ScheduledExecutorService ses =
                Executors.newScheduledThreadPool(1);
        Runnable pinger = new Runnable() {
            public void run() {
                //System.out.println("PING!");
                if (iTaskTypeName.equals("SYNCTIME")) {
                    try {
                    publishTimeValue(
                            iWriteTopicName
                            ,qServerIp
                            ,qControlLog
                            ,qControlPass
                            ,qMessageValue
                    );
                    } catch (Throwable e3) {
                        //e3.printStackTrace();
                        //System.out.println("pinger running..");
                        throw  e3;
                    }

                }
            }
        };
        if (qIntervalType.equals("DAYS")) {
            ses.scheduleAtFixedRate(pinger, iIntervalValue, iIntervalValue, TimeUnit.DAYS);
        } else if (qIntervalType.equals("HOURS")){
            ses.scheduleAtFixedRate(pinger, iIntervalValue, iIntervalValue, TimeUnit.HOURS);
        } else if (qIntervalType.equals("MINUTES")){
            ses.scheduleAtFixedRate(pinger, iIntervalValue, iIntervalValue, TimeUnit.MINUTES);
        } else if (qIntervalType.equals("SECONDS")){
            ses.scheduleAtFixedRate(pinger, iIntervalValue, iIntervalValue, TimeUnit.SECONDS);
        }
        } catch (Throwable e1) {
            //e1.printStackTrace();
            throw  e1;
        }
    }

    public void publishTimeValue(
            String wWriteTopicName
            ,String wServerIp
            ,String wControlLog
            ,String wControlPass
            ,String wMessageValue
    ) {
        try {

            Date syncDate = MessageHandling.redefineSyncDate(wMessageValue);
            long unixSyncDate = syncDate.getTime() / 1000L;
            String MessCode = String.valueOf(unixSyncDate);
            MqttClient client = new MqttClient(wServerIp, wControlLog, null);
            MqttConnectOptions options = new MqttConnectOptions();
            options.setUserName(wControlLog);
            options.setPassword(wControlPass.toCharArray());

            if (wServerIp.contains("ssl://")) {
                SSLSocketFactory ssf = MessageHandling.configureSSLSocketFactory();
                options.setSocketFactory(ssf);
            }


            MqttMessage message = new MqttMessage(MessCode.getBytes());

            client.connect(options);
            client.publish(wWriteTopicName, message);
            client.disconnect();
        } catch(MqttException | GeneralSecurityException | IOException me) {
            me.printStackTrace();
            //System.out.println("publishTimeValue exception");
        }

    }



    public void mqttServerConnectionTest(
           String wServerIp
            ,String wControlLog
            ,String wControlPass
    ) throws Throwable {
        try {

            String MessCode = "try connection";
            MqttClient client = new MqttClient(wServerIp, wControlLog, null);
            MqttConnectOptions options = new MqttConnectOptions();
            options.setUserName(wControlLog);
            options.setPassword(wControlPass.toCharArray());

            if (wServerIp.contains("ssl://")) {
                SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
                sslContext.init(null, null, null);
                options.setSocketFactory(sslContext.getSocketFactory());
            }


            MqttMessage message = new MqttMessage(MessCode.getBytes());

            client.connect(options);
            client.publish("/taskTopic", message);
            client.disconnect();
        } catch(MqttException | GeneralSecurityException me) {
            //me.printStackTrace();
            throw  me;
        }

    }
}
