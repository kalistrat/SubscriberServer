package mqttch;


import org.eclipse.paho.client.mqttv3.MqttException;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import javax.net.ssl.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.*;
import java.security.cert.CertificateException;
import java.sql.*;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Date;

/**
 * Created by kalistrat on 02.05.2017.
 */
public class MessageHandling {

    static final String JDBC_DRIVER = "com.mysql.jdbc.Driver";
    static final String DB_URL = "jdbc:mysql://localhost/things";
    static final String USER = "kalistrat";
    static final String PASS = "045813";

    //public static List<SubscriberLogger> SubscriberLoggerList = new ArrayList<SubscriberLogger>();

    public static Document loadXMLFromString(String xml) throws Exception
    {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        InputSource is = new InputSource(new StringReader(xml));
        return builder.parse(is);
    }

    public static int getSubsriberIndexByName(String sName){
        int indx = -1;
        for (SubscriberLogger iObj : Main.SubscriberLoggerList) {
            if (iObj.TopicName.equals(sName)) {
                indx = Main.SubscriberLoggerList.indexOf(iObj);
            }
        }
        return indx;
    }

    public static String getCurrentDir(){
        String path=System.getProperty("java.class.path");
        String FileSeparator=(String)System.getProperty("file.separator");
        return path.substring(0, path.lastIndexOf(FileSeparator)+1);
    }

    public static void logAction(String strLog){
        try {
            String filename = Main.AbsPath + "SubscriberServer.log";
            FileWriter fw = new FileWriter(filename, true); //the true will append the new data
            fw.write((new SimpleDateFormat("dd/MM/YYYY HH:mm:ss")).format(new Date()) + " : " + strLog +"\n");//appends the string to the file
            fw.close();
        }  catch (Exception e){
        e.printStackTrace();
        }
    }

    public static int getItransitionConditionIndexByName(String sName){
        int indx = -1;
        for (DtransitionCondition iObj : Main.DtransitionConditionList) {
            if (iObj.ReadTopicName.equals(sName)) {
                indx = Main.DtransitionConditionList.indexOf(iObj);
            }
        }
        return indx;
    }

    public static int getPublisherTaskIndexByName(String sName){
        int indx = -1;
        for (PublisherTask iObj : Main.PublisherTaskList) {
            if (iObj.iWriteTopicName.equals(sName)) {
                indx = Main.PublisherTaskList.indexOf(iObj);
            }
        }
        return indx;
    }

