package de.tuberlin.batchjoboperator.common.statemachine;


import java.util.Set;

public interface ConditionProvider<T extends StateMachineContext> {
    Set<Condition<T>> getCondition(String conditionName);
}
