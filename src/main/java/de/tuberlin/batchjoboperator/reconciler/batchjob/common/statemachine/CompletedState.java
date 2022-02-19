package de.tuberlin.batchjoboperator.reconciler.batchjob.common.statemachine;

import de.tuberlin.batchjoboperator.crd.batchjob.BatchJob;
import de.tuberlin.batchjoboperator.crd.batchjob.BatchJobState;
import io.javaoperatorsdk.operator.api.reconciler.UpdateControl;

public class CompletedState extends AbstractState {

    @Override
    public UpdateControl<BatchJob> onAnyChange(Context context, Action action) {
        if (context.getResource().getSpec().isRequeue()) {
            return action.updateState(BatchJobState.InQueueState);
        }

        return action.noChange();
    }
}
