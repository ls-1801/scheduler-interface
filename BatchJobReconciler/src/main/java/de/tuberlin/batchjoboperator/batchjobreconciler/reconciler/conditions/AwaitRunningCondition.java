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
    protected boolean updateInternal(BatchJobContext client) {
        return getNullSafe(() -> client.getApplication().isExisting() && (client.getApplication()
                                                                                .isCompleted() || client.getApplication()
                                                                                                        .isRunning()))
                .orElse(false);
    }
}