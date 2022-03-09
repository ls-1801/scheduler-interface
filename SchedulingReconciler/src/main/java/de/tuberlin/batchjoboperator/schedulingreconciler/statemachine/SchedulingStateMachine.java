package de.tuberlin.batchjoboperator.schedulingreconciler.statemachine;

import com.google.common.collect.Streams;
import de.tuberlin.batchjoboperator.common.Action;
import de.tuberlin.batchjoboperator.common.Condition;
import de.tuberlin.batchjoboperator.common.OnCondition;
import de.tuberlin.batchjoboperator.common.State;
import de.tuberlin.batchjoboperator.common.StateMachine;
import de.tuberlin.batchjoboperator.common.crd.scheduling.JobConditionValue;
import de.tuberlin.batchjoboperator.common.crd.scheduling.SchedulingState;
import lombok.extern.slf4j.Slf4j;

import java.util.Set;
import java.util.stream.Collectors;

import static de.tuberlin.batchjoboperator.schedulingreconciler.statemachine.SchedulingCondition.AWAIT_COMPLETION_CONDITION;
import static de.tuberlin.batchjoboperator.schedulingreconciler.statemachine.SchedulingCondition.AWAIT_EMPTY_JOB_QUEUE;
import static de.tuberlin.batchjoboperator.schedulingreconciler.statemachine.SchedulingCondition.AWAIT_JOBS_ACQUIRED_CONDITION;
import static de.tuberlin.batchjoboperator.schedulingreconciler.statemachine.SchedulingCondition.AWAIT_JOBS_ENQUEUE;
import static de.tuberlin.batchjoboperator.schedulingreconciler.statemachine.SchedulingCondition.AWAIT_JOBS_RELEASED_CONDITION;
import static de.tuberlin.batchjoboperator.schedulingreconciler.statemachine.SchedulingCondition.AWAIT_JOBS_SCHEDULED_CONDITION;
import static de.tuberlin.batchjoboperator.schedulingreconciler.statemachine.SchedulingCondition.AWAIT_JOB_IN_QUEUE;
import static de.tuberlin.batchjoboperator.schedulingreconciler.statemachine.SchedulingCondition.AWAIT_NUMBER_OF_SLOTS_CONDITION;
import static de.tuberlin.batchjoboperator.schedulingreconciler.statemachine.SchedulingCondition.AWAIT_SLOTS_ACQUIRED_CONDITION;
import static de.tuberlin.batchjoboperator.schedulingreconciler.statemachine.SchedulingCondition.AWAIT_SLOTS_AVAILABLE_CONDITION;
import static de.tuberlin.batchjoboperator.schedulingreconciler.statemachine.SchedulingCondition.AWAIT_SLOTS_RELEASED_CONDITION;


@Slf4j
public class SchedulingStateMachine {


