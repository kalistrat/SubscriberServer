package mqttch;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by kalistrat on 20.10.2017.
 */
public class actuatorState {

    Integer iStateId;
    Integer iStateDeltaT;
    List<DtransitionCondition> iConditionList;
    actuatorStateTimer stateTimer;

    public actuatorState(int qStateId) throws Throwable {
        iStateId = qStateId;
        iStateDeltaT = 5;
        stateTimer = new actuatorStateTimer();
        iConditionList = new ArrayList<>();
        setConditionList();

        for (DtransitionCondition iCondition : iConditionList) {
            iCondition.setStateListener(new StateListener() {
                @Override
                public void afterConditionPerformed(DtransitionCondition conditionPerformed) {
                    if (isPerformedAllConditions()) {
                        if (stateTimer.commitedTime.intValue() == 0) {
                            stateTimer.startExecution();
                        } else {
                            if (stateTimer.commitedTime.intValue() >= iStateDeltaT.intValue()) {
                                //releaseThisState
                            }
                        }
                    } else {
                        if (stateTimer.commitedTime.intValue() != 0) {
                            stateTimer.stopExecution();
                        }
                    }
                }
            });
        }

    }

    private boolean isPerformedAllConditions(){
        int iCnt = 0;
        for (DtransitionCondition iCondRule : iConditionList) {
            if (iCondRule.isPerforming) {
                iCnt = iCnt + 1;
            }
        }
        if (iConditionList.size() == iCnt) {
            return true;
        } else {
            return false;
        }
    }

    private String getXMLConditionList(){
        try {

            Class.forName(MessageHandling.JDBC_DRIVER);
            Connection Con = DriverManager.getConnection(
                    MessageHandling.DB_URL
                    , MessageHandling.USER
                    , MessageHandling.PASS
            );

            CallableStatement Stmt = Con.prepareCall("{? = call s_get_state_condition_list(?)}");
            Stmt.registerOutParameter(1, Types.BLOB);
            Stmt.setInt(2,iStateId);
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

    public void setConditionList() throws Throwable {

        for (DtransitionCondition iDc : iConditionList) {
            iDc.disconnectVarList();
            iDc = null;
        }
        iConditionList.clear();
        System.gc();

        Document xmlDocument = MessageHandling
                .loadXMLFromString(getXMLConditionList());

        Node serverListNode = (Node) XPathFactory.newInstance().newXPath()
                .compile("/condition_list").evaluate(xmlDocument, XPathConstants.NODE);

        NodeList nodeList = serverListNode.getChildNodes();

        for (int i=0; i<nodeList.getLength(); i++){
            NodeList childNodeList = nodeList.item(i).getChildNodes();
            DtransitionCondition iCondRule = new DtransitionCondition();
            for (int j=0; j<childNodeList.getLength();j++) {
                if (childNodeList.item(j).getNodeName().equals("actuator_state_condition_id")) {
                    iCondRule.setConditionId(Integer.parseInt(childNodeList.item(j).getTextContent()));
                    iCondRule.setVarsList();
                } else if (childNodeList.item(j).getNodeName().equals("left_part_expression")) {
                    iCondRule.setLeftExpr(childNodeList.item(j).getTextContent());
                } else if (childNodeList.item(j).getNodeName().equals("sign_expression")) {
                    iCondRule.setSignExpr(childNodeList.item(j).getTextContent());
                } else if (childNodeList.item(j).getNodeName().equals("right_part_expression")) {
                    iCondRule.setRightExpr(childNodeList.item(j).getTextContent());
                }
            }
            iConditionList.add(iCondRule);
        }
    }
}
