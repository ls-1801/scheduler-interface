package de.tuberlin.batchjoboperator.common;


public interface ConditionProvider<T extends StateMachineContext> {
    Condition<T> getCondition(String conditionName);
}