    public static final StateMachine<SchedulingContext> STATE_MACHINE = StateMachine.of(
            /*
                AcquireState:
                If acquisition of jobs + slots have already been done AND all Jobs are in the InQueueState the
                scheduling can transition into the SubmissionState.
                If any of the Jobs or Slots require acquisition `acquireSlotsAndJobs` is called and scheduling stays
                int the AcquireState.
                NOTE: Conditions are evaluated in Order!
             */
            State.<SchedulingContext>withName(SchedulingState.AcquireState.name())
                 .condition(OnCondition.any(
                         SchedulingStateMachine::acquireSlotsAndJobs,
                         SchedulingState.AcquireState.name(),
                         AWAIT_JOBS_RELEASED_CONDITION,
                         AWAIT_SLOTS_RELEASED_CONDITION
                 ))
                 .condition(OnCondition.all(
                         log("Jobs and Slots acquired"),
                         SchedulingState.SubmissionState.name(),
                         AWAIT_JOBS_ACQUIRED_CONDITION,
                         AWAIT_SLOTS_ACQUIRED_CONDITION,
                         AWAIT_JOBS_ENQUEUE
                 ))
                 .build(),
            State.<SchedulingContext>anonymous()
                 .condition(OnCondition.any(
                         SchedulingStateMachine::abortScheduling,
                         SchedulingState.AcquireState.name(),
                         AWAIT_JOBS_RELEASED_CONDITION,
                         AWAIT_SLOTS_RELEASED_CONDITION
                 ))
                 .condition(OnCondition.all(
                         SchedulingStateMachine::schedulingCompleted,
                         SchedulingState.CompletedState.name(),
                         AWAIT_COMPLETION_CONDITION
                 ))
                 .subState(State.<SchedulingContext>anonymous()
                                .condition(OnCondition.any(
                                        log("Queue is empty"),
                                        SchedulingState.AwaitingCompletionState.name(),
                                        AWAIT_EMPTY_JOB_QUEUE
                                ))
                                .subState(State.<SchedulingContext>withName(SchedulingState.SubmissionState.name())
                                               .condition(OnCondition.all(
                                                       log("Submitted Jobs where scheduled"),
                                                       SchedulingState.ConfirmationState.name(),
                                                       AWAIT_JOBS_SCHEDULED_CONDITION
                                               ))
                                               .build())
                                .subState(State.<SchedulingContext>withName(SchedulingState.ConfirmationState.name())
                                               .condition(OnCondition.any(
                                                       SchedulingStateMachine::scheduleJobs,
                                                       SchedulingState.SubmissionState.name(),
                                                       AWAIT_SLOTS_AVAILABLE_CONDITION,
                                                       AWAIT_NUMBER_OF_SLOTS_CONDITION
                                               ))
                                               .build())
                                .build())
                 .subState(
                         State.<SchedulingContext>withName(SchedulingState.AwaitingCompletionState.name())
                              .condition(OnCondition.any(
                                      log("Queue was extended"),
                                      SchedulingState.SubmissionState.name(),
                                      AWAIT_JOB_IN_QUEUE
                              ))
                              .build()
                 )
                 .build(),
            State.<SchedulingContext>withName(SchedulingState.CompletedState.name())
                 .condition(OnCondition.all(
                         SchedulingStateMachine::schedulingFinished,
                         SchedulingState.FinishedState.name(),
                         AWAIT_JOBS_RELEASED_CONDITION,
                         AWAIT_SLOTS_RELEASED_CONDITION
                 ))

                 .build(),
            State.<SchedulingContext>withName(SchedulingState.FinishedState.name())
                 .build()
    );

    public static void acquireSlotsAndJobs(Set<Condition<SchedulingContext>> conditions, SchedulingContext context) {
        Action.getConditions(conditions, AwaitJobsReleasedCondition.class).forEach(c -> {
            c.getJobs().stream().filter(JobConditionValue::getValue)
             .forEach(p -> context.acquireJob(p.getName()));
        });

        Action.getConditions(conditions, AwaitSlotsReleasedCondition.class).forEach((c) -> {
            context.acquireSlot(c.getSlotsName());
        });
    }

    public static void scheduleJobs(Set<Condition<SchedulingContext>> conditions, SchedulingContext context) {
        var awaitNumberOfSlots = Action.getConditions(conditions, AwaitNumberOfSlotsAvailableCondition.class).stream()
                                       .map(c -> c.jobName);

        var awaitSlotsAvailable = Action.getConditions(conditions, AwaitSlotsAvailableCondition.class).stream()
                                        .map(c -> c.jobName);

        var jobs = Streams.concat(awaitNumberOfSlots, awaitSlotsAvailable).collect(Collectors.toSet());

        var runnableJobs = context.getStrategy().orderRunnableJobs(jobs);
        var replication = context.getStrategy().getReplication();
        for (var runnableJob : runnableJobs) {
            var slots = context.getStrategy().getSlotsForJob(runnableJob);
            if (slots.size() != replication.get(runnableJob)) {
                log.info("Not enough Slots for {}", runnableJob);
                if (context.getStrategy().allowedToSkipJobs()) {
                    continue;
                }
                break;
            }

            context.submitJob(runnableJob, slots);
        }
    }

    public static void abortScheduling(Set<Condition<SchedulingContext>> conditions, SchedulingContext context) {
        log.info("Job or Slot was released, Abort Scheduling");

    }

    public static void schedulingCompleted(Set<Condition<SchedulingContext>> conditions, SchedulingContext context) {
        log.info("Scheduling has completed");

    }

    public static void schedulingFinished(Set<Condition<SchedulingContext>> conditions, SchedulingContext context) {
        log.info("Scheduling has finished");

    }

    public static void updateQueue(Set<Condition<SchedulingContext>> conditions, SchedulingContext context) {
        log.info("Queue was updated");

    }

    public static Action<SchedulingContext> log(String message) {
        return (Set<Condition<SchedulingContext>> conditions, SchedulingContext context) -> {
            log.info("{}", message);
        };
    }
}
