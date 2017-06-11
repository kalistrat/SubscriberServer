package mqttch;


import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by kalistrat on 07.06.2017.
 */
public class DtransitionCondition {
    List<ConditionVariable> VarsList;
    String ReadTopicName;
    public DtransitionCondition(String readTopicName
            , String mqttHostName
            , String leftPartExpression
            , String rightPartExpression
            , String signExpression
            , Integer timeInterval
            , int conditionId
    ) throws Throwable {

        try {


            VarsList = new ArrayList<ConditionVariable>();
            setVarsList(conditionId);
            ReadTopicName = readTopicName;

            for (ConditionVariable iO : VarsList) {

                iO.setListener(new VarListener() {
                    public void afterValueChange(String ChangedVarName) {
                        System.out.println("Изменена переменная : " + ChangedVarName);
                    }
                });
            }

        } catch (Throwable e0){
        //e0.printStackTrace();
        //System.out.println("Исключение передаётся в конструктор");
            throw  e0;

        }



    }

    public void setVarsList(int qConditionId) throws Throwable {
        try {

            Class.forName(MessageHandling.JDBC_DRIVER);
            Connection Con = DriverManager.getConnection(
                    MessageHandling.DB_URL
                    , MessageHandling.USER
                    , MessageHandling.PASS
            );

            String DataSql = "select cv.var_code\n" +
                    ",ud.mqtt_topic_write\n" +
                    ",concat(ms.server_ip,concat(':',ms.server_port))\n" +
                    "from user_state_condition_vars cv\n" +
                    "join user_device ud on ud.user_device_id=cv.user_device_id\n" +
                    "join mqtt_servers ms on ms.server_id=ud.mqqt_server_id\n" +
                    "where cv.actuator_state_condition_id = ?";

            PreparedStatement DataStmt = Con.prepareStatement(DataSql);
            DataStmt.setInt(1, qConditionId);


            ResultSet DataRs = DataStmt.executeQuery();

            while (DataRs.next()) {

                VarsList.add(new ConditionVariable(
                        qConditionId
                        ,DataRs.getString(2)//String topicName
                        ,DataRs.getString(3)//String mqttServerHost
                        ,DataRs.getString(1)//String varName
                ));



            }

            Con.close();

        } catch(SQLException se) {
            //Handle errors for JDBC
            se.printStackTrace();
        } catch (Throwable edc){
            //System.out.println("Начала трейса..");
            //edc.printStackTrace();
            //System.out.println("Конец трейса..");
            //System.out.println("MqttException in setVarsList");
            throw  edc;

        }
    }

    public void disconnectVarList() throws Throwable {
        try {
            for (ConditionVariable iObj : VarsList){
                iObj.client.disconnect();
            }
        }catch (Throwable e){
            //e.printStackTrace();
            throw  e;
        }
    }
}
