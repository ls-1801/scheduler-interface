package de.tuberlin.batchjoboperator.batchjobreconciler.reconciler.conditions;

import de.tuberlin.batchjoboperator.batchjobreconciler.reconciler.BatchJobContext;
import de.tuberlin.batchjoboperator.common.NamespacedName;
import de.tuberlin.batchjoboperator.common.crd.scheduling.SchedulingState;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import static de.tuberlin.batchjoboperator.common.constants.SchedulingConstants.ACTIVE_SCHEDULING_LABEL_NAME;
import static de.tuberlin.batchjoboperator.common.constants.SchedulingConstants.ACTIVE_SCHEDULING_LABEL_NAMESPACE;
import static de.tuberlin.batchjoboperator.common.util.General.getNullSafe;

@NoArgsConstructor
@Slf4j
public class AwaitEnqueueRequest extends BatchJobCondition {

    private static final String condition = AWAIT_ENQUEUE_REQUEST_CONDITION;

    @Override
    public String getCondition() {
        return condition;
    }

    @Override
    protected boolean updateInternal(BatchJobContext context) {
        var schedulingNameOpt = getNullSafe(() -> {
            var name = job.getMetadata().getLabels().get(ACTIVE_SCHEDULING_LABEL_NAME);
            var namespace = job.getMetadata().getLabels().get(ACTIVE_SCHEDULING_LABEL_NAMESPACE);

            log.info("{}: {}, {):{}",
                    ACTIVE_SCHEDULING_LABEL_NAME, name,
                    ACTIVE_SCHEDULING_LABEL_NAMESPACE, namespace);

            if (name == null || namespace == null)
                return null;

            return new NamespacedName(
                    job.getMetadata().getLabels().get(ACTIVE_SCHEDULING_LABEL_NAME),
                    job.getMetadata().getLabels().get(ACTIVE_SCHEDULING_LABEL_NAMESPACE));
        });


        if (schedulingNameOpt.isEmpty())
            return false;


        var scheduling = context.getScheduling(schedulingNameOpt.get());

        if (scheduling == null || scheduling.getStatus().getState() == SchedulingState.Error) {
            return error("Scheduling is set but does not exist or is in a bad state");
        }

        return true;
    }
}
