package de.tuberlin.batchjoboperator.schedulingreconciler.statemachine;

import com.google.common.collect.ImmutableMap;
import de.tuberlin.batchjoboperator.common.crd.scheduling.AbstractSchedulingJobCondition;
import de.tuberlin.batchjoboperator.common.statemachine.Condition;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.function.Supplier;

@NoArgsConstructor
@Slf4j
public abstract class SchedulingCondition extends AbstractSchedulingJobCondition implements Condition<SchedulingContext> {

    public static final String AWAIT_JOBS_ACQUIRED_CONDITION = "AWAIT_JOBS_ACQUIRED_CONDITION";
    public static final String AWAIT_TESTBED_ACQUIRED_CONDITION = "AWAIT_SLOTS_ACQUIRED_CONDITION";
    public static final String AWAIT_JOBS_ENQUEUE = "AWAIT_JOBS_ENQUEUE";
    public static final String AWAIT_JOBS_RELEASED_CONDITION = "AWAIT_JOBS_RELEASED_CONDITION";
    public static final String AWAIT_TESTBED_RELEASED_CONDITION = "AWAIT_SLOTS_RELEASED_CONDITION";

    public static final String AWAIT_JOBS_SCHEDULED_CONDITION = "AWAIT_JOBS_SCHEDULED_CONDITION";
    public static final String AWAIT_COMPLETION_CONDITION = "AWAIT_COMPLETION_CONDITION";

    public static final String AWAIT_NUMBER_OF_SLOTS_CONDITION = "AWAIT_NUMBER_OF_SLOTS_CONDITION";
    public static final String AWAIT_SLOTS_AVAILABLE_CONDITION = "AWAIT_SLOTS_AVAILABLE_CONDITION";

    public static final String AWAIT_EMPTY_JOB_QUEUE = "AWAIT_EMPTY_JOB_QUEUE";
    public static final String AWAIT_JOB_IN_QUEUE = "AWAIT_JOB_IN_QUEUE";


    public static final Map<String, Supplier<SchedulingCondition>> constructorMap =
            new ImmutableMap.Builder<String, Supplier<SchedulingCondition>>()
                    .put(AWAIT_JOBS_ACQUIRED_CONDITION, AwaitJobsAcquiredCondition::new)
                    .put(AWAIT_TESTBED_ACQUIRED_CONDITION, AwaitTestbedAcquiredCondition::new)
                    .put(AWAIT_JOBS_ENQUEUE, AwaitJobsEnqueueCondition::new)
                    .put(AWAIT_JOBS_RELEASED_CONDITION, AwaitJobsReleasedCondition::new)
                    .put(AWAIT_TESTBED_RELEASED_CONDITION, AwaitTestbedReleasedCondition::new)
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
        this.lastUpdateTimestamp = Instant.now().toString();
    }

    @Override
    public void update(SchedulingContext context) {
        error = null;
//            this.job = context.getResource();
        var result = updateInternal(context);
        log.debug("Update Condition: {} -> {}", getCondition(), result);

        if (value != result) {
            log.debug("Condition: {} has changed", getCondition());
            lastUpdateTimestamp = Instant.now().toString();
            value = result;
        }
    }

    protected boolean error(String problem) {
        log.error("Condition {} got an error: \n{}\n", getCondition(), problem);
        if (!problem.equals(error)) {
            lastUpdateTimestamp = Instant.now().toString();
        }

        this.error = problem;
        return false;
    }

    protected abstract boolean updateInternal(SchedulingContext client);

    protected boolean checkTimeout(Duration duration) {
        if (lastUpdateTimestamp == null) {
            log.warn("{} checked timeout, but timestamp does not exist", getCondition());
            return false;
        }

        if (Instant.parse(lastUpdateTimestamp).plus(duration).isBefore(Instant.now())) {
            log.warn("{}: timeout exceeded", getCondition());
            return true;
        }

        return false;
    }
}
