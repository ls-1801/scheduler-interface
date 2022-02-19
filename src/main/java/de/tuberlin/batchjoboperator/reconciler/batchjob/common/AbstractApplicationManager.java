package de.tuberlin.batchjoboperator.reconciler.batchjob.common;

import de.tuberlin.batchjoboperator.crd.batchjob.BatchJob;
import de.tuberlin.batchjoboperator.reconciler.batchjob.common.statemachine.AbstractState;
import de.tuberlin.batchjoboperator.reconciler.batchjob.common.statemachine.ManagedApplicationEvent;
import de.tuberlin.batchjoboperator.reconciler.batchjob.common.statemachine.StateProvider;
import io.javaoperatorsdk.operator.api.reconciler.UpdateControl;
import lombok.Synchronized;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nonnull;

public abstract class AbstractApplicationManager implements ApplicationManager {

    @Override
    @Synchronized
    public UpdateControl<BatchJob> handle(@Nonnull BatchJob resource) {

        var pair = getAction(resource);
        var action = pair.getKey();
        var event = pair.getValue();
        var context = getContext(resource, event);

        switch (event) {
            case SCHEDULED:
                return StateProvider.of(resource).onScheduled(context, action);
            case RUNNING:
                return StateProvider.of(resource).onRunning(context, action);
            case COMPLETED:
                return StateProvider.of(resource).onCompleted(context, action);
            case ERROR:
                return StateProvider.of(resource).onFailed(context, action);
            case NO_APPLICATION:
            case REMOVED:
                return StateProvider.of(resource).onNoApplication(context, action);
            case NO_CHANGE:
                return UpdateControl.noUpdate();
        }

        throw new RuntimeException("Bug: Not Reachable, unless ENUM incomplete!");
    }

    protected AbstractState.Context getContext(@Nonnull BatchJob resource, ManagedApplicationEvent event) {
        return new AbstractState.Context() {
            @Override
            public ManagedApplicationEvent getEvent() {
                return event;
            }

            @Override
            public BatchJob getResource() {
                return resource;
            }
        };
    }

    protected abstract Pair<AbstractState.Action, ManagedApplicationEvent> getAction(@Nonnull BatchJob resource);


}
