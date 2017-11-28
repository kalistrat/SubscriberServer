package mqttch;

import io.moquette.interception.AbstractInterceptHandler;
import io.moquette.interception.InterceptHandler;
import io.moquette.interception.messages.InterceptPublishMessage;
import io.moquette.server.Server;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.*;

/**
 * Created by kalistrat on 27.09.2017.
 */
public class internalMqttServer extends Server {
    String passWordFilePath;
    List<actuatorState> actuatorStateList;
    List<PublisherTask> PublisherTaskList;
    String iUserLog;
    String RegularPort;
    String SecurePort;
    //List<serverTypePort> serverTypePorts;

    class serverTypePort{
        public String serverPortNum;
        public String serverPortType;

        serverTypePort(){
            serverPortNum = null;
            serverPortType = null;
        }
        public void setPortNum(String pNum){
            serverPortNum = pNum;
        }
        public void setPortType(String pType){
            serverPortType = pType;
        }

    }

    class serverFolderPassword{
        public String folderLogIn;
        public String folderPassWord;

        serverFolderPassword(){
            folderLogIn = null;
            folderPassWord = null;
        }
        public void setFolderLogIn(String sLog){
            folderLogIn = sLog;
        }
        public void setfolderPassWord(String sPass){
            folderPassWord = sPass;
        }

    }

    class PublisherListener extends AbstractInterceptHandler {

        @Override
        public String getID() {
            return "EmbeddedLauncherPublishListener";
        }

        @Override
        public void onPublish(InterceptPublishMessage msg) {

            System.out.println(
                    "Received on topic: " + msg.getTopicName().toString() + " content: " + StandardCharsets.UTF_8.decode(msg.getPayload().nioBuffer()).toString());

            String topic = msg.getTopicName().toString();
            String message = StandardCharsets.UTF_8.decode(msg.getPayload().nioBuffer()).toString();

            if (message.length()<200) {
                addMessageIntoDB(topic,message,iUserLog);
            }


        }
    }

    List<? extends InterceptHandler> userHandlers;
    Properties configProps;

