package de.tuberlin.esi.batchjobreconciler.reconciler.conditions;

import de.tuberlin.esi.batchjobreconciler.reconciler.BatchJobContext;
import lombok.NoArgsConstructor;

import static de.tuberlin.esi.common.util.General.getNullSafe;

@NoArgsConstructor
public class AwaitRunningCondition extends BatchJobCondition {

    public static final String condition = AWAIT_RUNNING_CONDITION;

    @Override
    public String getCondition() {
        return condition;
    }


    @Override
    protected boolean updateInternal(BatchJobContext context) {
        if (context.getApplication().isFailed()) {
            return error("Application has failed");
        }

        return getNullSafe(() -> context.getApplication().isExisting() && (context.getApplication()
                                                                                  .isCompleted() || context.getApplication()
                                                                                                           .isRunning()))
                .orElse(false);
    }
}