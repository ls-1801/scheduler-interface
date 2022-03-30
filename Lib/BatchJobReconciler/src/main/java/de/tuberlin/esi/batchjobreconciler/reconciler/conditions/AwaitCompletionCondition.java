package de.tuberlin.esi.batchjobreconciler.reconciler.conditions;

import de.tuberlin.esi.batchjobreconciler.reconciler.BatchJobContext;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import static de.tuberlin.esi.common.util.General.getNullSafe;

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
        if (context.getApplication().isFailed()) {
            return error("Application has failed");
        }

        return getNullSafe(context.getApplication()::isCompleted).orElse(false);
    }
}
