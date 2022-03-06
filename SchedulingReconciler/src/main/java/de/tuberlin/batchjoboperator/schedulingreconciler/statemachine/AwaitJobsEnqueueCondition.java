package de.tuberlin.batchjoboperator.schedulingreconciler.statemachine;

import de.tuberlin.batchjoboperator.common.crd.batchjob.BatchJobState;
import de.tuberlin.batchjoboperator.common.crd.scheduling.JobConditionValue;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
public class AwaitJobsEnqueueCondition extends SchedulingCondition {

    public static final String condition = AWAIT_JOBS_ENQUEUE;

    @Getter
    @Setter
    private Set<JobConditionValue> jobs;

    @Override
    public String getCondition() {
        return AWAIT_JOBS_ENQUEUE;
    }

    @Override
    protected boolean updateInternal(SchedulingContext context) {
        this.jobs = context.getAllJobs().stream()
                           .filter(job -> !context.getJobsSubmittedDuringCurrentCycle().contains(job))
                           .filter(job -> !context.getAlreadyScheduledJobs().contains(job))
                           .map(job -> new JobConditionValue(job, false))
                           .collect(Collectors.toSet());

        this.jobs = this.jobs.stream()
                             .map(p -> {
                                 var job = context.getJob(p.getName());
                                 var isActive = job.getStatus().getState() == BatchJobState.InQueueState;
                                 log.debug("Test if {} is in the InQueueState: {} ({})",
                                         p.getName(), isActive, job.getStatus().getState());

                                 return new JobConditionValue(p.getName(), isActive);
                             })
                             .collect(Collectors.toSet());

        return this.jobs.stream().allMatch(JobConditionValue::getValue);
    }


    @Override
    public void initialize(SchedulingContext context) {
        super.initialize(context);
        this.jobs = context.getAllJobs().stream().map(n -> new JobConditionValue(n, false)).collect(Collectors.toSet());
    }
}