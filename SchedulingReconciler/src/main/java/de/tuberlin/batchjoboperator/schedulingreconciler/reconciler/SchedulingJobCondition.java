package de.tuberlin.batchjoboperator.schedulingreconciler.reconciler;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import de.tuberlin.batchjoboperator.common.NamespacedName;
import de.tuberlin.batchjoboperator.common.crd.batchjob.BatchJob;
import de.tuberlin.batchjoboperator.common.crd.scheduling.AbstractSchedulingJobCondition;
import io.fabric8.kubernetes.client.KubernetesClient;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Objects;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "condition")
@JsonSubTypes({
        @JsonSubTypes.Type(value = AwaitingJobSubmissionCondition.class, name =
                SchedulingJobCondition.AWAITING_JOB_SUBMISSION),
        @JsonSubTypes.Type(value = AwaitNumberOfSlotsAvailableCondition.class, name =
                SchedulingJobCondition.AWAITING_NUMBER_OF_SLOTS_AVAILABLE),
        @JsonSubTypes.Type(value = AwaitSlotsAvailableCondition.class, name =
                SchedulingJobCondition.AWAITING_SLOTS_AVAILABLE),
        @JsonSubTypes.Type(value = AwaitingJobCompletionCondition.class, name =
                SchedulingJobCondition.AWAITING_JOB_COMPLETION),
        @JsonSubTypes.Type(value = AwaitingJobEnqueueCondition.class, name =
                SchedulingJobCondition.AWAITING_JOB_ENQUEUE),
        @JsonSubTypes.Type(value = AwaitPreviousJobSubmittedCondition.class, name =
                SchedulingJobCondition.AWAITING_PRECEDING_JOB_SUBMISSION),
        @JsonSubTypes.Type(value = AwaitingJobStartCondition.class, name = SchedulingJobCondition.AWAITING_JOB_START)}
)
@NoArgsConstructor
public abstract class SchedulingJobCondition extends AbstractSchedulingJobCondition {

    public static final String AWAITING_JOB_SUBMISSION = "AwaitingJobSubmission";
    public static final String AWAITING_NUMBER_OF_SLOTS_AVAILABLE = "AwaitingNumberOfSlotsAvailable";
    public static final String AWAITING_SLOTS_AVAILABLE = "AwaitingSlotsAvailable";
    public static final String AWAITING_JOB_START = "AwaitingJobStart";
    public static final String AWAITING_JOB_COMPLETION = "AwaitingJobCompletion";
    public static final String AWAITING_PRECEDING_JOB_SUBMISSION = "AwaitingPreviousJobSubmission";
    public static final String AWAITING_JOB_ENQUEUE = "AwaitingJobEnqueue";

    public SchedulingJobCondition(NamespacedName name) {
        super();
        this.name = name;
    }

    public SchedulingJobCondition update(KubernetesClient client) {
        var job = client.resources(BatchJob.class).inNamespace(name.getNamespace()).withName(name.getName()).get();
        // value is false if job was deleted
        error = null;
        if (job == null || value != updateInternal(client, job)) {
            value = job != null && !value;
            lastUpdateTimestamp = LocalDateTime.now().toString();
        }
        return this;
    }

    protected boolean error(String problem) {
        this.error = problem;
        return false;
    }

    abstract boolean updateInternal(KubernetesClient client, BatchJob name);

    public boolean preventRunnable() {
        return false;
    }

    public boolean preventSubmission() {
        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AbstractSchedulingJobCondition that = (AbstractSchedulingJobCondition) o;
        return getCondition().equals(that.getCondition()) && getName().equals(that.getName());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getCondition(), getName());
    }
}
