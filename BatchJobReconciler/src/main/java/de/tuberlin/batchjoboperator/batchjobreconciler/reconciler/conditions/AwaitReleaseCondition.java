package de.tuberlin.batchjoboperator.batchjobreconciler.reconciler.conditions;

import de.tuberlin.batchjoboperator.batchjobreconciler.reconciler.BatchJobContext;
import de.tuberlin.batchjoboperator.common.NamespacedName;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import static de.tuberlin.batchjoboperator.common.constants.SchedulingConstants.ACTIVE_SCHEDULING_LABEL_NAME;
import static de.tuberlin.batchjoboperator.common.constants.SchedulingConstants.ACTIVE_SCHEDULING_LABEL_NAMESPACE;
import static de.tuberlin.batchjoboperator.common.util.General.getNullSafe;
import static de.tuberlin.batchjoboperator.common.util.General.nonNull;

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
        return getNullSafe(() -> {

            var schedulingName = job.getMetadata().getLabels().get(ACTIVE_SCHEDULING_LABEL_NAME);
            var schedulingNamespace = job.getMetadata().getLabels().get(ACTIVE_SCHEDULING_LABEL_NAMESPACE);
            if (!nonNull(schedulingName, schedulingNamespace))
                return true;

            var scheduling = context.getScheduling(new NamespacedName(schedulingName, schedulingNamespace));
            return scheduling == null;
        }).orElse(true);
    }
}
