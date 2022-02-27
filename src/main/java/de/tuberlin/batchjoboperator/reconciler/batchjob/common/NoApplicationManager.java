package de.tuberlin.batchjoboperator.reconciler.batchjob.common;

import de.tuberlin.batchjoboperator.crd.batchjob.BatchJob;
import de.tuberlin.batchjoboperator.crd.batchjob.BatchJobState;
import de.tuberlin.batchjoboperator.reconciler.batchjob.common.statemachine.AbstractState;
import de.tuberlin.batchjoboperator.reconciler.batchjob.common.statemachine.ManagedApplicationEvent;
import io.javaoperatorsdk.operator.api.reconciler.UpdateControl;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nonnull;
import java.util.Objects;

@Slf4j
public class NoApplicationManager extends AbstractApplicationManager {

    @Override
    protected Pair<AbstractState.Action, ManagedApplicationEvent> getAction(@Nonnull BatchJob resource) {
        var event = ManagedApplicationEvent.NO_APPLICATION;
        var action = new AbstractState.Action() {
            @Override
            public UpdateControl<BatchJob> notExpected(String stateName) {
                throw new RuntimeException("NO_SPARK caused an unexpected State transition from: " + stateName);
            }

            @Override
            public UpdateControl<BatchJob> updateState(BatchJobState newState) {
                if (Objects.equals(resource.getStatus().getState(), newState)) return noChange();
                log.info("BatchJob changes state from {} to {}", resource.getStatus().getState(), newState);
                resource.getStatus().setState(newState);
                return UpdateControl.updateStatus(resource);
            }

            @Override
            public UpdateControl<BatchJob> noChange() {
                return UpdateControl.noUpdate();
            }

            @Override
            public UpdateControl<BatchJob> deleteApplication() {
                throw new RuntimeException("Cannot delete Application when NO_SPARK event");
            }
        };

        return Pair.of(action, event);
    }

}
