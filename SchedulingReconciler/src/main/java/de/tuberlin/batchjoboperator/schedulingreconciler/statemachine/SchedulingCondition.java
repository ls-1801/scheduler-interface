package de.tuberlin.batchjoboperator.schedulingreconciler.statemachine;

import com.google.common.collect.ImmutableMap;
import de.tuberlin.batchjoboperator.common.Condition;
import de.tuberlin.batchjoboperator.common.crd.scheduling.AbstractSchedulingJobCondition;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.function.Supplier;

@NoArgsConstructor
@Slf4j
public abstract class SchedulingCondition extends AbstractSchedulingJobCondition implements Condition<SchedulingContext> {

    public static final String AWAIT_JOBS_ACQUIRED_CONDITION = "AWAIT_JOBS_ACQUIRED_CONDITION";
    public static final String AWAIT_SLOTS_ACQUIRED_CONDITION = "AWAIT_SLOTS_ACQUIRED_CONDITION";
    public static final String AWAIT_JOBS_ENQUEUE = "AWAIT_JOBS_ENQUEUE";
    public static final String AWAIT_JOBS_RELEASED_CONDITION = "AWAIT_JOBS_RELEASED_CONDITION";
    public static final String AWAIT_SLOTS_RELEASED_CONDITION = "AWAIT_SLOTS_RELEASED_CONDITION";

    public static final String AWAIT_JOBS_SCHEDULED_CONDITION = "AWAIT_JOBS_SCHEDULED_CONDITION";
    public static final String AWAIT_COMPLETION_CONDITION = "AWAIT_COMPLETION_CONDITION";

    public static final String AWAIT_NUMBER_OF_SLOTS_CONDITION = "AWAIT_NUMBER_OF_SLOTS_CONDITION";
    public static final String AWAIT_SLOTS_AVAILABLE_CONDITION = "AWAIT_SLOTS_AVAILABLE_CONDITION";

    public static final String AWAIT_EMPTY_JOB_QUEUE = "AWAIT_EMPTY_JOB_QUEUE";
    public static final String AWAIT_JOB_IN_QUEUE = "AWAIT_JOB_IN_QUEUE";


    public static final Map<String, Supplier<SchedulingCondition>> constructorMap =
            new ImmutableMap.Builder<String, Supplier<SchedulingCondition>>()
                    .put(AWAIT_JOBS_ACQUIRED_CONDITION, AwaitJobsAcquiredCondition::new)
                    .put(AWAIT_SLOTS_ACQUIRED_CONDITION, AwaitSlotsAcquiredCondition::new)
                    .put(AWAIT_JOBS_ENQUEUE, AwaitJobsEnqueueCondition::new)
                    .put(AWAIT_JOBS_RELEASED_CONDITION, AwaitJobsReleasedCondition::new)
                    .put(AWAIT_SLOTS_RELEASED_CONDITION, AwaitSlotsReleasedCondition::new)
                    .put(AWAIT_JOBS_SCHEDULED_CONDITION, AwaitJobsScheduledCondition::new)
                    .put(AWAIT_COMPLETION_CONDITION, AwaitJobsCompletionCondition::new)
                    .put(AWAIT_NUMBER_OF_SLOTS_CONDITION, () -> new AwaitNumberOfSlotsAvailableCondition())
                    .put(AWAIT_SLOTS_AVAILABLE_CONDITION, () -> new AwaitSlotsAvailableCondition())
                    .put(AWAIT_EMPTY_JOB_QUEUE, AwaitEmptyJobQueue::new)
                    .put(AWAIT_JOB_IN_QUEUE, AwaitJobInQueueCondition::new)
                    .build();


    public abstract String getCondition();

    @Override
    public void initialize(SchedulingContext context) {
//            this.name = NamespacedName.of(context.getResource());
//            this.job = context.getResource();
    }

    @Override
    public void update(SchedulingContext context) {
        error = null;
//            this.job = context.getResource();
        var result = updateInternal(context);
        log.debug("Update Condition: {} -> {}", getCondition(), result);

        if (value != result) {
            log.debug("Condition: {} has changed", getCondition());
            lastUpdateTimestamp = LocalDateTime.now().toString();
            value = result;
        }
    }

    protected boolean error(String problem) {
        log.error("Condition {} got an error: \n{}\n", getCondition(), problem);
        if (!problem.equals(error)) {
            lastUpdateTimestamp = LocalDateTime.now().toString();
        }

        this.error = problem;
        return false;
    }

    protected abstract boolean updateInternal(SchedulingContext client);
}
