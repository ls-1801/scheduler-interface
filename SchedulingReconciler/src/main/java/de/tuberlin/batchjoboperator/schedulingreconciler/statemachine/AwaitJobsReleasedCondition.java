package de.tuberlin.batchjoboperator.schedulingreconciler.statemachine;

import de.tuberlin.batchjoboperator.common.NamespacedName;
import de.tuberlin.batchjoboperator.common.crd.scheduling.JobConditionValue;
import lombok.Getter;
import lombok.Setter;

import javax.annotation.Nonnull;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static de.tuberlin.batchjoboperator.common.constants.SchedulingConstants.ACTIVE_SCHEDULING_LABEL_NAME;
import static de.tuberlin.batchjoboperator.common.constants.SchedulingConstants.ACTIVE_SCHEDULING_LABEL_NAMESPACE;
import static de.tuberlin.batchjoboperator.common.util.General.getNullSafe;

public class AwaitJobsReleasedCondition extends SchedulingCondition {

    public static final String condition = AWAIT_JOBS_RELEASED_CONDITION;

    @Getter
    @Setter
    private Set<JobConditionValue> jobs;

    @Override
    public String getCondition() {
        return AWAIT_JOBS_RELEASED_CONDITION;
    }

    @Override
    protected boolean updateInternal(SchedulingContext context) {
        var newJobs = context.getAllJobs();
        Objects.requireNonNull(this.jobs)
               .addAll(newJobs.stream().map(n -> new JobConditionValue(n, false)).collect(Collectors.toSet()));

        this.jobs = this.jobs.stream()
                             .map(p -> isReleased(context, p))
                             .collect(Collectors.toSet());

        return this.jobs.stream().anyMatch(JobConditionValue::getValue);
    }

    @Nonnull
    private JobConditionValue isReleased(SchedulingContext context, JobConditionValue p) {
        var activeSchedulingName = getNullSafe(() -> {
            var job = context.getJob(p.getName());
            var name = job.getMetadata().getLabels().get(ACTIVE_SCHEDULING_LABEL_NAME);
            var namespace =
                    job.getMetadata().getLabels().get(ACTIVE_SCHEDULING_LABEL_NAMESPACE);

            return new NamespacedName(name, namespace);
        });

        var isNotActive = activeSchedulingName
                .map(activeScheduling -> !activeScheduling.equals(NamespacedName.of(context.getResource())))
                .orElse(true);

        return new JobConditionValue(p.getName(), isNotActive);
    }


    @Override
    public void initialize(SchedulingContext context) {
        super.initialize(context);
        this.jobs = context.getAllJobs().stream().map(n -> new JobConditionValue(n, false)).collect(Collectors.toSet());
    }
}

