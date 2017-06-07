package mqttch;

import java.util.List;

/**
 * Created by kalistrat on 07.06.2017.
 */
public class ItransitionCondition {
    List<ConditionVariable> VarsList;
    public ItransitionCondition(String readTopicName
        ,String mqttHostName
        ,String leftPartExpression
        ,String rightPartExpression
        ,String signExpression
    ){

        VarsList.get(0).setListener(new VarListener() {
            public void afterValueChange(String ChangedVarName) {

            }
        });

    }
}
