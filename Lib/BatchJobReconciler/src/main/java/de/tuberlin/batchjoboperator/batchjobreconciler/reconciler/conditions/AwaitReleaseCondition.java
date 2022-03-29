package de.tuberlin.batchjoboperator.batchjobreconciler.reconciler.conditions;

import de.tuberlin.batchjoboperator.batchjobreconciler.reconciler.BatchJobContext;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import static de.tuberlin.batchjoboperator.common.util.General.getNullSafe;

@NoArgsConstructor
@Slf4j
public class AwaitReleaseCondition extends BatchJobCondition {

    public static final String condition = AWAIT_RELEASE_CONDITION;

    @Override
    public String getCondition() {
        return condition;
    }


    @Override
    protected boolean updateInternal(BatchJobContext context) {
        var resource = context.getResource();
        return getNullSafe(() ->
                resource.getSpec().getActiveScheduling() == null ||
                        !resource.getSpec().getActiveScheduling()
                                 .equals(resource.getStatus().getActiveScheduling())
        )
                .orElse(true);
    }
}
