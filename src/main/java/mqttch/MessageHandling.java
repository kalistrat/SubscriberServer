package mqttch;


import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import javax.net.ssl.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.*;
import java.security.*;
import java.security.cert.CertificateException;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Date;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by kalistrat on 02.05.2017.
 */
public class MessageHandling {

    static final String JDBC_DRIVER = "com.mysql.jdbc.Driver";
    static final String DB_URL = "jdbc:mysql://localhost/things";
    static final String USER = "kalistrat";
    static final String PASS = "045813";

    public static Document loadXMLFromString(String xml) throws Exception
    {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        InputSource is = new InputSource(new StringReader(xml));
        return builder.parse(is);
    }

    public static int safeLongToInt(long l) {
        if (l < Integer.MIN_VALUE || l > Integer.MAX_VALUE) {
            throw new IllegalArgumentException
                    (l + " cannot be cast to int without changing its value.");
        }
        return (int) l;
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

    public static List<String> GetListFromStringDevider(String DevidedString, String Devider){
        List<String> StrPieces = new ArrayList<String>();
        try {
            int k = 0;
            String iDevidedString;
            // 123|321|456|

            if (DevidedString.startsWith(Devider)) {
                DevidedString = DevidedString.substring(1,DevidedString.length());
            }

            if (!DevidedString.contains(Devider)) {
                iDevidedString = DevidedString + Devider;
            } else {
                if (!DevidedString.endsWith(Devider)) {
                    iDevidedString = DevidedString + Devider;
                } else {
                    iDevidedString = DevidedString;
                }
            }

            while (!iDevidedString.equals("")) {
                int Pos = iDevidedString.indexOf(Devider);
                StrPieces.add(iDevidedString.substring(0, Pos));
                iDevidedString = iDevidedString.substring(Pos + 1);
                k = k + 1;
                if (k > 100000) {
                    iDevidedString = "";
                }
            }

        } catch (Exception e){
            e.printStackTrace();
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


                if (!(
                        ActionType.equals("add")
                                || ActionType.equals("delete")
                                || ActionType.equals("change")
                                || ActionType.equals("call")
                )) {
                    OutMessage = "Неизвестный тип операции;";
                }

                if (!(
                        SubcriberType.equals("state")
                                || SubcriberType.equals("task")
                                || SubcriberType.equals("server")
                                || SubcriberType.equals("state_message")
                                || SubcriberType.equals("drop")
                                || SubcriberType.equals("sync")

                )) {
                    OutMessage = OutMessage + "Неизвестный тип подписчика;";
                }

                if (OutMessage.equals("")) {
                    if (SubcriberType.equals("state")) {
                        OutMessage = deviceStateListUpdate(ActionType,UserLog,EntityId);
                    } else if (SubcriberType.equals("task")) {
                        OutMessage = dTaskListUpdate(ActionType,UserLog,EntityId);
                    } else if (SubcriberType.equals("server")) {
                        OutMessage = dServerListUpdate(ActionType,UserLog);
                    } else if (SubcriberType.equals("state_message")) {
                        OutMessage = mqqtMessagePublish(ActionType,UserLog,EntityId);
                    } else if (SubcriberType.equals("drop")) {
                        //OutMessage = sendMessageToDrop(ActionType,UserLog,EntityId);
                    } else if (SubcriberType.equals("sync")) {
                        //OutMessage = sendMessageToSync(ActionType,UserLog,EntityId);
                    } else {
                        OutMessage = "Неподдерживаемый тип подписчика;";
                    }
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

    public static String deviceStateListUpdate(
            String qActionType
            ,String qUserLog
            ,String qStateId
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
                    if (changeServer.addServerDeviceState(qStateId)) {
                        outMess = "Y|"+"Состояние " + qStateId + " успешно добавлено" + "|";
                    } else {
                        outMess = "Y|"+"Состояние " + qStateId + " уже добавлено" + "|";
                    }
                } else {
                    outMess = "N|"+"Состояние " + qStateId + " не добавлено. Сервера не существует" + "|";
                }

            } else if (qActionType.equals("delete")) {
                if (changeServer.deleteServerDeviceState(qStateId)) {
                    outMess = "Y|"+"Состояние " + qStateId + " успешно удалёно" + "|";
                } else {
                    outMess = "N|"+"Состояние " + qStateId + " не найдено" + "|";
                }
            } else if (qActionType.equals("change")) {
                if (changeServer.changeServerDeviceState(qStateId)) {
                    outMess = "Y|"+"Состояние " + qStateId + " успешно изменено" + "|";
                } else {
                    outMess = "N|"+"Состояние " + qStateId + " не найдено" + "|";
                }
            } else {
                outMess = "N|"+"Неподдерживаемый тип операции для состояния" + "|";
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
                    if (changeServer.addServerTask(qTaskId)) {
                        outMess = "Y|"+"Задание " + qTaskId + " успешно добавлено" + "|";
                    } else {
                        outMess = "Y|"+"Задание " + qTaskId + " уже добавлено" + "|";
                    }
                } else {
                    outMess = "N|"+"Задание " + qTaskId + " не добавлено. Сервера не существует" + "|";
                }

            } else if (qActionType.equals("delete")) {

                if (changeServer.deleteServerTask(qTaskId)) {
                    outMess = "Y|"+"Задание " + qTaskId + " успешно удалёно" + "|";
                } else {
                    outMess = "N|"+"Задание " + qTaskId + " не найдено" + "|";
                }

            } else {
                outMess = "N|"+"Непподерживаемый тип операций для заданий" + "|";
            }
        } catch (Throwable e) {
            outMess = "N|Ошибка выполнения операции на mqtt-сервере;|";
        }

        return outMess;
    }

