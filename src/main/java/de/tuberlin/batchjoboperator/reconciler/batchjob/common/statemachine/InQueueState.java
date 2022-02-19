package de.tuberlin.batchjoboperator.reconciler.batchjob.common.statemachine;

import de.tuberlin.batchjoboperator.crd.batchjob.BatchJob;
import de.tuberlin.batchjoboperator.crd.batchjob.BatchJobState;
import io.javaoperatorsdk.operator.api.reconciler.UpdateControl;

public class InQueueState extends AbstractState {

    @Override
    public UpdateControl<BatchJob> onFailed(Context context, Action action) {
        return action.updateState(BatchJobState.FailedSubmissionState);
    }

    @Override
    public UpdateControl<BatchJob> onScheduled(Context context, Action action) {
        return action.updateState(BatchJobState.SubmittedState);
    }


    public UpdateControl<BatchJob> onCompleted(Context context, Action action) {
        return action.deleteApplication();
    }

    @Override
    public UpdateControl<BatchJob> onNoApplication(Context context, Action action) {
        return action.noChange();
    }
}
