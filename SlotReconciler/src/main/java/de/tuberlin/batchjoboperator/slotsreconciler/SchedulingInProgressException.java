package de.tuberlin.batchjoboperator.slotsreconciler;

public class SchedulingInProgressException extends RuntimeException {
    public SchedulingInProgressException(String s) {
        super(s);
    }
}
