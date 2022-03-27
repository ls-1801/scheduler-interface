package de.tuberlin.batchjoboperator.schedulingreconciler.statemachine;

import de.tuberlin.batchjoboperator.common.crd.NamespacedName;

public class JobClaimedByAnotherSchedulingException extends ClaimedByAnotherSchedulingException {
    public JobClaimedByAnotherSchedulingException(NamespacedName jobName, NamespacedName activeScheduling) {
        super("BatchJob", jobName, activeScheduling);
    }
}

