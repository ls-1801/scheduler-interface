package de.tuberlin.batchjoboperator.batchjobreconciler.reconciler.conditions;

import com.fasterxml.jackson.annotation.JsonIgnore;
import de.tuberlin.batchjoboperator.batchjobreconciler.reconciler.BatchJobContext;
import de.tuberlin.batchjoboperator.batchjobreconciler.reconciler.CreationRequest;
import de.tuberlin.batchjoboperator.common.crd.slots.SlotsStatusState;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.annotation.Nullable;

import static de.tuberlin.batchjoboperator.common.util.General.getNullSafe;

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
        var creationRequestOpt = getNullSafe(() -> CreationRequest.fromLabels(job.getMetadata().getLabels()));

        if (creationRequestOpt.isEmpty())
            return false;

        var slots = context.getSlots(creationRequestOpt.get().getSlotsName());

        if (slots == null || slots.getStatus().getState() == SlotsStatusState.ERROR) {
            return error("Slots is set but does not exist or is in a bad state");
        }

        this.creationRequest = creationRequestOpt.get();
        return true;
    }
}
