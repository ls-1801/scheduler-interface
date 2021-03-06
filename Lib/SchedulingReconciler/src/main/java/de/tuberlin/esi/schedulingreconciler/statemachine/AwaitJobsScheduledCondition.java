package de.tuberlin.esi.schedulingreconciler.statemachine;

import de.tuberlin.esi.common.crd.NamespacedName;
import de.tuberlin.esi.common.crd.batchjob.BatchJobState;
import de.tuberlin.esi.common.crd.scheduling.JobConditionValue;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static de.tuberlin.esi.common.util.General.getNullSafe;

@Slf4j
public class AwaitJobsScheduledCondition extends SchedulingCondition {

    public static final String condition = AWAIT_JOBS_SCHEDULED_CONDITION;


    private static final Set<BatchJobState> definitelyPastScheduledStates = Set.of(
            BatchJobState.RunningState,
            BatchJobState.CompletedState
    );

    private static final Set<BatchJobState> scheduledStates = Set.of(
            BatchJobState.ScheduledState
    );


    @Getter
    @Setter
    private Set<JobConditionValue> jobs;

    @Override
    public String getCondition() {
        return AWAIT_JOBS_SCHEDULED_CONDITION;
    }

    private boolean isJobScheduled(JobConditionValue jobValue, SchedulingContext context) {
        var job = context.getJob(jobValue.getName());
        log.debug("Job {} State {}", jobValue.getName(), job.getStatus().getState());

        // Once a Job is Running or even Completed, Slot State is guaranteed to be updated. Checking it again might,
        //even show them being free again as the job may have already completed
        if (definitelyPastScheduledStates.contains(job.getStatus().getState())) {
            return true;
        }

        if (!scheduledStates.contains(job.getStatus().getState())) {
            return false;
        }


        // This is required, since the slots may have not been updated even though
        // the BatchJob is in the scheduled state
        var freeSlots = context.getFreeSlots();
        log.debug("FreeSlots: {}", freeSlots);
        var desiredSlots = getNullSafe(() -> job.getSpec().getCreationRequest().getSlotIds())
                .orElseThrow(() -> new RuntimeException("Missing SlotIds in CreationRequest"));


        log.debug("Jobs should haven been scheduled onto slots: {}", desiredSlots);
        var slotsNotUpdated = desiredSlots.stream()
                                          .anyMatch(freeSlots::contains);

        return !slotsNotUpdated;
    }

    @Override
    protected boolean updateInternal(SchedulingContext context) {
        if (checkTimeout(Duration.of(2, ChronoUnit.MINUTES))) {
            return error("Timeout Exceeded when waiting for jobs to be scheduled");
        }

        var newJobs = context.getJobsSubmittedDuringCurrentCycle();
        Objects.requireNonNull(this.jobs)
               .addAll(newJobs.stream().map(n -> new JobConditionValue(n, false)).collect(Collectors.toSet()));

        log.debug("Testing if all jobs have been scheduled: {}", this.jobs);
        this.jobs = this.jobs.stream()
                             .map(job -> {
                                 return new JobConditionValue(
                                         job.getName(),
                                         getNullSafe(() -> isJobScheduled(job, context)).orElse(null));
                             })
                             .collect(Collectors.toSet());


        var badStatus =
                this.jobs.stream().filter(p -> p.getValue() == null).map(JobConditionValue::getName)
                         .collect(Collectors.toSet());

        if (!badStatus.isEmpty()) {
            var jobNames = badStatus.stream()
                                    .map(NamespacedName::toString)
                                    .collect(Collectors.joining(","));
            return error("Could not determine for Jobs: " + jobNames);
        }

        // Mark Jobs as scheduled
        this.jobs.stream().filter(JobConditionValue::getValue).forEach(j -> context.jobScheduledEvent(j.getName()));

        return this.jobs.stream().allMatch(JobConditionValue::getValue);
    }


    @Override
    public void initialize(SchedulingContext context) {
        super.initialize(context);
        var newJobsSubmitted = context.getJobsSubmittedDuringCurrentCycle();
        this.jobs = newJobsSubmitted.stream().map(n -> new JobConditionValue(n, false)).collect(Collectors.toSet());
    }
}
