package de.tuberlin.batchjoboperator.reconciler.batchjob.common.statemachine;

import de.tuberlin.batchjoboperator.crd.batchjob.BatchJob;
import de.tuberlin.batchjoboperator.crd.batchjob.BatchJobState;
import io.javaoperatorsdk.operator.api.reconciler.UpdateControl;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class SubmittedState extends AbstractState {
    @Override
    public UpdateControl<BatchJob> onNoApplication(Context context, Action action) {
        return action.updateState(BatchJobState.InQueueState);
    }

    @Override
    public UpdateControl<BatchJob> onRunning(Context context, Action action) {
        return action.updateState(BatchJobState.RunningState);
    }

    @Override
    public UpdateControl<BatchJob> onScheduled(Context context, Action action) {
        log.info("Received multiple on schedule Events: probably application operator did something");
        return action.noChange();
    }
}