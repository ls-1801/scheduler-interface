package de.tuberlin.esi.schedulingreconciler.statemachine;

import com.google.common.collect.Streams;
import de.tuberlin.esi.common.crd.scheduling.JobConditionValue;
import de.tuberlin.esi.common.crd.scheduling.SchedulingState;
import de.tuberlin.esi.common.statemachine.AbortStateMachineSilentlyException;
import de.tuberlin.esi.common.statemachine.Action;
import de.tuberlin.esi.common.statemachine.Condition;
import de.tuberlin.esi.common.statemachine.OnCondition;
import de.tuberlin.esi.common.statemachine.State;
import de.tuberlin.esi.common.statemachine.StateMachine;
import lombok.extern.slf4j.Slf4j;

import java.util.Set;
import java.util.stream.Collectors;

import static de.tuberlin.esi.common.statemachine.Action.getConditions;
import static de.tuberlin.esi.schedulingreconciler.statemachine.SchedulingCondition.AWAIT_COMPLETION_CONDITION;
import static de.tuberlin.esi.schedulingreconciler.statemachine.SchedulingCondition.AWAIT_EMPTY_JOB_QUEUE;
import static de.tuberlin.esi.schedulingreconciler.statemachine.SchedulingCondition.AWAIT_JOBS_ACQUIRED_CONDITION;
import static de.tuberlin.esi.schedulingreconciler.statemachine.SchedulingCondition.AWAIT_JOBS_ENQUEUE;
import static de.tuberlin.esi.schedulingreconciler.statemachine.SchedulingCondition.AWAIT_JOBS_RELEASED_CONDITION;
import static de.tuberlin.esi.schedulingreconciler.statemachine.SchedulingCondition.AWAIT_JOBS_SCHEDULED_CONDITION;
import static de.tuberlin.esi.schedulingreconciler.statemachine.SchedulingCondition.AWAIT_JOB_IN_QUEUE;
import static de.tuberlin.esi.schedulingreconciler.statemachine.SchedulingCondition.AWAIT_NUMBER_OF_SLOTS_CONDITION;
import static de.tuberlin.esi.schedulingreconciler.statemachine.SchedulingCondition.AWAIT_SLOTS_AVAILABLE_CONDITION;
import static de.tuberlin.esi.schedulingreconciler.statemachine.SchedulingCondition.AWAIT_TESTBED_ACQUIRED_CONDITION;
import static de.tuberlin.esi.schedulingreconciler.statemachine.SchedulingCondition.AWAIT_TESTBED_RELEASED_CONDITION;


@Slf4j
public class SchedulingStateMachine {


