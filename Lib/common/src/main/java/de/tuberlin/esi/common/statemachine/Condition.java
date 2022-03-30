package de.tuberlin.esi.common.statemachine;

public interface Condition<T extends StateMachineContext> {
    Boolean getValue();

    String getError();

    void update(T context);

    void initialize(T context);

    String getCondition();
}
