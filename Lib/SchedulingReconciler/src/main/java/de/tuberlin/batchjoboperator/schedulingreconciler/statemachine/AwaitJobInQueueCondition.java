package de.tuberlin.batchjoboperator.schedulingreconciler.statemachine;

public class AwaitJobInQueueCondition extends SchedulingCondition {

    public static final String condition = AWAIT_JOB_IN_QUEUE;

    @Override
    public String getCondition() {
        return AWAIT_JOB_IN_QUEUE;
    }

    @Override
    protected boolean updateInternal(SchedulingContext context) {
        return !context.getStrategy().isQueueEmpty();
    }

}

