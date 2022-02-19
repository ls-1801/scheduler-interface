package de.tuberlin.batchjoboperator.reconciler.batchjob.common.statemachine;

import de.tuberlin.batchjoboperator.crd.batchjob.BatchJob;
import de.tuberlin.batchjoboperator.crd.batchjob.BatchJobState;
import io.javaoperatorsdk.operator.api.reconciler.UpdateControl;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class FailedState extends AbstractState {
    @Override
    public UpdateControl<BatchJob> onAnyChange(Context context, Action action) {
        log.info("{} while in Failed State", context.getEvent());
        return action.noChange();
    }

    @Override
    public UpdateControl<BatchJob> onNoApplication(Context context, Action action) {
        log.info("Moving from Failed state back to In Queue state after Application has been removed");
        return action.updateState(BatchJobState.InQueueState);
    }
}
