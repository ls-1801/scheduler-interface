package de.tuberlin.esi.batchjobreconciler.reconciler.conditions;

import de.tuberlin.esi.batchjobreconciler.reconciler.BatchJobContext;
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
