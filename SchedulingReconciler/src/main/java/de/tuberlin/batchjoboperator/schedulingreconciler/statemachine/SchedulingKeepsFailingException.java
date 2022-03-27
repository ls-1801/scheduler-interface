package de.tuberlin.batchjoboperator.schedulingreconciler.statemachine;

public class SchedulingKeepsFailingException extends RuntimeException {
    public SchedulingKeepsFailingException() {
        super("Scheduling Algorithm Conditions, stays fulfilled, but no job can be scheduled!");
    }
}
