package de.tuberlin.batchjoboperator.batchjobreconciler.reconciler.conditions;

import de.tuberlin.batchjoboperator.batchjobreconciler.reconciler.BatchJobContext;

import static de.tuberlin.batchjoboperator.common.util.General.getNullSafe;

public class AwaitPodScheduledCondition extends BatchJobCondition {
    public static final String condition = AWAIT_POD_SCHEDULED_CONDITION;

    @Override
    public String getCondition() {
        return condition;
    }


    @Override
    protected boolean updateInternal(BatchJobContext client) {
        return getNullSafe(() -> client.getApplication().isExisting() && client.getApplication().getPods()
                                                                               .stream()
                                                                               .allMatch(p -> p.getSpec()
                                                                                               .getNodeName() != null))
                .orElse(false);
    }
}
