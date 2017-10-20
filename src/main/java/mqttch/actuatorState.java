package mqttch;

import java.util.List;

/**
 * Created by kalistrat on 20.10.2017.
 */
public class actuatorState {

    Integer iStateId;
    List<DtransitionCondition> iConditionList;

    public actuatorState(int qStateId){
        iStateId = qStateId;

        for (DtransitionCondition iCondition : iConditionList) {
            iCondition.setStateListener(new StateListener() {
                @Override
                public void afterConditionPerformed(Integer conditionId) {

                }
            });
        }

    }
}
