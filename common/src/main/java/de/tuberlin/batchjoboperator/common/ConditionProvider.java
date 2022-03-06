package de.tuberlin.batchjoboperator.common;


import java.util.Set;

public interface ConditionProvider<T extends StateMachineContext> {
    Set<Condition<T>> getCondition(String conditionName);
}
