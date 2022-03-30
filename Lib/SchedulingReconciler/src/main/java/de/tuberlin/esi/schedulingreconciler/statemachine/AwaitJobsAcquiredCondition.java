package de.tuberlin.esi.schedulingreconciler.statemachine;

import de.tuberlin.esi.common.crd.NamespacedName;
import de.tuberlin.esi.common.crd.scheduling.JobConditionValue;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;
import java.util.stream.Collectors;

import static de.tuberlin.esi.common.util.General.getNullSafe;

public class AwaitJobsAcquiredCondition extends SchedulingCondition {

    public static final String condition = AWAIT_JOBS_ACQUIRED_CONDITION;

    @Getter
    @Setter
    private Set<JobConditionValue> jobs;

    @Override
    public String getCondition() {
        return AWAIT_JOBS_ACQUIRED_CONDITION;
    }

    @Override
    protected boolean updateInternal(SchedulingContext context) {

        this.jobs = this.jobs.stream()
                             .map(p -> {
                                 var activeSchedulingName =
                                         getNullSafe(() -> context.getJob(p.getName()).getSpec().getActiveScheduling());

                                 var isActive = activeSchedulingName
                                         .map(activeScheduling -> activeScheduling.equals(NamespacedName.of(context.getResource())))
                                         .orElse(false);

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

