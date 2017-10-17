package mqttch;

import io.moquette.interception.AbstractInterceptHandler;
import io.moquette.interception.InterceptHandler;
import io.moquette.interception.messages.InterceptPublishMessage;
import io.moquette.server.Server;
import org.eclipse.paho.client.mqttv3.MqttException;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Properties;

/**
 * Created by kalistrat on 27.09.2017.
 */
public class internalMqttServer extends Server {
    String passWordFilePath;
    List<DtransitionCondition> ConditionList;
    List<PublisherTask> PublisherTaskList;
    String iUserLog;

    static class PublisherListener extends AbstractInterceptHandler {

        @Override
        public String getID() {
            return "EmbeddedLauncherPublishListener";
        }

        @Override
        public void onPublish(InterceptPublishMessage msg) {
            System.out.println(
                    "Received on topic: " + msg.getTopicName().toString() + " content: " + StandardCharsets.UTF_8.decode(msg.getPayload().nioBuffer()).toString());
        }
    }

    List<? extends InterceptHandler> userHandlers;
    Properties configProps;

    public internalMqttServer(String UserLog
    ,String RegularPort
    ,String SecurePort
    ) throws InterruptedException, IOException {

        configProps = new Properties();
        iUserLog = UserLog;

        passWordFilePath = Main.AbsPath +"passwords/"+iUserLog+".conf";
        File passFile = new File(passWordFilePath);
        passFile.mkdirs();

        System.out.println("passfilename : " + passWordFilePath);

        configProps.setProperty("port", RegularPort);
        configProps.setProperty("host", "0.0.0.0");
        configProps.setProperty("password_file", passWordFilePath);
        configProps.setProperty("allow_anonymous", Boolean.FALSE.toString());
        configProps.setProperty("authenticator_class", "");
        configProps.setProperty("authorizator_class", "");
        configProps.setProperty("ssl_port",SecurePort);
        configProps.setProperty("jks_path",Main.AbsPath +"serverkeystore.jks");
        configProps.setProperty("key_store_password","3PointShotMqtt");
        configProps.setProperty("key_manager_password","3PointShotMqtt");

        userHandlers = Collections.singletonList(new PublisherListener());
        this.startServer(configProps);
        for (int i=0; i<userHandlers.size(); i++) {
            this.addInterceptHandler(userHandlers.get(0));
        }
    }

    public boolean addServerTask(
            String oTaskTypeName
            ,int oTaskInterval
            ,String oIntervalType
            ,String oWriteTopicName
            ,String oServerIp
            ,String oControlLog
            ,String oControlPass
            ,String oMessageValue
    ) throws Throwable {


        int indx = -1;
        for (PublisherTask iObj : this.PublisherTaskList) {
            if (iObj.iWriteTopicName.equals(oWriteTopicName)) {
                indx = this.PublisherTaskList.indexOf(iObj);
            }
        }

        if (indx != -1) {
            this.PublisherTaskList.add(new PublisherTask(
                    oTaskTypeName
                    , oTaskInterval
                    , oIntervalType
                    , oWriteTopicName
                    , oServerIp
                    , oControlLog
                    , oControlPass
                    , oMessageValue
            ));
            return true;
        } else {
            return false;
        }

    }

    public boolean deleteServerTask(
            String oWriteTopicName
    ) throws Throwable {

        int indx = -1;
        for (PublisherTask iObj : this.PublisherTaskList) {
            if (iObj.iWriteTopicName.equals(oWriteTopicName)) {
                indx = this.PublisherTaskList.indexOf(iObj);
            }
        }

        if (indx != -1) {
            PublisherTask remTask = this.PublisherTaskList.get(indx);
            remTask = null;
            this.PublisherTaskList.remove(indx);
            System.gc();
            return true;
        } else {
            return false;
        }

    }

