package de.tuberlin.esi.schedulingreconciler.statemachine;

import de.tuberlin.esi.common.crd.NamespacedName;

public class JobClaimedByAnotherSchedulingException extends ClaimedByAnotherSchedulingException {
    public JobClaimedByAnotherSchedulingException(NamespacedName jobName, NamespacedName activeScheduling) {
        super("BatchJob", jobName, activeScheduling);
    }
}

