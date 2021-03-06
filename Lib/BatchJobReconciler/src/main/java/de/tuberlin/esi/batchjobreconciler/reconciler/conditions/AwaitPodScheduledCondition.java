package de.tuberlin.esi.batchjobreconciler.reconciler.conditions;

import de.tuberlin.esi.batchjobreconciler.reconciler.BatchJobContext;
import de.tuberlin.esi.common.crd.NamespacedName;
import io.fabric8.kubernetes.api.model.Pod;
import lombok.extern.slf4j.Slf4j;

import static de.tuberlin.esi.common.util.General.getNullSafe;

@Slf4j
public class AwaitPodScheduledCondition extends BatchJobCondition {
    public static final String condition = AWAIT_POD_SCHEDULED_CONDITION;

    @Override
    public String getCondition() {
        return condition;
    }


    private void logPodNode(Pod p) {
        log.debug("Pod {} NodeName: {}", NamespacedName.of(p), p.getSpec().getNodeName());
    }

    @Override
    protected boolean updateInternal(BatchJobContext context) {
        if (context.getApplication().isFailed()) {
            return error("Application has failed");
        }

        return getNullSafe(() ->
                context.getApplication().isExisting() &&
                        !context.getApplication().getPods().isEmpty() &&
                        context.getApplication().getPods()
                               .stream()
                               .peek(this::logPodNode)
                               .allMatch(p -> p.getSpec()
                                               .getNodeName() != null))
                .orElse(false);
    }
}
