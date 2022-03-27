package de.tuberlin.batchjoboperator.testbedreconciler;

public class SchedulingInProgressException extends RuntimeException {
    public SchedulingInProgressException(String s) {
        super(s);
    }
}
