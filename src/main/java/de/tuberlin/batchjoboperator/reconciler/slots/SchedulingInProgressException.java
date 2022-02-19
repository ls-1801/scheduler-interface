package de.tuberlin.batchjoboperator.reconciler.slots;

public class SchedulingInProgressException extends RuntimeException {
    public SchedulingInProgressException(String s) {
        super(s);
    }
}
