package de.tuberlin.batchjoboperator.batchjobreconciler.reconciler.conditions;

import de.tuberlin.batchjoboperator.batchjobreconciler.reconciler.BatchJobContext;
import lombok.NoArgsConstructor;

import static de.tuberlin.batchjoboperator.common.util.General.getNullSafe;

@NoArgsConstructor
public class AwaitRunningCondition extends BatchJobCondition {

    public static final String condition = AWAIT_RUNNING_CONDITION;

    @Override
    public String getCondition() {
        return condition;
    }


    @Override
    protected boolean updateInternal(BatchJobContext context) {
        return getNullSafe(() -> context.getApplication().isExisting() && (context.getApplication()
                                                                                  .isCompleted() || context.getApplication()
                                                                                                           .isRunning()))
                .orElse(false);
    }
}