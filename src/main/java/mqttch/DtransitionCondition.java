package mqttch;


import java.sql.*;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * Created by kalistrat on 07.06.2017.
 */
public class DtransitionCondition implements StateChangeListenable {
    List<ConditionVariable> VarsList;
    //String ReadTopicName;
    StateListener stateListener;
    Integer iConditionId;
    boolean isPerforming;
    MathParser mathParser;
    String rightExpr;
    String leftExpr;
    String signExpr;
    //Integer iStateId;
    actuatorState iActuatorState;

    public DtransitionCondition(String readTopicName
            , String mqttHostName
            , String leftPartExpression
            , String rightPartExpression
            , String signExpression
            , Integer timeInterval
            , final int conditionId
    ) throws Throwable {

        try {


            VarsList = new ArrayList<ConditionVariable>();
            //ReadTopicName = readTopicName;
            iConditionId = conditionId;
            isPerforming = false;
            rightExpr = rightPartExpression;
            leftExpr = leftPartExpression;
            signExpr = signExpression;
            //iStateId = null;
            setVarsList();

        } catch (Throwable e0){
        //e0.printStackTrace();
        //System.out.println("Исключение передаётся в конструктор");
            throw  e0;

        }

    }

    public DtransitionCondition(){
        VarsList = new ArrayList<ConditionVariable>();
        isPerforming = false;
        mathParser = new MathParser();
    }

    public void setParentState(actuatorState ActuatorState){
        iActuatorState = ActuatorState;
    }

    public void setConditionId(int conditionId){
        this.iConditionId = conditionId;
    }

    public void setRightExpr(String rightExpr){
        this.rightExpr = rightExpr;
    }

    public void setLeftExpr(String qleftExpr){
        this.leftExpr = qleftExpr;
    }

    public void setSignExpr(String qsignExpr){
        this.signExpr = qsignExpr;
    }


    public void setVarsList() throws Throwable {
        addVarsList(iConditionId);
        for (ConditionVariable iO : VarsList) {


            iO.setVarListener(new VarListener() {
                @Override
                public void afterValueChange(ConditionVariable varChanged) {
                    //System.out.println("Изменена переменная : " + varChanged.VarName);
                    if (isConditionPerformed(varChanged)) {
                        isPerforming = true;
                        stateListener.afterConditionPerformed(getObjectCondition());
                    } else {
                        isPerforming = false;
                        System.out.println("iActuatorState.stateTimer.commitedTime : " + iActuatorState.stateTimer.commitedTime);
                        iActuatorState.stateTimer.stopExecution();
                    }
                }
            });
        }
    }

    public DtransitionCondition getObjectCondition(){
        return this;
    }

    public void addVarsList(int qConditionId) throws Throwable {
        try {

            Class.forName(MessageHandling.JDBC_DRIVER);
            Connection Con = DriverManager.getConnection(
                    MessageHandling.DB_URL
                    , MessageHandling.USER
                    , MessageHandling.PASS
            );

            String DataSql = "select cv.var_code\n" +
                    ",ud.mqtt_topic_write\n" +
                    ",ms.server_ip\n" +
                    ",udt.control_log\n" +
                    ",udt.control_pass\n" +
                    "from user_state_condition_vars cv\n" +
                    "join user_device ud on ud.user_device_id=cv.user_device_id\n" +
                    "join user_devices_tree udt on udt.user_device_id=ud.user_device_id\n" +
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
                        ,DataRs.getString(4)//Логин
                        ,DataRs.getString(5)//Пароль
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

    public void setStateListener(StateListener listener){
        this.stateListener = listener;
    }

    private Integer calcDeltaTimeValue(){
        int curValue = 0;
        for (ConditionVariable iVar : VarsList){

            if (iVar.deltaTimeSec == null) {
                curValue = -1;
                break;
            } else {
                if (iVar.deltaTimeSec.intValue() > curValue) {
                    curValue = iVar.deltaTimeSec;
                }
            }
        }
        if (curValue != -1) {
            return curValue;
        } else {
            return null;
        }
    }

    private boolean isMeasuredDateDefine(java.util.Date varChangedDate,Integer cInterval){

        Calendar cal = Calendar.getInstance();
        cal.setTime(varChangedDate);
        cal.add(Calendar.SECOND, cInterval * (-1));

        java.util.Date leftBorderDate = cal.getTime();
        java.util.Date rightBorderDate = varChangedDate;
        int iCnt = 0;

        for (ConditionVariable iVar : VarsList) {
            if (iVar.VarDate == null) {
                break;
            } else {
                if (leftBorderDate.compareTo(iVar.VarDate) * iVar.VarDate.compareTo(rightBorderDate) >= 0) {
                    iCnt = iCnt + 1;
                }
            }
        }

        if (VarsList.size() == iCnt) {
            return true;
        } else {
            return false;
        }
    }

    private Double getExpressionValue(String strExpr){
        mathParser.VarList.clear();
        mathParser.var.clear();
        for (ConditionVariable iVariable : VarsList) {
            mathParser.setVariable(iVariable.VarName,iVariable.VarValue);
        }
        try {
            return mathParser.Parse(strExpr);
        } catch (Exception e) {
            return null;
        }
    }

    private boolean isConditionPerformed(ConditionVariable qVarChanged){
//        Integer deltaT = calcDeltaTimeValue();
//        if (deltaT != null) {
//            if (isMeasuredDateDefine(qVarChanged.VarDate,deltaT)) {
                Double rightExprValue = getExpressionValue(rightExpr);
                Double leftExprValue = getExpressionValue(leftExpr);

                if (rightExprValue!=null && leftExprValue!=null) {

                    if (signExpr.equals(">")) {
                        if (leftExprValue.doubleValue() > rightExprValue.doubleValue()) {
                            return true;
                        } else {
                            return false;
                        }
                    } else if (signExpr.equals("<")) {
                        if (leftExprValue.doubleValue() < rightExprValue.doubleValue()) {
                            return true;
                        } else {
                            return false;
                        }
                    } else if (signExpr.equals("<=")) {
                        if (leftExprValue.doubleValue() <= rightExprValue.doubleValue()) {
                            return true;
                        } else {
                            return false;
                        }
                    } else if (signExpr.equals(">=")) {
                        if (leftExprValue.doubleValue() >= rightExprValue.doubleValue()) {
                            return true;
                        } else {
                            return false;
                        }
                    } else if (signExpr.equals("=")) {
                        if (leftExprValue.doubleValue() == rightExprValue.doubleValue()) {
                            return true;
                        } else {
                            return false;
                        }
                    } else {
                        System.out.println("can't parse sign : condition_id " + iConditionId);
                        return false;
                    }
                } else {
                    System.out.println("can't parse expression : condition_id " + iConditionId);
                    return false;
                }

//            } else {
//                System.out.println("no entry in the date range: condition_id " + iConditionId);
//                return false;
//            }
//        } else {
//            System.out.println("deltaT = null : condition_id " + iConditionId);
//            return false;
//        }

    }

}
