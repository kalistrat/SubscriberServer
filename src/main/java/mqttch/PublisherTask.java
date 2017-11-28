package mqttch;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.w3c.dom.Document;

import javax.net.ssl.*;
import javax.xml.xpath.XPathFactory;
import java.io.IOException;
import java.security.*;
import java.sql.*;
import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created by kalistrat on 17.07.2017.
 */
public class PublisherTask {

    String iTaskTypeName;
    int iIntervalValue;
    String iIntervalType;
    String iWriteTopicName;
    String iServerIp;
    String iControlLog;
    String iControlPass;
    String iMessageValue;
    ScheduledExecutorService ses;
    Integer iTaskId;


    public PublisherTask(int qTaskId)throws Throwable {

        try {
            iTaskId = qTaskId;

            Document xmlDocument = MessageHandling
                    .loadXMLFromString(getTaskData(qTaskId));

            iTaskTypeName = XPathFactory.newInstance().newXPath()
                    .compile("/task_data/task_type_name").evaluate(xmlDocument);
            iIntervalValue = Integer.parseInt(XPathFactory.newInstance().newXPath()
                    .compile("/task_data/task_interval").evaluate(xmlDocument));
            iIntervalType = XPathFactory.newInstance().newXPath()
                    .compile("/task_data/interval_type").evaluate(xmlDocument);
            iWriteTopicName = XPathFactory.newInstance().newXPath()
                    .compile("/task_data/write_topic_name").evaluate(xmlDocument);
            iServerIp = XPathFactory.newInstance().newXPath()
                    .compile("/task_data/server_ip").evaluate(xmlDocument);
            iControlLog = XPathFactory.newInstance().newXPath()
                    .compile("/task_data/control_log").evaluate(xmlDocument);
            iControlPass = XPathFactory.newInstance().newXPath()
                    .compile("/task_data/control_pass").evaluate(xmlDocument);
            iMessageValue = XPathFactory.newInstance().newXPath()
                    .compile("/task_data/message_value").evaluate(xmlDocument);


        ses =
                Executors.newScheduledThreadPool(1);
        Runnable pinger = new Runnable() {
            public void run() {
                //System.out.println("PING!");
                if (iTaskTypeName.equals("SYNCTIME")) {
                    try {
                    publishTimeValue();
                    } catch (Throwable e3) {
                        //e3.printStackTrace();
                        //System.out.println("pinger running..");
                        throw  e3;
                    }

                } else {
                    MessageHandling.publishMqttMessage(
                            iWriteTopicName
                            ,iServerIp
                            ,iControlLog
                            ,iControlPass
                            ,iMessageValue
                    );
                }
            }
        };
        if (iIntervalType.equals("DAYS")) {
            ses.scheduleAtFixedRate(pinger, 0, iIntervalValue, TimeUnit.DAYS);
        } else if (iIntervalType.equals("HOURS")){
            ses.scheduleAtFixedRate(pinger, 0, iIntervalValue, TimeUnit.HOURS);
        } else if (iIntervalType.equals("MINUTES")){
            ses.scheduleAtFixedRate(pinger, 0, iIntervalValue, TimeUnit.MINUTES);
        } else if (iIntervalType.equals("SECONDS")){
            ses.scheduleAtFixedRate(pinger, 0, iIntervalValue, TimeUnit.SECONDS);
        }
        } catch (Throwable e1) {
            //e1.printStackTrace();
            throw  e1;
        }
    }

    private String getTaskData(int qTaskId){
        try {

            Class.forName(MessageHandling.JDBC_DRIVER);
            Connection Con = DriverManager.getConnection(
                    MessageHandling.DB_URL
                    , MessageHandling.USER
                    , MessageHandling.PASS
            );

            CallableStatement Stmt = Con.prepareCall("{? = call s_get_task_data(?)}");
            Stmt.registerOutParameter(1, Types.BLOB);
            Stmt.setInt(2, qTaskId);
            Stmt.execute();
            Blob CondValue = Stmt.getBlob(1);
            //System.out.println("qTaskId :" + qTaskId);
            //System.out.println("CondValue" + CondValue);

            String resultStr = new String(CondValue.getBytes(1l, (int) CondValue.length()));
            Con.close();
            return resultStr;

        }catch(SQLException se){
            //Handle errors for JDBC
            se.printStackTrace();
            return null;
        }catch(Exception e) {
            //Handle errors for Class.forName
            e.printStackTrace();
            return null;
        }
    }

    private void publishTimeValue() {
        try {

            Date syncDate = MessageHandling.redefineSyncDate(iMessageValue);
            long unixSyncDate = syncDate.getTime() / 1000L;
            String MessCode = String.valueOf(unixSyncDate);
            String clientIdPostFix = String.valueOf((new Date()).getTime() / 1000L);
            //MqttClient client = new MqttClient(iServerIp, iControlLog + clientIdPostFix, null);
            MqttClient client = new MqttClient(iServerIp, MqttClient.generateClientId(), null);

            MqttConnectOptions options = new MqttConnectOptions();
            options.setUserName(iControlLog);
            options.setPassword(iControlPass.toCharArray());

            if (iServerIp.contains("ssl://")) {
                SSLSocketFactory ssf = MessageHandling.configureSSLSocketFactory();
                options.setSocketFactory(ssf);
            }
            MqttMessage message = new MqttMessage(MessCode.getBytes());
            client.connect(options);

            //System.out.println("publishTimeValue : iControlLog : " + iControlLog);
            //System.out.println("publishTimeValue : iControlPass : " + iControlPass);
            //System.out.println("publishTimeValue : iServerIp : " + iServerIp);
            //System.out.println("publishTimeValue : message : " + message);
            //System.out.println("publishTimeValue : iWriteTopicName : " + iWriteTopicName);

            client.publish(iWriteTopicName, message);
            client.disconnect();

        } catch(MqttException | GeneralSecurityException | IOException me) {
            me.printStackTrace();
            //System.out.println("publishTimeValue EXCEPTION!!!!!!!!!!");
        }

    }

}
