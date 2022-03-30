package de.tuberlin.esi.batchjobreconciler.reconciler.conditions;

import com.fasterxml.jackson.annotation.JsonIgnore;
import de.tuberlin.esi.batchjobreconciler.reconciler.BatchJobContext;
import de.tuberlin.esi.common.crd.batchjob.CreationRequest;
import de.tuberlin.esi.common.crd.testbed.TestbedState;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.annotation.Nullable;

import static de.tuberlin.esi.common.util.General.getNullSafe;

@NoArgsConstructor
public class AwaitCreationRequest extends BatchJobCondition {

    public static final String condition = AWAIT_CREATION_REQUEST_CONDITION;

    @JsonIgnore
    @Nullable
    @Getter
    private CreationRequest creationRequest;

    @Override
    public String getCondition() {
        return condition;
    }

    @Override
    protected boolean updateInternal(BatchJobContext context) {
        var creationRequestOpt = getNullSafe(() -> context.getResource().getSpec().getCreationRequest());

        if (creationRequestOpt.isEmpty())
            return false;

        var slots = context.getSlots(creationRequestOpt.get().getTestbedName());

        if (slots == null || slots.getStatus().getState() == TestbedState.ERROR) {
            return error("Slots is set but does not exist or is in a bad state");
        }

        this.creationRequest = creationRequestOpt.get();
        return true;
    }
}
