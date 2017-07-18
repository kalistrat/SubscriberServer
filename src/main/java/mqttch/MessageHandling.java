package mqttch;

import java.sql.*;
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

    public static int getSubsriberIndexByName(String sName){
        int indx = -1;
        for (SubscriberLogger iObj : Main.SubscriberLoggerList) {
            if (iObj.TopicName.equals(sName)) {
                indx = Main.SubscriberLoggerList.indexOf(iObj);
            }
        }
        return indx;
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
            System.out.println("eClientData : " + eClientData);
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
                        SubcriberType.equals("sensor")
                                || SubcriberType.equals("i_transion")
                                || SubcriberType.equals("d_transion")
                                || SubcriberType.equals("task")
                )) {
                    OutMessage = OutMessage + "Неизвестный тип подписчика;";
                }

                if (OutMessage.equals("")) {
                    if (SubcriberType.equals("sensor")) {
                        OutMessage = sensorListUpdate(ActionType, UserLog, EntityId);
                    }

                    if (SubcriberType.equals("i_transion")) {
                        OutMessage = "добавление\\удаление независимых переходов не поддерживается;";
                    }

                    if (SubcriberType.equals("d_transion")) {
                        OutMessage = dTransitionListUpdate(ActionType,UserLog,EntityId);
                    }

                    if (SubcriberType.equals("task")) {
                        //OutMessage = dTaskListUpdate(ActionType,UserLog,EntityId);
                    }

                    //itransitionListUpdate
                    //dtransitionListUpdate
                } else {
                    OutMessage = "N|" + OutMessage + "|";
                }

            } else {
                OutMessage = "N|Неподдерживаемый тип сообщения;|";
            }

            return OutMessage;
        } catch (Exception eMessageHandling){
            eMessageHandling.printStackTrace();
            return "N|Ошибка обработчика сообщения;|";
        }
    }

    public static String sensorListUpdate(
            String qActionType
            ,String qUserLog
            ,String qDeviceId
    ){
        String outMess;
        String TopicName = "";
        String MqttServerHost = "";
        int iDeviceId = Integer.parseInt(qDeviceId);

        try {

            Class.forName(JDBC_DRIVER);
            Connection Con = DriverManager.getConnection(
                    DB_URL
                    , USER
                    , PASS
            );

            CallableStatement Stmt = Con.prepareCall("{call s_p_sensor_initial(?, ?, ?, ?)}");
            Stmt.setString(1, qUserLog);
            Stmt.setInt(2, iDeviceId);
            Stmt.registerOutParameter(3, Types.VARCHAR);
            Stmt.registerOutParameter(4, Types.VARCHAR);

            Stmt.execute();

            TopicName = Stmt.getString(3);
            MqttServerHost = Stmt.getString(4);

            Con.close();

        }catch(SQLException se){
            //Handle errors for JDBC
            se.printStackTrace();
        }catch(Exception e) {
            //Handle errors for Class.forName
            e.printStackTrace();
        }

        if (TopicName == null) {
            TopicName = "";
        }

        if (!TopicName.equals("")) {

            int SubscriberIndx = MessageHandling.getSubsriberIndexByName(TopicName);
            try {
                if (qActionType.equals("add")) {
                    if (SubscriberIndx == -1) {
                        Main.SubscriberLoggerList.add(new SubscriberLogger(TopicName,MqttServerHost));
                        outMess = "Y|"+"Подписчик " + TopicName + " успешно добавлен" + "|";
                    } else {
                        outMess = "N|"+"Подписчик " + TopicName + " уже добавлен" + "|";
                    }

                } else {
                    if (SubscriberIndx != -1) {
                        SubscriberLogger s1 = Main.SubscriberLoggerList.get(SubscriberIndx);
                        s1.client.disconnect();
                        Main.SubscriberLoggerList.remove(SubscriberIndx);
                        //s1 = null;
                        //System.gc();
                        outMess = "Y|"+"Подписчик " + TopicName + " успешно удалён" + "|";
                    } else {
                        outMess = "N|"+"Подписчик " + TopicName + " не найден" + "|";
                    }
                }
            } catch (Throwable e) {
                outMess = "N|Ошибка подключения к mqtt-серверу;|";
            }
        } else {
            outMess = "N|Ошибка инициализации устройства из базы данных;|";
        }

        return outMess;
    }

    public static void topicDataLog(String qTopicName
                                    ,java.sql.Timestamp qMessDate
            ,String StringValue
            ,Double doubleValue
    ){
        try {

            Class.forName(JDBC_DRIVER);
            Connection Con = DriverManager.getConnection(
                    DB_URL
                    , USER
                    , PASS
            );

            CallableStatement Stmt = Con.prepareCall("{call s_p_topic_data_log(?, ?, ? ,?)}");
            Stmt.setString(1, qTopicName);
            Stmt.setTimestamp(2, qMessDate);
            Stmt.setString(3,StringValue);
            if (doubleValue != null) {
                Stmt.setDouble(4, doubleValue);
            } else {
                Stmt.setNull(4, Types.DECIMAL);
            }
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

    public static void createSubscriberLoggerList(){
        try {
            Class.forName(JDBC_DRIVER);
            Connection Con = DriverManager.getConnection(
                    DB_URL
                    , USER
                    , PASS
            );

            String DataSql = "select 'add'\n" +
                    ",u.user_log\n" +
                    ",ud.user_device_id\n" +
                    "from user_device ud\n" +
                    "join action_type aty on aty.action_type_id=ud.action_type_id\n" +
                    "join users u on u.user_id=ud.user_id\n" +
                    "where aty.action_type_id = 1";

            PreparedStatement DataStmt = Con.prepareStatement(DataSql);


            ResultSet DataRs = DataStmt.executeQuery();

            while (DataRs.next()) {

                System.out.println(

                        DataRs.getString(1)
                        +DataRs.getString(2)
                        +String.valueOf(DataRs.getInt(3))
                        + " : "
                        + sensorListUpdate(DataRs.getString(1),DataRs.getString(2),String.valueOf(DataRs.getInt(3)))
                );
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

    public static String dTransitionListUpdate(
            String qActionType
            ,String qUserLog
            ,String qConditionId
    ){
        String outMess;
        String ReadTopicName = "";
        String MqttServerHost = "";
        String leftExpr = "";
        String rightExpr = "";
        String signExpr = "";
        Integer TimeInt = null;

        int iConditionId = Integer.parseInt(qConditionId);

        try {

            Class.forName(JDBC_DRIVER);
            Connection Con = DriverManager.getConnection(
                    DB_URL
                    , USER
                    , PASS
            );

            String DataSql = "select ud.mqtt_topic_write\n" +
                    ",ms.server_ip\n" +
                    ",uasc.left_part_expression\n" +
                    ",uasc.right_part_expression\n" +
                    ",uasc.sign_expression\n" +
                    ",uasc.condition_interval\n" +
                    "from user_actuator_state_condition uasc\n" +
                    "join user_actuator_state uas on uas.user_actuator_state_id=uasc.user_actuator_state_id\n" +
                    "join user_device ud on ud.user_device_id=uas.user_device_id\n" +
                    "join mqtt_servers ms on ms.server_id=ud.mqqt_server_id\n" +
                    "where uasc.actuator_state_condition_id = ?";

            PreparedStatement DataStmt = Con.prepareStatement(DataSql);
            DataStmt.setInt(1, iConditionId);


            ResultSet DataRs = DataStmt.executeQuery();

            while (DataRs.next()) {
                ReadTopicName = DataRs.getString(1);
                MqttServerHost = DataRs.getString(2);
                leftExpr = DataRs.getString(3);
                rightExpr = DataRs.getString(4);
                signExpr = DataRs.getString(5);
                TimeInt = DataRs.getInt(6);
            }

            Con.close();

        }catch(SQLException se){
            //Handle errors for JDBC
            se.printStackTrace();
        }catch(Exception e) {
            //Handle errors for Class.forName
            e.printStackTrace();
        }

        if (ReadTopicName == null) {
            ReadTopicName = "";
        }

        if (!ReadTopicName.equals("")) {

            int ConditionIndx = MessageHandling.getItransitionConditionIndexByName(ReadTopicName);
            try {
                if (qActionType.equals("add")) {
                    if (ConditionIndx == -1) {
                        Main.DtransitionConditionList.add(new DtransitionCondition(
                                ReadTopicName
                                ,MqttServerHost
                                ,leftExpr
                                ,rightExpr
                                ,signExpr
                                ,TimeInt
                                ,iConditionId
                            )
                        );
                        outMess = "Y|"+"Зависимый переход " + ReadTopicName + " успешно добавлен" + "|";
                    } else {
                        outMess = "N|"+"Зависимый переход " + ReadTopicName + " уже добавлен" + "|";
                    }

                } else {
                    if (ConditionIndx != -1) {
                        DtransitionCondition ic1 = Main.DtransitionConditionList.get(ConditionIndx);
                        ic1.disconnectVarList();
                        Main.DtransitionConditionList.remove(ConditionIndx);
                        //s1 = null;
                        //System.gc();
                        outMess = "Y|"+"Зависимый переход " + ReadTopicName + " успешно удалён" + "|";
                    } else {
                        outMess = "N|"+"Зависимый переход " + ReadTopicName + " не найден" + "|";
                    }
                }
            } catch (Throwable e) {
                outMess = "N|Ошибка подключения к mqtt-серверу;|";
            }
        } else {
            outMess = "N|Ошибка инициализации условия из базы данных;|";
        }

        return outMess;
    }

    public static String dTaskListUpdate(
            String qActionType
            ,String qUserLog
            ,String qTaskId
    ){
        String iTaskTypeName;
        int iTaskInterval;
        String iIntervalType;
        String iWriteTopicName = null;
        String iServerIp;
        String iControlLog;
        String iControlPass;
        String iMessageValue;
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

//            int ConditionIndx = MessageHandling.getItransitionConditionIndexByName(iWriteTopicName);
//            try {
//                if (qActionType.equals("add")) {
//                    if (ConditionIndx == -1) {
//                        Main.DtransitionConditionList.add(new DtransitionCondition(
//                                        ReadTopicName
//                                        ,MqttServerHost
//                                        ,leftExpr
//                                        ,rightExpr
//                                        ,signExpr
//                                        ,TimeInt
//                                        ,iConditionId
//                                )
//                        );
//                        outMess = "Y|"+"Зависимый переход " + ReadTopicName + " успешно добавлен" + "|";
//                    } else {
//                        outMess = "N|"+"Зависимый переход " + ReadTopicName + " уже добавлен" + "|";
//                    }
//
//                } else {
//                    if (ConditionIndx != -1) {
//                        DtransitionCondition ic1 = Main.DtransitionConditionList.get(ConditionIndx);
//                        ic1.disconnectVarList();
//                        Main.DtransitionConditionList.remove(ConditionIndx);
//                        //s1 = null;
//                        //System.gc();
//                        outMess = "Y|"+"Зависимый переход " + ReadTopicName + " успешно удалён" + "|";
//                    } else {
//                        outMess = "N|"+"Зависимый переход " + ReadTopicName + " не найден" + "|";
//                    }
//                }
//            } catch (Throwable e) {
//                outMess = "N|Ошибка подключения к mqtt-серверу;|";
//            }
            outMess = "N|Ошибка инициализации задания, они пока не поддерживаются;|";
        } else {
            outMess = "N|Ошибка инициализации условия из базы данных;|";
        }

        return outMess;
    }
}
