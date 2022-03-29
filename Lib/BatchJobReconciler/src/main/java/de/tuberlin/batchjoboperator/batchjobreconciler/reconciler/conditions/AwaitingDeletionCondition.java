package de.tuberlin.batchjoboperator.batchjobreconciler.reconciler.conditions;

import de.tuberlin.batchjoboperator.batchjobreconciler.reconciler.BatchJobContext;
import lombok.NoArgsConstructor;

@NoArgsConstructor
public class AwaitingDeletionCondition extends BatchJobCondition {

    public static final String condition = AWAIT_DELETION_CONDITION;

    @Override
    public String getCondition() {
        return condition;
    }


    @Override
    protected boolean updateInternal(BatchJobContext context) {
        return !context.getApplication().isExisting();
    }
}
