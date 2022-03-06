package de.tuberlin.batchjoboperator.batchjobreconciler.reconciler.conditions;

import com.fasterxml.jackson.annotation.JsonIgnore;
import de.tuberlin.batchjoboperator.batchjobreconciler.reconciler.BatchJobContext;
import de.tuberlin.batchjoboperator.common.Condition;
import de.tuberlin.batchjoboperator.common.NamespacedName;
import de.tuberlin.batchjoboperator.common.crd.batchjob.AbstractBatchJobCondition;
import de.tuberlin.batchjoboperator.common.crd.batchjob.BatchJob;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

@NoArgsConstructor
@Slf4j
public abstract class BatchJobCondition extends AbstractBatchJobCondition implements Condition<BatchJobContext> {

    public static final String AWAIT_CREATION_REQUEST_CONDITION = "AWAIT_CREATION_REQUEST_CONDITION";
    public static final String AWAIT_ENQUEUE_REQUEST_CONDITION = "AWAIT_ENQUEUE_REQUEST_CONDITION";
    public static final String AWAIT_RELEASE_CONDITION = "AWAIT_RELEASE_CONDITION";
    public static final String AWAIT_RUNNING_CONDITION = "AWAIT_RUNNING_CONDITION";
    public static final String AWAIT_COMPLETION_CONDITION = "AWAIT_COMPLETION_CONDITION";
    public static final String AWAIT_DELETION_CONDITION = "AWAIT_DELETION";
    public static final String AWAIT_POD_SCHEDULED_CONDITION = "AWAIT_POD_SCHEDULED_CONDITION";
    public static final Map<String, Supplier<BatchJobCondition>> constructors = Map.of(
            AWAIT_ENQUEUE_REQUEST_CONDITION, AwaitEnqueueRequest::new,
            AWAIT_CREATION_REQUEST_CONDITION, AwaitCreationRequest::new,
            AWAIT_RELEASE_CONDITION, AwaitReleaseCondition::new,
            AWAIT_COMPLETION_CONDITION, AwaitCompletionCondition::new,
            AWAIT_POD_SCHEDULED_CONDITION, AwaitPodScheduledCondition::new,
            AWAIT_RUNNING_CONDITION, AwaitRunningCondition::new,
            AWAIT_DELETION_CONDITION, AwaitingDeletionCondition::new
    );
    @JsonIgnore
    protected BatchJob job;

    public abstract String getCondition();

    @Override
    public void initialize(BatchJobContext context) {
        this.name = NamespacedName.of(context.getResource());
        this.job = context.getResource();
    }

    @Override
    public void update(BatchJobContext context) {
        error = null;
        this.job = context.getResource();
        var result = updateInternal(context);
        log.debug("Update Condition: {} -> {}", getCondition(), result);

        if (value != result) {
            log.debug("Condition: {} has changed", getCondition());
            lastUpdateTimestamp = LocalDateTime.now().toString();
            value = result;
        }
    }

    protected boolean error(String problem) {
        log.error("Condition {} got an error: \n{}\n", getCondition(), problem);
        if (!problem.equals(error)) {
            lastUpdateTimestamp = LocalDateTime.now().toString();
        }

        this.error = problem;
        return false;
    }

    protected abstract boolean updateInternal(BatchJobContext client);


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AbstractBatchJobCondition that = (AbstractBatchJobCondition) o;
        return getCondition().equals(that.getCondition()) && getName().equals(that.getName());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getCondition(), getName());
    }
}

