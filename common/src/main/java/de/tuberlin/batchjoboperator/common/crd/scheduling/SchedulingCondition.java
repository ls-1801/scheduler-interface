package de.tuberlin.batchjoboperator.common.crd.scheduling;

public enum SchedulingCondition {
    AWAITING_EMPTY_SLOT,
    AWAITING_APPLICATION_START,
    AWAITING_COMPLETION,
    COMPLETED,
    ERROR
}
