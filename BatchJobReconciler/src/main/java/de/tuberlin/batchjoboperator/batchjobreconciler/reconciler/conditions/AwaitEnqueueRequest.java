package de.tuberlin.batchjoboperator.batchjobreconciler.reconciler.conditions;

import com.fasterxml.jackson.annotation.JsonIgnore;
import de.tuberlin.batchjoboperator.batchjobreconciler.reconciler.BatchJobContext;
import de.tuberlin.batchjoboperator.common.crd.NamespacedName;
import de.tuberlin.batchjoboperator.common.crd.scheduling.SchedulingState;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;

import static de.tuberlin.batchjoboperator.common.util.General.getNullSafe;

@NoArgsConstructor
@Slf4j
public class AwaitEnqueueRequest extends BatchJobCondition {

    public static final String condition = AWAIT_ENQUEUE_REQUEST_CONDITION;

    @JsonIgnore
    @Getter
    @Nullable
    NamespacedName activeScheduling;

    @Override
    public String getCondition() {
        return condition;
    }

    @Override
    protected boolean updateInternal(BatchJobContext context) {
        var schedulingNameOpt =
                getNullSafe(() -> context.getResource().getSpec().getActiveScheduling());


        if (schedulingNameOpt.isEmpty())
            return false;


        var scheduling = context.getScheduling(schedulingNameOpt.get());

        if (scheduling == null || scheduling.getStatus().getState() == SchedulingState.Error) {
            return error("Scheduling is set but does not exist or is in a bad state");
        }

        this.activeScheduling = schedulingNameOpt.get();
        return true;
    }
}
