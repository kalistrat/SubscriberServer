package mqttch;

import io.moquette.interception.AbstractInterceptHandler;
import io.moquette.interception.InterceptHandler;
import io.moquette.interception.messages.InterceptPublishMessage;
import io.moquette.server.Server;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Date;

/**
 * Created by kalistrat on 27.09.2017.
 */
public class internalMqttServer extends Server {
    String passWordFilePath;
    List<DtransitionCondition> ConditionList;
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
        ConditionList = new ArrayList<>();
        Document xmlDocument = MessageHandling
                .loadXMLFromString(getServerDataList(iUserLog));

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

    public boolean addServerTask(
            String qTaskId
    ) throws Throwable {


        Document xmlDocument = MessageHandling
                .loadXMLFromString(getTaskData(Integer.parseInt(qTaskId)));

        String oTaskTypeName = XPathFactory.newInstance().newXPath()
                .compile("/task_data/task_type_name").evaluate(xmlDocument);
        Integer oTaskInterval = Integer.parseInt(XPathFactory.newInstance().newXPath()
                .compile("/task_data/task_interval").evaluate(xmlDocument));
        String oIntervalType = XPathFactory.newInstance().newXPath()
                .compile("/task_data/interval_type").evaluate(xmlDocument);
        String oWriteTopicName = XPathFactory.newInstance().newXPath()
                .compile("/task_data/write_topic_name").evaluate(xmlDocument);
        String oServerIp = XPathFactory.newInstance().newXPath()
                .compile("/task_data/server_ip").evaluate(xmlDocument);
        String oControlLog = XPathFactory.newInstance().newXPath()
                .compile("/task_data/control_log").evaluate(xmlDocument);
        String oControlPass = XPathFactory.newInstance().newXPath()
                .compile("/task_data/control_pass").evaluate(xmlDocument);
        String oMessageValue = XPathFactory.newInstance().newXPath()
                .compile("/task_data/message_value").evaluate(xmlDocument);

        int indx = -1;

        for (PublisherTask iObj : this.PublisherTaskList) {

            if (iObj.iWriteTopicName.equals(oWriteTopicName)) {
                indx = this.PublisherTaskList.indexOf(iObj);
            }
        }

        if (indx == -1) {
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
            String qTaskId
    ) throws Throwable {

        Document xmlDocument = MessageHandling
                .loadXMLFromString(getTaskData(Integer.parseInt(qTaskId)));
        String oWriteTopicName = XPathFactory.newInstance().newXPath()
                .compile("/task_data/write_topic_name").evaluate(xmlDocument);

        int indx = -1;
        for (PublisherTask iObj : this.PublisherTaskList) {
            if (iObj.iWriteTopicName.equals(oWriteTopicName)) {
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
        createPublisherTaskList();

    }

    public void addMessageIntoDB(String qTopicName
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

    public boolean addServerCondition(String qConditionId) throws Throwable {

        Document xmlDocument = MessageHandling
                .loadXMLFromString(getConditionData(Integer.parseInt(qConditionId)));

        String ReadTopicName = XPathFactory.newInstance().newXPath()
                .compile("/condition_rule/mqtt_topic_write").evaluate(xmlDocument);
        String MqttServerHost = XPathFactory.newInstance().newXPath()
                .compile("/condition_rule/server_ip").evaluate(xmlDocument);
        String leftExpr = XPathFactory.newInstance().newXPath()
                .compile("/condition_rule/left_part_expression").evaluate(xmlDocument);
        String rightExpr = XPathFactory.newInstance().newXPath()
                .compile("/condition_rule/right_part_expression").evaluate(xmlDocument);
        String signExpr = XPathFactory.newInstance().newXPath()
                .compile("/condition_rule/sign_expression").evaluate(xmlDocument);
        Integer TimeInt = Integer.parseInt(XPathFactory.newInstance().newXPath()
                .compile("/condition_rule/condition_interval").evaluate(xmlDocument));


        int indx = -1;
        for (DtransitionCondition iObj : this.ConditionList) {
            if (iObj.ReadTopicName.equals(ReadTopicName)) {
                indx = this.ConditionList.indexOf(iObj);
            }
        }

        if (indx == -1) {
            this.ConditionList.add(new DtransitionCondition(
                    ReadTopicName
                    ,MqttServerHost
                    ,leftExpr
                    ,rightExpr
                    ,signExpr
                    ,TimeInt
                    ,Integer.parseInt(qConditionId)
            ));
            return true;
        } else {
            return false;
        }
    }

    public String getConditionData(int qConditionId){
        try {

            Class.forName(MessageHandling.JDBC_DRIVER);
            Connection Con = DriverManager.getConnection(
                    MessageHandling.DB_URL
                    , MessageHandling.USER
                    , MessageHandling.PASS
            );

            CallableStatement Stmt = Con.prepareCall("{? = call s_message_recerve(?)}");
            Stmt.registerOutParameter(1,Types.BLOB);
            Stmt.setInt(2, qConditionId);
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


    public boolean deleteServerCondition(
            String oServerConditionId
    ) throws Throwable {

        Document xmlDocument = MessageHandling
                .loadXMLFromString(getConditionData(Integer.parseInt(oServerConditionId)));
        String ReadTopicName = XPathFactory.newInstance().newXPath()
                .compile("/condition_rule/mqtt_topic_write").evaluate(xmlDocument);

        int indx = -1;
        for (DtransitionCondition iObj : this.ConditionList) {
            if (iObj.ReadTopicName.equals(ReadTopicName)) {
                indx = this.ConditionList.indexOf(iObj);
            }
        }

        if (indx != -1) {
            DtransitionCondition remCondition = this.ConditionList.get(indx);
            remCondition = null;
            this.ConditionList.remove(indx);
            System.gc();
            return true;
        } else {
            return false;
        }

    }


    public String getTaskData(int qTaskId){
        try {

            Class.forName(MessageHandling.JDBC_DRIVER);
            Connection Con = DriverManager.getConnection(
                    MessageHandling.DB_URL
                    , MessageHandling.USER
                    , MessageHandling.PASS
            );

            CallableStatement Stmt = Con.prepareCall("{? = call s_get_task_data(?)}");
            Stmt.registerOutParameter(1,Types.BLOB);
            Stmt.setInt(2, qTaskId);
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

    public String getServerDataList(String qUserLog){
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