    public static Double StrToDouble(String StrValue){
        try {
            //System.out.println(Sval);
            return Double.parseDouble(StrValue.replace(",","."));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static Integer StrToIntValue(String Sval) {

        try {
            //System.out.println(Sval);
            return Integer.parseInt(Sval);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static java.util.Date redefineSyncDate(String TimeZone){
        int addHours = StrToIntValue(TimeZone.replace("UTC",""));
        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date());
        cal.add(Calendar.HOUR, -3);
        cal.add(Calendar.HOUR, addHours);
        return cal.getTime();
    }

    public static List<String> GetListFromString(String DevidedString){
        List<String> StrPieces = new ArrayList<String>();
        int k = 0;
        String iDevidedString = DevidedString;

        //System.out.println("Последний символ: " + DevidedString.substring(DevidedString.length()-1, DevidedString.length()));

        if ((DevidedString.indexOf("/") != -1)&&
                (DevidedString.substring(DevidedString.length()-1, DevidedString.length()).equals("/"))) {

            while (!iDevidedString.equals("")) {
                int Pos = iDevidedString.indexOf("/");
                //System.out.println(Pos);
                StrPieces.add(iDevidedString.substring(0, Pos));
                iDevidedString = iDevidedString.substring(Pos + 1);
                k = k + 1;
                if (k > 100) {
                    iDevidedString = "";
                }
            }
        }

        return StrPieces;
    }

    public static List<String> GetListFromStringDevider(String DevidedString,String Devider){
        List<String> StrPieces = new ArrayList<String>();
        int k = 0;
        String iDevidedString = DevidedString;

        while (!iDevidedString.equals("")) {
            int Pos = iDevidedString.indexOf(Devider);
            StrPieces.add(iDevidedString.substring(0, Pos));
            iDevidedString = iDevidedString.substring(Pos + 1);
            k = k + 1;
            if (k > 100000) {
                iDevidedString = "";
            }
        }

        return StrPieces;
    }

    public static String ExecuteMessage(String eClientData){

        try {

            String OutMessage = "";
            System.out.println("Принятое сообщение : " + eClientData);
            List<String> MessageList = MessageHandling.GetListFromString(eClientData);

            //System.out.println("MessageList.size :" + MessageList.size());

            if (MessageList.size() == 4) {

                String ActionType = MessageList.get(0);
                String UserLog = MessageList.get(1);
                String SubcriberType = MessageList.get(2);
                String EntityId = MessageList.get(3);

//                System.out.println("ActionType :" + ActionType);
//                System.out.println("UserLog :" + UserLog);
//                System.out.println("SubcriberType :" + SubcriberType);
//                System.out.println("SubscriberId :" + SubscriberId);


                if (!(
                        ActionType.equals("add")
                                || ActionType.equals("delete")
                )) {
                    OutMessage = "Неизвестный тип операции;";
                }

                if (!(
                        SubcriberType.equals("d_transion")
                                || SubcriberType.equals("task")
                                || SubcriberType.equals("server")
                )) {
                    OutMessage = OutMessage + "Неизвестный тип подписчика;";
                }

                if (OutMessage.equals("")) {

                    if (SubcriberType.equals("i_transion")) {
                        OutMessage = "добавление\\удаление независимых переходов не поддерживается;";
                    }

                    if (SubcriberType.equals("d_transion")) {
                        OutMessage = dTransitionListUpdate(ActionType,UserLog,EntityId);
                    }

                    if (SubcriberType.equals("task")) {
                        OutMessage = dTaskListUpdate(ActionType,UserLog,EntityId);
                    }

                    if (SubcriberType.equals("server")) {
                        OutMessage = "добавление\\удаление независимых переходов не поддерживается;";
                    }

                    //itransitionListUpdate
                    //dtransitionListUpdate
                } else {
                    OutMessage = "N|" + OutMessage + "|";
                }

            } else {
                OutMessage = "N|Неподдерживаемый тип сообщения;|";
            }

            System.out.println("Отправленое сообщение :" + OutMessage);

            return OutMessage;
        } catch (Exception eMessageHandling){
            eMessageHandling.printStackTrace();
            return "N|Ошибка обработчика сообщения;|";
        }
    }

//    public static String sensorListUpdate(
//            String qActionType
//            ,String qUserLog
//            ,String qDeviceId
//    ){
//        String outMess;
//        String TopicName = "";
//        String MqttServerHost = "";
//        String DeviceLog = "";
//        String DevicePass = "";
//        String MesDataType = "";
//        int iDeviceId = Integer.parseInt(qDeviceId);
//
//        try {
//
//            Class.forName(JDBC_DRIVER);
//            Connection Con = DriverManager.getConnection(
//                    DB_URL
//                    , USER
//                    , PASS
//            );
//
//            CallableStatement Stmt = Con.prepareCall("{call s_p_sensor_initial(?, ?, ?, ?, ?, ?, ?)}");
//            Stmt.setString(1, qUserLog);
//            Stmt.setInt(2, iDeviceId);
//            Stmt.registerOutParameter(3, Types.VARCHAR);
//            Stmt.registerOutParameter(4, Types.VARCHAR);
//            Stmt.registerOutParameter(5, Types.VARCHAR);
//            Stmt.registerOutParameter(6, Types.VARCHAR);
//            Stmt.registerOutParameter(7, Types.VARCHAR);
//
//            Stmt.execute();
//
//            TopicName = Stmt.getString(3);
//            MqttServerHost = Stmt.getString(4);
//            DeviceLog = Stmt.getString(5);
//            DevicePass = Stmt.getString(6);
//            MesDataType = Stmt.getString(7);
//
//            Con.close();
//
//        }catch(SQLException se){
//            //Handle errors for JDBC
//            se.printStackTrace();
//        }catch(Exception e) {
//            //Handle errors for Class.forName
//            e.printStackTrace();
//        }
//
//        if (TopicName == null) {
//            TopicName = "";
//        }
//
//        if (!TopicName.equals("")) {
//
//            int SubscriberIndx = MessageHandling.getSubsriberIndexByName(TopicName);
//            try {
//                if (qActionType.equals("add")) {
//                    if (SubscriberIndx == -1) {
//                        Main.SubscriberLoggerList.add(new SubscriberLogger(TopicName,MqttServerHost,DeviceLog,DevicePass,MesDataType));
//                        outMess = "Y|"+"Подписчик " + TopicName + " успешно добавлен" + "|";
//                    } else {
//                        outMess = "N|"+"Подписчик " + TopicName + " уже добавлен" + "|";
//                    }
//
//                } else {
//                    if (SubscriberIndx != -1) {
//                        SubscriberLogger s1 = Main.SubscriberLoggerList.get(SubscriberIndx);
//                        s1.client.disconnect();
//                        Main.SubscriberLoggerList.remove(SubscriberIndx);
//                        s1 = null;
//                        System.gc();
//                        outMess = "Y|"+"Подписчик " + TopicName + " успешно удалён" + "|";
//                    } else {
//                        outMess = "N|"+"Подписчик " + TopicName + " не найден" + "|";
//                    }
//                }
//            } catch (Throwable e) {
//                outMess = "N|Ошибка подключения к mqtt-серверу;|";
//            }
//        } else {
//            outMess = "N|Ошибка инициализации устройства из базы данных;|";
//        }
//
//        return outMess;
//    }

//    public static void topicDataLog(String qTopicName
//                                    ,java.sql.Timestamp qMessDate
//            ,String StringValue
//            ,Double doubleValue
//                                    ,java.sql.Timestamp qMeasDateValue
//    ){
//        try {
//
//            Class.forName(JDBC_DRIVER);
//            Connection Con = DriverManager.getConnection(
//                    DB_URL
//                    , USER
//                    , PASS
//            );
//
//            CallableStatement Stmt = Con.prepareCall("{call s_p_topic_data_log(?, ?, ?, ?, ?)}");
//            Stmt.setString(1, qTopicName);
//            Stmt.setTimestamp(2, qMessDate);
//            Stmt.setString(3,StringValue);
//            if (doubleValue != null) {
//                Stmt.setDouble(4, doubleValue);
//            } else {
//                Stmt.setNull(4, Types.DECIMAL);
//            }
//
//            if (qMeasDateValue != null) {
//                Stmt.setTimestamp(5, qMeasDateValue);
//            } else {
//                Stmt.setNull(5, Types.TIMESTAMP);
//            }
//            Stmt.execute();
//
//            Con.close();
//            MessageHandling.logAction("Сообщение " + StringValue + " записано в базу данных");
//
//
//        }catch(SQLException se){
//            //Handle errors for JDBC
//            se.printStackTrace();
//        }catch(Exception e) {
//            //Handle errors for Class.forName
//            e.printStackTrace();
//        }
//
//    }

//    public static void createSubscriberLoggerList(){
//        try {
//            Class.forName(JDBC_DRIVER);
//            Connection Con = DriverManager.getConnection(
//                    DB_URL
//                    , USER
//                    , PASS
//            );
//
//            String DataSql = "select 'add'\n" +
//                    ",u.user_log\n" +
//                    ",ud.user_device_id\n" +
//                    "from user_device ud\n" +
//                    "join action_type aty on aty.action_type_id=ud.action_type_id\n" +
//                    "join users u on u.user_id=ud.user_id\n" +
//                    "where aty.action_type_id = 1";
//
//            PreparedStatement DataStmt = Con.prepareStatement(DataSql);
//
//
//            ResultSet DataRs = DataStmt.executeQuery();
//
//            while (DataRs.next()) {
//
//                System.out.println(
//
//                        DataRs.getString(1)
//                        +DataRs.getString(2)
//                        +String.valueOf(DataRs.getInt(3))
//                        + " : "
//                        + sensorListUpdate(DataRs.getString(1),DataRs.getString(2),String.valueOf(DataRs.getInt(3)))
//                );
//            }
//
//
//            Con.close();
//
//        } catch (SQLException se3) {
//            //Handle errors for JDBC
//            se3.printStackTrace();
//        } catch (Exception e13) {
//            //Handle errors for Class.forName
//            e13.printStackTrace();
//        }
//    }

    public static String dTransitionListUpdate(
            String qActionType
            ,String qUserLog
            ,String qConditionId
    ){

        String outMess;
        internalMqttServer changeServer = null;
        for (internalMqttServer iServ: Main.mqttServersList) {
            if (iServ.iUserLog.equals(qUserLog)){
                changeServer = iServ;
            }
        }

        try {
            if (qActionType.equals("add")) {
                if (changeServer != null) {
                    if (changeServer.addServerCondition(qConditionId)) {
                        outMess = "Y|"+"Условие " + qConditionId + " успешно добавлено" + "|";
                    } else {
                        outMess = "Y|"+"Условие " + qConditionId + " уже добавлено" + "|";
                    }
                } else {
                    outMess = "N|"+"Условие " + qConditionId + " не добавлено. Сервера не существует" + "|";
                }

            } else {
                if (changeServer.deleteServerCondition(qConditionId)) {
                    outMess = "Y|"+"Условие " + qConditionId + " успешно удалёно" + "|";
                } else {
                    outMess = "N|"+"Условие " + qConditionId + " не найдено" + "|";
                }
            }
        } catch (Throwable e) {
            outMess = "N|Ошибка подключения к mqtt-серверу;|";
        }

        return outMess;
    }

    public static String dTaskListUpdate(
            String qActionType
            ,String qUserLog
            ,String qTaskId
    ){
        String iTaskTypeName = "";
        int iTaskInterval=0;
        String iIntervalType = "";
        String iWriteTopicName = null;
        String iServerIp = "";
        String iControlLog = "";
        String iControlPass = "";
        String iMessageValue = "";
        String outMess;

        int iTaskId = Integer.parseInt(qTaskId);

        try {

            Class.forName(JDBC_DRIVER);
            Connection Con = DriverManager.getConnection(
                    DB_URL
                    , USER
                    , PASS
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
                    "join task_type tt on tt.task_type_id=tas.task_type_id\n" +
                    "left join user_devices_tree udtr on udtr.user_device_id=tas.user_device_id\n" +
                    "left join mqtt_servers ms on ms.server_id=udtr.mqtt_server_id\n" +
                    "left join timezones tz on tz.timezone_id=udtr.timezone_id\n" +
                    "where tas.user_device_task_id = ?";

            PreparedStatement DataStmt = Con.prepareStatement(DataSql);
            DataStmt.setInt(1, iTaskId);


            ResultSet DataRs = DataStmt.executeQuery();

            while (DataRs.next()) {
                iTaskTypeName = DataRs.getString(1);
                iTaskInterval = DataRs.getInt(2);
                iIntervalType = DataRs.getString(3);
                iWriteTopicName = DataRs.getString(4);
                iServerIp = DataRs.getString(5);
                iControlLog = DataRs.getString(6);
                iControlPass = DataRs.getString(7);
                iMessageValue = DataRs.getString(8);
            }

            Con.close();

        }catch(SQLException se){
            //Handle errors for JDBC
            se.printStackTrace();
        }catch(Exception e) {
            //Handle errors for Class.forName
            e.printStackTrace();
        }

        if (iWriteTopicName == null) {
            iWriteTopicName = "";
        }

        if (!iWriteTopicName.equals("")) {

            internalMqttServer changeServer = null;
                    for (internalMqttServer iServ: Main.mqttServersList) {
                        if (iServ.iUserLog.equals(qUserLog)){
                            changeServer = iServ;
                        }
                    }

            try {
                if (qActionType.equals("add")) {
                    if (changeServer != null) {
                        if (changeServer.addServerTask(
                                iTaskTypeName
                                ,iTaskInterval
                                ,iIntervalType
                                ,iWriteTopicName
                                ,iServerIp
                                ,iControlLog
                                ,iControlPass
                                ,iMessageValue
                        )) {
                            outMess = "Y|"+"Задание " + iWriteTopicName + " успешно добавлено" + "|";
                        } else {
                            outMess = "Y|"+"Задание " + iWriteTopicName + " уже добавлено" + "|";
                        }
                    } else {
                        outMess = "N|"+"Задание " + iWriteTopicName + " не добавлено. Сервера не существует" + "|";
                    }

                } else {
                    if (changeServer.deleteServerTask(iWriteTopicName)) {
                        outMess = "Y|"+"Задание " + iWriteTopicName + " успешно удалёно" + "|";
                    } else {
                        outMess = "N|"+"Задание " + iWriteTopicName + " не найдено" + "|";
                    }
                }
            } catch (Throwable e) {
                outMess = "N|Ошибка подключения к mqtt-серверу;|";
            }
        } else {
            outMess = "N|Ошибка инициализации задания из базы данных;|";
        }

        return outMess;
    }

//    public static void createPublisherTaskList(){
//        try {
//            Class.forName(JDBC_DRIVER);
//            Connection Con = DriverManager.getConnection(
//                    DB_URL
//                    , USER
//                    , PASS
//            );
//
//            String DataSql = "select 'add' act_type\n" +
//                    ",u.user_log\n" +
//                    ",tas.user_device_task_id\n" +
//                    "from user_device_task tas\n" +
//                    "join user_device ud on ud.user_device_id=tas.user_device_id\n" +
//                    "join users u on u.user_id=ud.user_id";
//
//            PreparedStatement DataStmt = Con.prepareStatement(DataSql);
//
//
//            ResultSet DataRs = DataStmt.executeQuery();
//
//            while (DataRs.next()) {
//
//                System.out.println(
//
//                        DataRs.getString(1)
//                                +DataRs.getString(2)
//                                +String.valueOf(DataRs.getInt(3))
//                                + " : "
//                                + dTaskListUpdate(DataRs.getString(1),DataRs.getString(2),String.valueOf(DataRs.getInt(3)))
//                );
//            }
//
//
//            Con.close();
//
//        } catch (SQLException se3) {
//            //Handle errors for JDBC
//            se3.printStackTrace();
//        } catch (Exception e13) {
//            //Handle errors for Class.forName
//            e13.printStackTrace();
//        }
//    }


//    public static void changeControllerPassWord(
//            String ControlName
//            , String ControlPassSha
//            , String NewControlName
//            , String NewControlPassSha
//    ){
//        try {
//            String filename = Main.AbsPath + "/password_file.conf";
//            Path path = Paths.get(filename);
//            Charset charset = StandardCharsets.UTF_8;
//            String content = new String(Files.readAllBytes(path), charset);
//            content = content.replaceAll(ControlName + ":" + ControlPassSha
//                    ,NewControlName + ":" + NewControlPassSha);
//            Files.write(path, content.getBytes(charset));
//        } catch (IOException e) {
//            //Simple exception handling, replace with what's necessary for your use case!
//            e.printStackTrace();
//        }
//    }


//    public static void rebootMqttServer() throws InterruptedException, IOException, MqttException, Throwable {
//
//        //Stoping Subscribers
//        for (SubscriberLogger iLog : Main.SubscriberLoggerList) {
//            SubscriberLogger delLog = iLog;
//            delLog.client.disconnect();
//            delLog = null;
//        }
//        Main.SubscriberLoggerList.clear();
//
//        //Stoping Rules
//        for (DtransitionCondition iRule : Main.DtransitionConditionList) {
//            DtransitionCondition delRule = iRule;
//            delRule.disconnectVarList();
//            delRule = null;
//        }
//        Main.DtransitionConditionList.clear();
//
//        //Stoping Rules
//        for (PublisherTask iTask : Main.PublisherTaskList) {
//            PublisherTask delTask = iTask;
//            delTask = null;
//        }
//        Main.PublisherTaskList.clear();
//        Main.iServ.stopServer();
//        System.gc();
//
//        Main.iServ.startServer(Main.iServ.configProps);
//        for (int i=0; i<Main.iServ.userHandlers.size(); i++) {
//            Main.iServ.addInterceptHandler(Main.iServ.userHandlers.get(0));
//        }
//        Thread.sleep(2000);
//        System.out.println("Сервер перезагружен...");
//        System.out.println("Создание подписчиков для датчиков...");
//        MessageHandling.createSubscriberLoggerList();
//        System.out.println("Создание заданий...");
//        MessageHandling.createPublisherTaskList();
//        System.out.println("Подписка завершена");
//
//    }

//    public static String folderListUpdate(
//            String qActionType
//            ,String qUserLog
//            ,String qUserLeafId
//    ){
//        String outMess;
//        int iLeafId = Integer.parseInt(qUserLeafId);
//        String ControlLogin = "";
//        String ControlPassSha = "";
//
//        try {
//
//            Class.forName(JDBC_DRIVER);
//            Connection Con = DriverManager.getConnection(
//                    DB_URL
//                    , USER
//                    , PASS
//            );
//
//            String DataSql = "select udt.control_log\n" +
//                    ",udt.control_pass_sha\n" +
//                    "from user_devices_tree udt\n" +
//                    "join users u on u.user_id=udt.user_id\n" +
//                    "where u.user_log=?\n" +
//                    "and udt.leaf_id=?";
//
//            PreparedStatement DataStmt = Con.prepareStatement(DataSql);
//            DataStmt.setString(1,qUserLog);
//            DataStmt.setInt(2,iLeafId);
//
//            ResultSet DataRs = DataStmt.executeQuery();
//
//            while (DataRs.next()) {
//                ControlLogin = DataRs.getString(1);
//                ControlPassSha = DataRs.getString(2);
//            }
//
//            Con.close();
//
//        }catch(SQLException se){
//            //Handle errors for JDBC
//            se.printStackTrace();
//        }catch(Exception e) {
//            //Handle errors for Class.forName
//            e.printStackTrace();
//        }
//
//        if (ControlLogin == null) {
//            ControlLogin = "";
//        }
//
//        if (!ControlLogin.equals("")) {
//
//            try {
//                if (qActionType.equals("add")) {
//
//                    addControllerPassWord(ControlLogin,ControlPassSha);
//                    rebootMqttServer();
//                    outMess = "Y|"+"Контроллер " + ControlLogin + " успешно добавлен" + "|";
//
//                } else {
//
//                    deleteControllerPassWord(ControlLogin,ControlPassSha);
//                    rebootMqttServer();
//                    outMess = "Y|"+"Контроллер " + ControlLogin + " успешно удалён" + "|";
//
//                }
//            } catch (Throwable e) {
//                e.printStackTrace();
//                outMess = "N|Ошибка подключения к mqtt-серверу;|";
//            }
//        } else {
//            outMess = "N|Ошибка инициализации устройства из базы данных;|";
//        }
//
//        return outMess;
//    }


    public static SSLSocketFactory configureSSLSocketFactory() throws KeyManagementException, NoSuchAlgorithmException,
            UnrecoverableKeyException, IOException, CertificateException, KeyStoreException {
        KeyStore ks = KeyStore.getInstance("JKS");
        InputStream jksInputStream = new FileInputStream(Main.AbsPath + "clientkeystore.jks");
        ks.load(jksInputStream, "3Point".toCharArray());

        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(ks, "3Point".toCharArray());

        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(ks);

        SSLContext sc = SSLContext.getInstance("TLS");
        TrustManager[] trustManagers = tmf.getTrustManagers();
        sc.init(kmf.getKeyManagers(), trustManagers, null);

        SSLSocketFactory ssf = sc.getSocketFactory();
        return ssf;
    }
}