    public static String mqqtMessagePublish(
            String qActionType
            ,String qUserLog
            ,String qStateMessageId
    ) {
        String outMess;
        try {


        Document xmlDocument = MessageHandling
                .loadXMLFromString(getStateMessageData(Integer.parseInt(qStateMessageId)));

        String controlLog = XPathFactory.newInstance().newXPath()
                .compile("/device_message_data/control_log").evaluate(xmlDocument);
        String controlPass = XPathFactory.newInstance().newXPath()
                .compile("/device_message_data/control_pass").evaluate(xmlDocument);
        String topicName = XPathFactory.newInstance().newXPath()
                .compile("/device_message_data/mqtt_topic_write").evaluate(xmlDocument);
        String serverIp = XPathFactory.newInstance().newXPath()
                .compile("/device_message_data/server_ip").evaluate(xmlDocument);
        String messageCode = XPathFactory.newInstance().newXPath()
                .compile("/device_message_data/actuator_message_code").evaluate(xmlDocument);



            if (qActionType.equals("add")) {

                if (messageCode!=null) {
                    if (!messageCode.equals("")){
                        MessageHandling.publishMqttMessage(
                                topicName
                                ,serverIp
                                ,controlLog
                                ,controlPass
                                ,messageCode
                        );
                        outMess = "Y|"+"Сообщение состояния актуатора " + qStateMessageId + " успешно опубликовано" + "|";
                    } else {
                        outMess = "N|"+"Ошибка инициализации состояния актуатора " + qStateMessageId + "|";
                    }
                } else {
                    outMess = "N|"+"Ошибка инициализации состояния актуатора " + qStateMessageId + "|";
                }

            } else {
                outMess = "N|"+"Непподерживаемый тип операций для сообщений" + "|";
            }
        } catch (Throwable e) {
            outMess = "N|Ошибка выполнения операции на mqtt-сервере;|";
        }

        return outMess;
    }

