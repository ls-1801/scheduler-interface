package de.tuberlin.batchjoboperator.schedulingreconciler.statemachine;

public class AwaitEmptyJobQueue extends SchedulingCondition {

    public static final String condition = AWAIT_EMPTY_JOB_QUEUE;

    @Override
    public String getCondition() {
        return AWAIT_EMPTY_JOB_QUEUE;
    }

    @Override
    protected boolean updateInternal(SchedulingContext context) {
        return context.getStrategy().isQueueEmpty();
    }


}

