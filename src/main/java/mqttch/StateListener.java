package mqttch;

/**
 * Created by kalistrat on 20.10.2017.
 */
public interface StateListener {
    void afterConditionPerformed(DtransitionCondition conditionPerformed);
}
