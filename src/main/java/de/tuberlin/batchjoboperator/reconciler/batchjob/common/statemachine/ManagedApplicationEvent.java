package de.tuberlin.batchjoboperator.reconciler.batchjob.common.statemachine;

public enum ManagedApplicationEvent {
    SCHEDULED,
    RUNNING,
    COMPLETED,
    ERROR,
    REMOVED,
    NO_APPLICATION, NO_CHANGE,
}
