package mqttch;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by kalistrat on 02.05.2017.
 */
public class MessageHandling {

    static final private String JDBC_DRIVER = "com.mysql.jdbc.Driver";
    static final private String DB_URL = "jdbc:mysql://localhost/things";
    static final private String USER = "kalistrat";
    static final private String PASS = "045813";

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

    public static Double StrToDouble(String StrValue){
        try {
            //System.out.println(Sval);
            return Double.parseDouble(StrValue.replace(",","."));
        } catch (NumberFormatException e) {
            return null;
        }
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
                String SubscriberId = MessageList.get(3);

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
                )) {
                    OutMessage = OutMessage + "Неизвестный тип подписчика;";
                }

                if (OutMessage.equals("")) {
                    if (SubcriberType.equals("sensor")) {
                        OutMessage = sensorListUpdate(ActionType, UserLog, SubscriberId);
                    }

                    if (SubcriberType.equals("i_transion")) {
                        OutMessage = "добавление\\удаление независимых переходов не поддерживается;";
                    }

                    if (SubcriberType.equals("d_transion")) {
                        OutMessage = "добавление\\удаление зависимых переходов не поддерживается;";
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

    public static void topicDataLog(int qDeviceId
            ,String mqttMes
            ,Double doubleValue
    ){
        try {

            Class.forName(JDBC_DRIVER);
            Connection Con = DriverManager.getConnection(
                    DB_URL
                    , USER
                    , PASS
            );

            CallableStatement Stmt = Con.prepareCall("{call s_p_topic_data_log(?, ?, ?)}");
            Stmt.setInt(1, qDeviceId);
            Stmt.setString(2, mqttMes);
            Stmt.setDouble(3,doubleValue);
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
                    "join users u on u.user_id=ud.user_id";

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
}
