package de.tuberlin.batchjoboperator.reconciler.batchjob.common.statemachine;


import de.tuberlin.batchjoboperator.crd.batchjob.BatchJob;
import de.tuberlin.batchjoboperator.crd.batchjob.BatchJobState;
import io.javaoperatorsdk.operator.api.reconciler.UpdateControl;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AbstractState {

    public UpdateControl<BatchJob> onNoApplication(Context context, Action action) {
        return onAnyChange(context, action);
    }

    public UpdateControl<BatchJob> onScheduled(Context context, Action action) {
        return onAnyChange(context, action);
    }

    public UpdateControl<BatchJob> onRunning(Context context, Action action) {
        return onAnyChange(context, action);
    }

    public UpdateControl<BatchJob> onCompleted(Context context, Action action) {
        return onAnyChange(context, action);
    }

    public UpdateControl<BatchJob> onFailed(Context context, Action action) {
        return action.updateState(BatchJobState.FailedState);
    }

    public UpdateControl<BatchJob> onUnknownChange(Context context, Action action) {
        return action.updateState(BatchJobState.UnknownState);
    }

    public UpdateControl<BatchJob> onAnyChange(Context context, Action action) {
        return action.notExpected(this.getClass().getName());
    }

    public interface Action {
        UpdateControl<BatchJob> notExpected(String stateName);

        UpdateControl<BatchJob> updateState(BatchJobState newState);


        UpdateControl<BatchJob> noChange();

        UpdateControl<BatchJob> deleteApplication();
    }

    public interface Context {
        ManagedApplicationEvent getEvent();

        BatchJob getResource();
    }
}

