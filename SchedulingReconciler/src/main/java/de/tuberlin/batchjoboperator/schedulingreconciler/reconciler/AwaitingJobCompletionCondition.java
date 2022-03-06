package de.tuberlin.batchjoboperator.schedulingreconciler.reconciler;

import de.tuberlin.batchjoboperator.common.NamespacedName;
import de.tuberlin.batchjoboperator.common.crd.batchjob.BatchJob;
import de.tuberlin.batchjoboperator.common.crd.batchjob.BatchJobState;
import io.fabric8.kubernetes.client.KubernetesClient;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Set;

@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class AwaitingJobCompletionCondition extends SchedulingJobCondition {
    private static final Set<BatchJobState> jobSubmittedState =
            Set.of(
                    BatchJobState.CompletedState
            );
    @Getter
    private final String condition = AWAITING_JOB_COMPLETION;

    public AwaitingJobCompletionCondition(NamespacedName name) {
        super(name);
    }

    @Override
    boolean updateInternal(KubernetesClient client, BatchJob job) {
        return jobSubmittedState.contains(job.getStatus().getState());
    }
}