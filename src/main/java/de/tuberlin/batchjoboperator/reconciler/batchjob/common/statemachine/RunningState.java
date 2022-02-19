package de.tuberlin.batchjoboperator.reconciler.batchjob.common.statemachine;

import de.tuberlin.batchjoboperator.crd.batchjob.BatchJob;
import de.tuberlin.batchjoboperator.crd.batchjob.BatchJobState;
import io.javaoperatorsdk.operator.api.reconciler.UpdateControl;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class RunningState extends AbstractState {

    @Override
    public UpdateControl<BatchJob> onCompleted(Context context, Action action) {
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
        action.removeTaintFromNode();

        log.info("Received Running Event: probably underlying pod changes from the Application Operator");
        return UpdateControl.noUpdate();
    }
}
