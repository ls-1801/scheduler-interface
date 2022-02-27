package de.tuberlin.batchjoboperator.reconciler.batchjob.common.statemachine;

import de.tuberlin.batchjoboperator.crd.batchjob.BatchJob;
import de.tuberlin.batchjoboperator.crd.batchjob.BatchJobState;
import io.javaoperatorsdk.operator.api.reconciler.UpdateControl;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RunningState extends AbstractState {

    @Override
    public UpdateControl<BatchJob> onCompleted(Context context, Action action) {
        context.getResource().getStatus().stopLatestSchedulingEvent(true);

        if (context.getResource().getSpec().isRequeue()) {
            action.deleteApplication();
            return action.updateState(BatchJobState.InQueueState);
        }

        return action.updateState(BatchJobState.CompletedState);
    }

    @Override
    public UpdateControl<BatchJob> onNoApplication(Context context, Action action) {
        return action.updateState(BatchJobState.InQueueState);
    }

    @Override
    public UpdateControl<BatchJob> onRunning(Context context, Action action) {
        log.info("Received Running Event: probably underlying pod changes from the Application Operator");
        return UpdateControl.noUpdate();
    }
}
