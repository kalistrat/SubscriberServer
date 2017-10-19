package mqttch;


import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import javax.net.ssl.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;
import java.security.*;
import java.security.cert.CertificateException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Date;
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
                                || ActionType.equals("change")
                )) {
                    OutMessage = "Неизвестный тип операции;";
                }

                if (!(
                        SubcriberType.equals("d_transion")
                                || SubcriberType.equals("task")
                                || SubcriberType.equals("server")
                                || SubcriberType.equals("folder")

                )) {
                    OutMessage = OutMessage + "Неизвестный тип подписчика;";
                }

                if (OutMessage.equals("")) {


                    if (SubcriberType.equals("d_transion")) {

                        OutMessage = dTransitionListUpdate(ActionType,UserLog,EntityId);

                    } else if (SubcriberType.equals("task")) {

                        OutMessage = dTaskListUpdate(ActionType,UserLog,EntityId);

                    } else if (SubcriberType.equals("server")) {

                        OutMessage = dServerListUpdate(ActionType,UserLog);

                    } else if (SubcriberType.equals("folder")) {

                        OutMessage = "добавление\\удаление независимых переходов не поддерживается;";

                    } else {

                        OutMessage = "Неподдерживаемый тип подписчика;";

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

            } else {
                if (changeServer.deleteServerTask(qTaskId)) {
                    outMess = "Y|"+"Задание " + qTaskId + " успешно удалёно" + "|";
                } else {
                    outMess = "N|"+"Задание " + qTaskId + " не найдено" + "|";
                }
            }
        } catch (Throwable e) {
            outMess = "N|Ошибка подключения к mqtt-серверу;|";
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
                    outMess = "N|"+"Сервер для" + qUserLog + " успешно добавлен" + "|";
                } else {
                    outMess = "N|"+"Сервер для" + qUserLog + " уже существует" + "|";
                }
            } else if (qActionType.equals("change") && changeServer != null) {
                changeServer.rebootMqttServer();
                outMess = "N|"+"Сервер для" + qUserLog + " перезагружен" + "|";
            } else {
                outMess = "N|"+"Неподдерживаемый тип операции для сервера;|";
            }
        } catch (Throwable e) {
            outMess = "N|Ошибка подключения к mqtt-серверу;|";
        }

        return outMess;
    }

    public static String dFolderListUpdate(
            String qActionType
            ,String qUserLog
            ,String qMessage
    ){
        String outMess;
        internalMqttServer changeServer = null;
        for (internalMqttServer iServ: Main.mqttServersList) {
            if (iServ.iUserLog.equals(qUserLog)){
                changeServer = iServ;
            }
        }

        Matcher insideBrackets = Pattern.compile("\\((.*?)\\)").matcher(qMessage);
        Matcher outsideBrackets = Pattern.compile("(.*)\\(.*?\\)").matcher(qMessage);
        insideBrackets.find();
        outsideBrackets.find();
        String folderLogIn = insideBrackets.group(1);
        String folderPassWord = outsideBrackets.group(1);

        try {
            if (qActionType.equals("add")) {
                if (changeServer != null && folderLogIn != null && folderPassWord != null) {
                    changeServer.addControllerPassWord(folderLogIn,folderPassWord);
                    outMess = "N|"+"Новый контроллер" + folderLogIn + "для" + qUserLog + " успешно добавлен" + "|";
                } else {
                    outMess = "N|"+"Не определён сервер или логин и пароль для контроллера" + qUserLog + "|";
                }
            } else if (qActionType.equals("change") && changeServer != null) {
                changeServer.rebootMqttServer();
                outMess = "N|"+"Пароль изменён и mqtt-сервер для" + qUserLog + " перезагружен" + "|";
            } else {
                outMess = "N|"+"Неподдерживаемый тип операции для сервера;|";
            }
        } catch (Throwable e) {
            outMess = "N|Ошибка подключения к mqtt-серверу;|";
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
}