    public static String dServerListUpdate(
            String qActionType
            ,String qUserLog
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
                if (changeServer == null) {
                    Main.mqttServersList.add(new internalMqttServer(qUserLog));
                    outMess = "N|"+"Сервер для " + qUserLog + " успешно добавлен" + "|";
                } else {
                    outMess = "N|"+"Сервер для " + qUserLog + " уже существует" + "|";
                }
            } else if (qActionType.equals("change") && changeServer != null) {
                changeServer.rebootMqttServer();
                outMess = "N|"+"Сервер для " + qUserLog + " перезагружен" + "|";
            } else {
                outMess = "N|"+"Неподдерживаемый тип операции для сервера;|";
            }
        } catch (Throwable e) {
            e.printStackTrace();
            outMess = "N|Ошибка выполнения операции на mqtt-сервере;|";
        }

        return outMess;
    }


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

    public static String getUsersList(){
        try {

            Class.forName(MessageHandling.JDBC_DRIVER);
            Connection Con = DriverManager.getConnection(
                    MessageHandling.DB_URL
                    , MessageHandling.USER
                    , MessageHandling.PASS
            );

            CallableStatement Stmt = Con.prepareCall("{? = call s_get_user_list()}");
            Stmt.registerOutParameter(1, Types.BLOB);
            Stmt.execute();
            Blob CondValue = Stmt.getBlob(1);
            String resultStr = new String(CondValue.getBytes(1l, (int) CondValue.length()));
            System.out.println("getUsersList resultStr :" + resultStr);
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

    private static String getStateMessageData(int actuatorStateId){
        try {

            Class.forName(MessageHandling.JDBC_DRIVER);
            Connection Con = DriverManager.getConnection(
                    MessageHandling.DB_URL
                    , MessageHandling.USER
                    , MessageHandling.PASS
            );

            CallableStatement Stmt = Con.prepareCall("{? = call s_get_state_message_data(?)}");
            Stmt.registerOutParameter(1, Types.BLOB);
            Stmt.setInt(2,actuatorStateId);
            Stmt.execute();
            Blob CondValue = Stmt.getBlob(1);
            String resultStr = new String(CondValue.getBytes(1l, (int) CondValue.length()));
            //System.out.println("getUsersList resultStr :" + resultStr);
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

    public static void createMqttServerList() throws Throwable {
        List<String> usersList = new ArrayList<>();
        Document xmlDocument = MessageHandling
                .loadXMLFromString(getUsersList());

        Node serverListNode = (Node) XPathFactory.newInstance().newXPath()
                .compile("/user_list").evaluate(xmlDocument, XPathConstants.NODE);

        NodeList nodeList = serverListNode.getChildNodes();

        for (int i=0; i<nodeList.getLength(); i++){
            NodeList childNodeList = nodeList.item(i).getChildNodes();
            for (int j=0; j<childNodeList.getLength();j++) {
                if (childNodeList.item(j).getNodeName().equals("user_log")) {
                    usersList.add(childNodeList.item(j).getTextContent());
                }
            }
        }

        for (String iObj : usersList) {
            internalMqttServer newServ = new internalMqttServer(iObj);
            newServ.setPublisherTaskList();
            newServ.setDeviceStateList();
            Main.mqttServersList.add(newServ);
        }
    }


    public static void publishMqttMessage(
            String wWriteTopicName
            ,String wServerIp
            ,String wControlLog
            ,String wControlPass
            ,String wMessageValue
    ) {
        try {

            //MqttClient client = new MqttClient(wServerIp, wControlLog + String.valueOf(((new Date()).getTime()) / 1000L), null);
            int randomNum = ThreadLocalRandom.current().nextInt(0, 1000 + 1);
            //System.out.println("randomNum : " + randomNum);
            MqttClient client = new MqttClient(wServerIp, MqttClient.generateClientId()+ String.valueOf(randomNum), null);

            MqttConnectOptions options = new MqttConnectOptions();
            options.setUserName(wControlLog);
            options.setPassword(wControlPass.toCharArray());
            if (wServerIp.contains("ssl://")) {
                SSLSocketFactory ssf = MessageHandling.configureSSLSocketFactory();
                options.setSocketFactory(ssf);
            }
            MqttMessage message = new MqttMessage(wMessageValue.getBytes());
            client.connect(options);
            client.publish(wWriteTopicName, message);
            client.disconnect();

        } catch(MqttException | GeneralSecurityException | IOException me) {
            //me.printStackTrace();
            System.out.println("publishMqttMessage EXCEPTION!!!!!!!!!!");
        }

    }

    public static void sendEmailMessage(
            String recipientEmail
            ,String message
    ){
        try {

            String from = "snslog@mail.ru";
            String subject = "Оповещение snslog.ru";
            List<String> recipients = new ArrayList<>();
            recipients.add(recipientEmail);
            SpringEmailService.send(from, recipients, subject, message, null, null, null);
        } catch (Exception e) {
            System.out.println("Ошибка отправки письма на :" + recipientEmail);
        }
    }

    public static void sendWhatsUpMessage(
            String recipientPhoneNum
            ,String message
    ){
        System.out.println("sending what's up message");
    }

    public static void sendSMSMessage(
            String recipientPhoneNum
            ,String message
    ){
        System.out.println("sending sms");
    }

    public static List getOverAllWseArgs(String UserLog){
        List Args = new ArrayList<String>();

        try {

            Class.forName(MessageHandling.JDBC_DRIVER);
            Connection Con = DriverManager.getConnection(
                    MessageHandling.DB_URL
                    , MessageHandling.USER
                    , MessageHandling.PASS
            );

            CallableStatement Stmt = Con.prepareCall("{call getOverAllWseArgs(?,?,?)}");
            Stmt.setString(1,UserLog);
            Stmt.registerOutParameter (2, Types.VARCHAR);
            Stmt.registerOutParameter (3, Types.VARCHAR);
            Stmt.execute();
            Args.add(Stmt.getString(2));
            Args.add(Stmt.getString(3));
            Con.close();

        }catch(SQLException se){
            //Handle errors for JDBC
            se.printStackTrace();
        }catch(Exception e) {
            //Handle errors for Class.forName
            e.printStackTrace();
        }

        return Args;
    }

    public static String getUnixTime(String timeZone){
        Date syncDate = MessageHandling.redefineSyncDate(timeZone);
        long unixSyncDate = syncDate.getTime() / 1000L;
        return String.valueOf(unixSyncDate);
    }

    public static List getMqttConnetionArgsUID(String UID){
        List Args = new ArrayList<String>();

        try {

            Class.forName(MessageHandling.JDBC_DRIVER);
            Connection Con = DriverManager.getConnection(
                    MessageHandling.DB_URL
                    , MessageHandling.USER
                    , MessageHandling.PASS
            );

            CallableStatement Stmt = Con.prepareCall("{call getMqttConnetionArgsUID(?,?,?,?,?,?,?)}");
            Stmt.setString(1,UID);
            Stmt.registerOutParameter (2, Types.VARCHAR);
            Stmt.registerOutParameter (3, Types.VARCHAR);
            Stmt.registerOutParameter (4, Types.VARCHAR);
            Stmt.registerOutParameter (5, Types.VARCHAR);
            Stmt.registerOutParameter (6, Types.VARCHAR);
            Stmt.registerOutParameter (7, Types.VARCHAR);

            Stmt.execute();
            Args.add(Stmt.getString(2));
            Args.add(Stmt.getString(3));
            Args.add(Stmt.getString(4));
            Args.add(Stmt.getString(5));
            Args.add(Stmt.getString(6));
            Args.add(Stmt.getString(7));

            Con.close();

        }catch(SQLException se){
            //Handle errors for JDBC
            se.printStackTrace();
        }catch(Exception e) {
            //Handle errors for Class.forName
            e.printStackTrace();
        }

        return Args;
    }


}