    public static final StateMachine<SchedulingContext> STATE_MACHINE = StateMachine.of(
            /*
                AcquireState:
                If acquisition of jobs + slots have already been done AND all Jobs are in the InQueueState the
                scheduling can transition into the ConfirmationState.
                If any of the Jobs or Slots require acquisition `acquireSlotsAndJobs` is called and scheduling stays
                int the AcquireState.
                NOTE: Conditions are evaluated in Order!
             */
            State.<SchedulingContext>withName(SchedulingState.AcquireState.name())
                 .condition(OnCondition.any(
                         SchedulingStateMachine::acquireSlotsAndJobs,
                         SchedulingState.AcquireState.name(),
                         AWAIT_JOBS_RELEASED_CONDITION,
                         AWAIT_TESTBED_RELEASED_CONDITION
                 ))
                 // All Jobs acquired Move to the ConfirmationState
                 .condition(OnCondition.all(
                         SchedulingStateMachine::logInQueueEvents,
                         SchedulingState.ConfirmationState.name(),
                         AWAIT_JOBS_ACQUIRED_CONDITION,
                         AWAIT_TESTBED_ACQUIRED_CONDITION,
                         AWAIT_JOBS_ENQUEUE
                 ))
                 .build(),
            // Claimed State.
            State.<SchedulingContext>anonymous()
                 // This either happens if a job claimed by the current scheduling, loses its active scheduled.
                 // (which should only happen if done manually), or if the scheduling was extended. In the case of an
                 // extension the job needs to be claimed.
                 // TODO: If the Slot is released the scheduling should probably stop, as there is currently no
                 //  plausible reason, why that should happen.
                 .condition(OnCondition.any(
                         SchedulingStateMachine::reaquireJobs,
                         SchedulingState.AcquireState.name(),
                         AWAIT_JOBS_RELEASED_CONDITION,
                         AWAIT_TESTBED_RELEASED_CONDITION
                 ))
                 // Move into the Completed State once ALL jobs are finished
                 .condition(OnCondition.all(
                         SchedulingStateMachine::schedulingCompleted,
                         SchedulingState.CompletedState.name(),
                         AWAIT_COMPLETION_CONDITION
                 ))
                 // Running State. Jobs are in the Queue and need scheduling
                 .subState(State.<SchedulingContext>anonymous()
                                // Once no job requires scheduling, move into the AwaitCompletionState
                                .condition(OnCondition.any(
                                        log("Queue is empty"),
                                        SchedulingState.AwaitingCompletionState.name(),
                                        AWAIT_EMPTY_JOB_QUEUE
                                ))
                                // Confirmation State awaits previous jobs from being scheduled
                                // This is the case if we scheduled jobs in a previous cycle, but they have not yet
                                // been confirmed by the TestBeds Slot Status. The condition waits until all pods
                                // associated with the Jobs Application are scheduled and the TestBed has updated its
                                // state.
                                .subState(State.<SchedulingContext>withName(SchedulingState.ConfirmationState.name())
                                               .condition(OnCondition.all(
                                                       SchedulingStateMachine::logScheduledEvents,
                                                       SchedulingState.SubmissionState.name(),
                                                       AWAIT_JOBS_SCHEDULED_CONDITION
                                               ))
                                               .build())
                                // Once the TestBed confirms previous scheduling jobs can be scheduled again.
                                // The underlying Scheduling Strategy creates conditions for new jobs to be scheduled.
                                // Once any of the conditions becomes true jobs are scheduled and move back into
                                // the confirmation state, unless the queue becomes empty.
                                .subState(State.<SchedulingContext>withName(SchedulingState.SubmissionState.name())
                                               .condition(OnCondition.any(
                                                       SchedulingStateMachine::scheduleJobs,
                                                       SchedulingState.ConfirmationState.name(),
                                                       AWAIT_SLOTS_AVAILABLE_CONDITION,
                                                       AWAIT_NUMBER_OF_SLOTS_CONDITION
                                               ))
                                               .build())
                                .build())
                 .subState(
                         // If no more jobs need to be scheduled, but previously scheduled jobs are still running, the
                         // scheduling remains in the AwaitingCompletionState. Once all Jobs are completed, move into
                         // the CompletedState
                         State.<SchedulingContext>withName(SchedulingState.AwaitingCompletionState.name())
                              .condition(OnCondition.any(
                                      log("Queue was extended"),
                                      SchedulingState.ConfirmationState.name(),
                                      AWAIT_JOB_IN_QUEUE
                              ))
                              .build()
                 )
                 .build(),
            // All Jobs are finished, but still claimed. This state exists because the external Scheduler might, want
            // to update Jobs before they are released
            State.<SchedulingContext>withName(SchedulingState.CompletedState.name())
                 .condition(OnCondition.all(
                         SchedulingStateMachine::schedulingFinished,
                         SchedulingState.FinishedState.name(),
                         AWAIT_JOBS_RELEASED_CONDITION,
                         AWAIT_TESTBED_RELEASED_CONDITION
                 ))

                 .build(),
            // Once all Jobs and Slots are released the Scheduling has Finished
            State.<SchedulingContext>withName(SchedulingState.FinishedState.name())
                 .build()
    );