    public internalMqttServer(String UserLog) throws InterruptedException, IOException, Exception {

        configProps = new Properties();
        iUserLog = UserLog;
        PublisherTaskList = new ArrayList<>();
        actuatorStateList = new ArrayList<>();
        setServerPorts();

        passWordFilePath = Main.AbsPath +"passwords/"+iUserLog+".conf";
        File passFile = new File(passWordFilePath);
        passFile.createNewFile();

        System.out.println("passfilename : " + passWordFilePath);

        setPassWordFile();

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

    public void setDeviceStateList() throws Throwable {

        Document xmlDocument = MessageHandling
                .loadXMLFromString(getDeviceStateList());

        Node node = (Node) XPathFactory.newInstance().newXPath()
                .compile("/actuator_state_list").evaluate(xmlDocument, XPathConstants.NODE);

        NodeList nodeList = node.getChildNodes();

        for (int i=0; i<nodeList.getLength(); i++){
            this.actuatorStateList.add(new actuatorState(Integer.parseInt(nodeList.item(i).getTextContent())));
        }
    }

    public void setPublisherTaskList() throws Throwable {

        Document xmlDocument = MessageHandling
                .loadXMLFromString(getPublisherTaskList());

        Node node = (Node) XPathFactory.newInstance().newXPath()
                .compile("/user_device_task_list").evaluate(xmlDocument, XPathConstants.NODE);

        NodeList nodeList = node.getChildNodes();

        for (int i=0; i<nodeList.getLength(); i++){
            //System.out.println("nodeList.item(i).getTextContent() : " + Integer.parseInt(nodeList.item(i).getTextContent()));
            this.PublisherTaskList.add(new PublisherTask(Integer.parseInt(nodeList.item(i).getTextContent())));
        }
    }



    public boolean addServerTask(
            String qTaskId
    ) throws Throwable {

        int taskId = Integer.parseInt(qTaskId);
        int indx = -1;

        for (PublisherTask iObj : this.PublisherTaskList) {
            if (iObj.iTaskId.intValue() == taskId) {
                indx = this.PublisherTaskList.indexOf(iObj);
            }
        }

        if (indx == -1) {
            this.PublisherTaskList.add(new PublisherTask(taskId));
            return true;
        } else {
            return false;
        }

    }

    public boolean deleteServerTask(
            String qTaskId
    ) throws Throwable {

        int taskId = Integer.parseInt(qTaskId);
        int indx = -1;

        for (PublisherTask iObj : this.PublisherTaskList) {
            if (iObj.iTaskId.intValue() == taskId) {
                indx = this.PublisherTaskList.indexOf(iObj);
            }
        }

        if (indx != -1) {
            PublisherTask remTask = this.PublisherTaskList.get(indx);
            remTask.ses.shutdown();
            remTask = null;
            this.PublisherTaskList.remove(indx);
            System.gc();
            return true;
        } else {
            return false;
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

//    public void deleteControllerPassWord (
//            String ControlName
//            , String ControlPassSha
//    ){
//        try {
//
//            Charset charset = StandardCharsets.UTF_8;
//
//            String filename = passWordFilePath.replaceFirst("^/(.:/)", "$1");
//            System.out.println("filename.replaceFirst : " + filename);
//
//            byte[] encoded = Files.readAllBytes(Paths.get(filename));
//            String fileContent = new String(encoded, charset);
//
//
//            String trimFileContent = fileContent.replace("\n" + ControlName + ":" + ControlPassSha,"");
//            FileWriter fw = new FileWriter(filename);
//            fw.write(trimFileContent);
//            fw.close();
//
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }

    public void rebootMqttServer() throws InterruptedException, IOException, MqttException, Throwable {

        for (actuatorState iStateRules : this.actuatorStateList) {
            actuatorState delStateRule = iStateRules;
            iStateRules.resetState();
            delStateRule = null;
        }
        this.actuatorStateList.clear();

        for (PublisherTask iTask : this.PublisherTaskList) {
            PublisherTask delTask = iTask;
            delTask.ses.shutdown();
            delTask = null;
        }
        this.PublisherTaskList.clear();
        stopServer();
        System.gc();
        setPassWordFile();

        startServer(configProps);

        for (int i=0; i<this.userHandlers.size(); i++) {
            this.addInterceptHandler(this.userHandlers.get(0));
        }
        setPublisherTaskList();
        setDeviceStateList();

    }

    private void addMessageIntoDB(String qTopicName
        ,String qMessAge
        ,String QUserLog
    ){
        try {

            Class.forName(MessageHandling.JDBC_DRIVER);
            Connection Con = DriverManager.getConnection(
                    MessageHandling.DB_URL
                    , MessageHandling.USER
                    , MessageHandling.PASS
            );

            CallableStatement Stmt = Con.prepareCall("{call s_message_recerve(?, ?, ?)}");
            Stmt.setString(1, qTopicName);
            Stmt.setString(2, qMessAge);
            Stmt.setString(3, QUserLog);

            Stmt.execute();
            Con.close();

        }catch(SQLException se){
            //Handle errors for JDBC
            se.printStackTrace();
        }catch(Exception e) {
            //Handle errors for Class.forName
            e.printStackTrace();
        }

    }

    public boolean addServerDeviceState(String qDeviceStateId) throws Throwable {

        int indx = -1;
        int loc_StateId = Integer.parseInt(qDeviceStateId);

        for (actuatorState iObj : this.actuatorStateList) {
            if (iObj.iStateId == loc_StateId) {
                indx = this.actuatorStateList.indexOf(iObj);
            }
        }

        if (indx == -1) {
            this.actuatorStateList.add(new actuatorState(loc_StateId));
            return true;
        } else {
            return false;
        }

    }


    private String getDeviceStateList(){
        try {

            Class.forName(MessageHandling.JDBC_DRIVER);
            Connection Con = DriverManager.getConnection(
                    MessageHandling.DB_URL
                    , MessageHandling.USER
                    , MessageHandling.PASS
            );

            CallableStatement Stmt = Con.prepareCall("{? = call s_get_user_state_list(?)}");
            Stmt.registerOutParameter(1,Types.BLOB);
            Stmt.setString(2, iUserLog);
            Stmt.execute();
            Blob CondValue = Stmt.getBlob(1);
            String resultStr;
            if (CondValue != null) {
                resultStr = new String(CondValue.getBytes(1l, (int) CondValue.length()));
            } else {
                resultStr = "<actuator_state_list/>";
            }
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

    private String getPublisherTaskList(){
        try {

            Class.forName(MessageHandling.JDBC_DRIVER);
            Connection Con = DriverManager.getConnection(
                    MessageHandling.DB_URL
                    , MessageHandling.USER
                    , MessageHandling.PASS
            );

            CallableStatement Stmt = Con.prepareCall("{? = call s_get_user_task_list(?)}");
            Stmt.registerOutParameter(1,Types.BLOB);
            Stmt.setString(2, iUserLog);
            Stmt.execute();
            Blob CondValue = Stmt.getBlob(1);
            String resultStr;
            if (CondValue != null) {
                resultStr = new String(CondValue.getBytes(1l, (int) CondValue.length()));
            } else {
                resultStr = "<user_device_task_list/>";
            }
            System.out.println("CondValue :" + iUserLog + " : " + resultStr);
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


    public boolean deleteServerDeviceState(
            String oServerStateId
    ) throws Throwable {


        int indx = -1;
        int loc_StateId = Integer.parseInt(oServerStateId);

        for (actuatorState iObj : this.actuatorStateList) {
            if (iObj.iStateId == loc_StateId) {
                indx = this.actuatorStateList.indexOf(iObj);
            }
        }

        if (indx != -1) {
            actuatorState remState = this.actuatorStateList.get(indx);
            remState.resetState();
            remState = null;
            this.actuatorStateList.remove(indx);
            System.gc();
            return true;
        } else {
            return false;
        }

    }

    public boolean changeServerDeviceState(
            String oServerStateId
    ) throws Throwable {


        int indx = -1;
        int loc_StateId = Integer.parseInt(oServerStateId);

        for (actuatorState iObj : this.actuatorStateList) {
            if (iObj.iStateId == loc_StateId) {
                indx = this.actuatorStateList.indexOf(iObj);
            }
        }

        if (indx != -1) {
            actuatorState changeState = this.actuatorStateList.get(indx);
            changeState.resetState();
            changeState.setConditionList();
            return true;
        } else {
            return false;
        }

    }


    private String getServerDataList(String qUserLog){
        try {

            Class.forName(MessageHandling.JDBC_DRIVER);
            Connection Con = DriverManager.getConnection(
                    MessageHandling.DB_URL
                    , MessageHandling.USER
                    , MessageHandling.PASS
            );

            CallableStatement Stmt = Con.prepareCall("{? = call s_get_server_data(?)}");
            Stmt.registerOutParameter(1,Types.BLOB);
            Stmt.setString(2, qUserLog);
            Stmt.execute();
            Blob CondValue = Stmt.getBlob(1);
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

    private String getServerPasswordsList(String qUserLog){
        try {

            Class.forName(MessageHandling.JDBC_DRIVER);
            Connection Con = DriverManager.getConnection(
                    MessageHandling.DB_URL
                    , MessageHandling.USER
                    , MessageHandling.PASS
            );

            //System.out.println("getServerPasswordsList qUserLog : " + qUserLog);

            CallableStatement Stmt = Con.prepareCall("{? = call s_get_folder_data(?)}");
            Stmt.registerOutParameter(1,Types.BLOB);
            Stmt.setString(2, qUserLog);
            Stmt.execute();
            Blob CondValue = Stmt.getBlob(1);
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


    private void setServerPorts() throws Exception {
        List<serverTypePort> serverTypePorts = new ArrayList<>();
        Document xmlDocument = MessageHandling
                .loadXMLFromString(getServerDataList(iUserLog));

        Node serverListNode = (Node) XPathFactory.newInstance().newXPath()
                .compile("/server_list").evaluate(xmlDocument, XPathConstants.NODE);

        NodeList nodeList = serverListNode.getChildNodes();

        for (int i=0; i<nodeList.getLength(); i++){
            NodeList childNodeList = nodeList.item(i).getChildNodes();
            serverTypePort serverTypePortObj = new serverTypePort();
            for (int j=0; j<childNodeList.getLength();j++) {
                if (childNodeList.item(j).getNodeName().equals("server_port")) {
                    serverTypePortObj.setPortNum(childNodeList.item(j).getTextContent());
                } else if (childNodeList.item(j).getNodeName().equals("server_type")) {
                    serverTypePortObj.setPortType(childNodeList.item(j).getTextContent());
                }
            }
            serverTypePorts.add(serverTypePortObj);
        }

        for (serverTypePort iObj : serverTypePorts) {
            if (iObj.serverPortType.equals("ssl")) {
                SecurePort = iObj.serverPortNum;
            } else {
                RegularPort = iObj.serverPortNum;
            }
        }

    }

    private void setPassWordFile()throws Exception {
        List<serverFolderPassword> serverPasswords = new ArrayList<>();
        Document xmlDocument = MessageHandling
                .loadXMLFromString(getServerPasswordsList(iUserLog));

        Node serverListNode = (Node) XPathFactory.newInstance().newXPath()
                .compile("/folder_list").evaluate(xmlDocument, XPathConstants.NODE);

        NodeList nodeList = serverListNode.getChildNodes();

        for (int i=0; i<nodeList.getLength(); i++){
            NodeList childNodeList = nodeList.item(i).getChildNodes();
            serverFolderPassword serverFolderPasswordObj = new serverFolderPassword();
            for (int j=0; j<childNodeList.getLength();j++) {
                if (childNodeList.item(j).getNodeName().equals("folder_login")) {
                    serverFolderPasswordObj.setFolderLogIn(childNodeList.item(j).getTextContent());
                } else if (childNodeList.item(j).getNodeName().equals("folder_password")) {
                    serverFolderPasswordObj.setfolderPassWord(childNodeList.item(j).getTextContent());
                }
            }
            serverPasswords.add(serverFolderPasswordObj);
        }

        FileWriter fw = new FileWriter(passWordFilePath, false);
        for (serverFolderPassword iObj: serverPasswords) {
            fw.append("\n" + iObj.folderLogIn + ":" + iObj.folderPassWord);
        }
        fw.close();
    }

}
