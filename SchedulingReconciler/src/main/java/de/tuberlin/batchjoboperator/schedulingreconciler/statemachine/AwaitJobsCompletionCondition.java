package de.tuberlin.batchjoboperator.schedulingreconciler.statemachine;

import de.tuberlin.batchjoboperator.common.NamespacedName;
import de.tuberlin.batchjoboperator.common.crd.batchjob.BatchJobState;
import de.tuberlin.batchjoboperator.common.crd.scheduling.JobConditionValue;
import lombok.Getter;
import lombok.Setter;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static de.tuberlin.batchjoboperator.common.util.General.getNullSafe;

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
        var newJobs = context.getAllJobs();
        Objects.requireNonNull(this.jobs)
               .addAll(newJobs.stream().map(n -> new JobConditionValue(n, false)).collect(Collectors.toSet()));


        this.jobs = this.jobs.stream()
                             .map(p -> {
                                 return new JobConditionValue(p.getName(), getNullSafe(() -> {
                                     return context.getJob(p.getName()).getStatus()
                                                   .getState() == BatchJobState.CompletedState;
                                 }).orElse(null));
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

        // Mark Jobs as completed
        this.jobs.stream().filter(JobConditionValue::getValue).forEach(j -> context.jobCompletedEvent(j.getName()));

        return this.jobs.stream()
                        .allMatch(JobConditionValue::getValue);
    }


    @Override
    public void initialize(SchedulingContext context) {
        super.initialize(context);
        var jobs = context.getAllJobs();
        this.jobs = jobs.stream().map(n -> new JobConditionValue(n, false)).collect(Collectors.toSet());
    }
}