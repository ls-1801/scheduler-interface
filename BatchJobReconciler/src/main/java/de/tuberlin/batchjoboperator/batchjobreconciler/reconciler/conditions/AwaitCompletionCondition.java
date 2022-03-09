package de.tuberlin.batchjoboperator.batchjobreconciler.reconciler.conditions;

import de.tuberlin.batchjoboperator.batchjobreconciler.reconciler.BatchJobContext;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import static de.tuberlin.batchjoboperator.common.util.General.getNullSafe;

@NoArgsConstructor
@Slf4j
public class AwaitCompletionCondition extends BatchJobCondition {

    public static final String condition = AWAIT_COMPLETION_CONDITION;

    @Override
    public String getCondition() {
        return condition;
    }


    @Override
    protected boolean updateInternal(BatchJobContext context) {
        return getNullSafe(context.getApplication()::isCompleted).orElse(false);
    }
}