    public static void acquireSlotsAndJobs(Set<Condition<SchedulingContext>> conditions, SchedulingContext context) {
        try {
            getConditions(conditions, AwaitJobsReleasedCondition.class).forEach(c -> {
                c.getJobs().stream().filter(JobConditionValue::getValue)
                 .forEach(p -> context.acquireJob(p.getName()));
            });

            getConditions(conditions, AwaitTestbedReleasedCondition.class).forEach((c) -> {
                context.acquireSlot(c.getSlotsName());
            });
        } catch (ClaimedByAnotherSchedulingException e) {
            log.warn(e.getMessage());
            // rollback
            getConditions(conditions, AwaitJobsReleasedCondition.class).forEach(c -> {
                c.getJobs().stream().filter(JobConditionValue::getValue)
                 .forEach(p -> context.releaseJobIfClaimed(p.getName()));
            });

            getConditions(conditions, AwaitTestbedReleasedCondition.class).forEach((c) -> {
                context.releaseTestbedIfClaimed(c.getSlotsName());
            });

            AbortStateMachineSilentlyException.abort(e);
        }

    }

    public static void scheduleJobs(Set<Condition<SchedulingContext>> conditions, SchedulingContext context) {
        // Query Conditions for Jobs that can be scheduled
        var awaitNumberOfSlots = getConditions(conditions, AwaitNumberOfSlotsAvailableCondition.class).stream()
                                                                                                      .map(c -> c.jobName);
        var awaitSlotsAvailable = getConditions(conditions, AwaitSlotsAvailableCondition.class).stream()
                                                                                               .map(c -> c.jobName);
        var jobs = Streams.concat(awaitNumberOfSlots, awaitSlotsAvailable).collect(Collectors.toSet());

        // Strategy needs to order Jobs
        var runnableJobs = context.getStrategy().orderRunnableJobs(jobs);
        // Replication is also strategy defined.
        // (QueueBased counts the occurrence, whereas SlotBased the number of slots)
        var replication = context.getStrategy().getReplication();

        for (var runnableJob : runnableJobs) {
            var slots = context.getStrategy().getSlotsForJob(runnableJob);
            if (slots.size() != replication.get(runnableJob)) {
                log.info("Not enough Slots for {}", runnableJob);
                // If the strategy does not require a strict scheduled, jobs that cannot be scheduled during this
                // cycle can be skipped
                if (context.getStrategy().allowedToSkipJobs()) {
                    continue;
                }
                // Otherwise, break out of the loop as no more jobs can be scheduled
                break;
            }

            log.info("Submit Job {} to Slots: {}", runnableJob, slots);

            context.submitJob(runnableJob, slots);
        }

        if (context.getJobsSubmittedDuringCurrentCycle().isEmpty() && context.schedulingRetryCounterIncAndGet() > 2) {
            // No job was scheduled during this cycle, this is the case if any AwaitSlotsAvailableCondition is
            // fulfilled, but no job can be scheduled. The StateMachine will continue spinning, since the conditions
            // is most-likely bad :(.
            throw new SchedulingKeepsFailingException();
        }
    }

    public static void reaquireJobs(Set<Condition<SchedulingContext>> conditions, SchedulingContext context) {
        log.info("Job or Slot was released, Abort Scheduling");

    }

    public static void schedulingCompleted(Set<Condition<SchedulingContext>> conditions, SchedulingContext context) {
        log.info("Scheduling has completed");

    }

    public static void schedulingFinished(Set<Condition<SchedulingContext>> conditions, SchedulingContext context) {
        log.info("Scheduling has finished");
    }

    public static Action<SchedulingContext> log(String message) {
        return (Set<Condition<SchedulingContext>> conditions, SchedulingContext context) -> {
            log.info("{}", message);
        };
    }

    public static void logInQueueEvents(Set<Condition<SchedulingContext>> conditions, SchedulingContext context) {
        var enqueue = getConditions(conditions, AwaitJobsEnqueueCondition.class);
        enqueue.forEach(c ->
                c.getJobs().forEach(j -> context.jobInQueueEvent(j.getName()))
        );
    }

    public static void logScheduledEvents(Set<Condition<SchedulingContext>> conditions, SchedulingContext context) {
        var enqueue = getConditions(conditions, AwaitJobsScheduledCondition.class);
        enqueue.forEach(c ->
                c.getJobs().forEach(j -> context.jobScheduledEvent(j.getName()))
        );
    }
}
