package de.tuberlin.esi.schedulingreconciler.statemachine;

import de.tuberlin.esi.common.crd.NamespacedName;
import de.tuberlin.esi.common.crd.batchjob.BatchJobState;
import de.tuberlin.esi.common.crd.scheduling.JobConditionValue;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nonnull;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static de.tuberlin.esi.common.util.General.getNullSafe;

@Slf4j
public class AwaitJobsCompletionCondition extends SchedulingCondition {

    public static final String condition = AWAIT_COMPLETION_CONDITION;

    @Getter
    @Setter
    private Set<JobConditionValue> jobs;

    @Override
    public String getCondition() {
        return AWAIT_COMPLETION_CONDITION;
    }

    @Override
    protected boolean updateInternal(SchedulingContext context) {
        var newJobs = context.getAlreadyScheduledJobs();
        Objects.requireNonNull(this.jobs)
               .addAll(newJobs.stream().map(n -> new JobConditionValue(n, false))
                              .collect(Collectors.toSet()));


        this.jobs = this.jobs.stream()
                             .map(p -> isJobCompleted(context, p))
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

        // Mark Jobs as completed
        this.jobs.stream().filter(JobConditionValue::getValue)
                 .forEach(j -> context.jobCompletedEvent(j.getName()));

        return this.jobs.stream()
                        .allMatch(JobConditionValue::getValue);
    }

    @Nonnull
    private JobConditionValue isJobCompleted(SchedulingContext context, JobConditionValue p) {
        return new JobConditionValue(p.getName(), getNullSafe(() ->
                context.getJob(p.getName()).getStatus().getState() == BatchJobState.CompletedState)
                .orElse(null));
    }


    @Override
    public void initialize(SchedulingContext context) {
        super.initialize(context);
        var allJobs = context.getAllJobs();
        this.jobs = allJobs.stream().map(n -> new JobConditionValue(n, false)).collect(Collectors.toSet());
    }
}