    public void createPublisherTaskList() throws Throwable {
        try {
            Class.forName(MessageHandling.JDBC_DRIVER);
            Connection Con = DriverManager.getConnection(
                    MessageHandling.DB_URL
                    , MessageHandling.USER
                    , MessageHandling.PASS
            );

            String DataSql = "select tt.task_type_name\n" +
                    ",tas.task_interval\n" +
                    ",tas.interval_type\n" +
                    ",case when tt.task_type_name = 'SYNCTIME' then udtr.time_topic\n" +
                    "else null end write_topic_name\n" +
                    ",ms.server_ip\n" +
                    ",udtr.control_log\n" +
                    ",udtr.control_pass\n" +
                    ",case when tt.task_type_name = 'SYNCTIME' then tz.timezone_value\n" +
                    "else null end message_value\n" +
                    "from user_device_task tas\n" +
                    "join user_device ud on ud.user_device_id=tas.user_device_id\n" +
                    "join users usr on usr.user_id=ud.user_id \n" +
                    "join task_type tt on tt.task_type_id=tas.task_type_id\n" +
                    "left join user_devices_tree udtr on udtr.user_device_id=tas.user_device_id\n" +
                    "left join mqtt_servers ms on ms.server_id=udtr.mqtt_server_id\n" +
                    "left join timezones tz on tz.timezone_id=udtr.timezone_id\n" +
                    "where usr.user_log=?";

            PreparedStatement DataStmt = Con.prepareStatement(DataSql);
            DataStmt.setString(1,iUserLog);
            ResultSet DataRs = DataStmt.executeQuery();
            while (DataRs.next()) {
                this.PublisherTaskList.add(new PublisherTask(
                        DataRs.getString(1)
                        , DataRs.getInt(2)
                        , DataRs.getString(3)
                        , DataRs.getString(4)
                        , DataRs.getString(5)
                        , DataRs.getString(6)
                        , DataRs.getString(7)
                        , DataRs.getString(8)
                ));
            }
            Con.close();

        } catch (SQLException se3) {
            //Handle errors for JDBC
            se3.printStackTrace();
        } catch (Exception e13) {
            //Handle errors for Class.forName
            e13.printStackTrace();
        }
    }

    public void addControllerPassWord(String ControlName, String ControlPassSha){
        try {
            FileWriter fw = new FileWriter(passWordFilePath, true); //the true will append the new data
            fw.append("\n" + ControlName + ":" + ControlPassSha);//appends the string to the file
            fw.close();
        }  catch (Exception e){
            e.printStackTrace();
        }
    }

    public void deleteControllerPassWord (
            String ControlName
            , String ControlPassSha
    ){
        try {

            Charset charset = StandardCharsets.UTF_8;

            String filename = passWordFilePath.replaceFirst("^/(.:/)", "$1");
            System.out.println("filename.replaceFirst : " + filename);

            byte[] encoded = Files.readAllBytes(Paths.get(filename));
            String fileContent = new String(encoded, charset);


            String trimFileContent = fileContent.replace("\n" + ControlName + ":" + ControlPassSha,"");
            FileWriter fw = new FileWriter(filename);
            fw.write(trimFileContent);
            fw.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void rebootMqttServer() throws InterruptedException, IOException, MqttException, Throwable {

        for (DtransitionCondition iRule : this.ConditionList) {
            DtransitionCondition delRule = iRule;
            delRule.disconnectVarList();
            delRule = null;
        }
        this.ConditionList.clear();

        for (PublisherTask iTask : this.PublisherTaskList) {
            PublisherTask delTask = iTask;
            delTask = null;
        }
        this.PublisherTaskList.clear();
        stopServer();
        System.gc();

        this.startServer(configProps);
        for (int i=0; i<this.userHandlers.size(); i++) {
            this.addInterceptHandler(this.userHandlers.get(0));
        }
        //Thread.sleep(2000);
        System.out.println("Сервер перезагружен...");
        System.out.println("Создание подписчиков для датчиков...");
        //MessageHandling.createSubscriberLoggerList();
        System.out.println("Создание заданий...");
        createPublisherTaskList();
        System.out.println("Подписка завершена");

    }

}
