package de.tuberlin.esi.schedulingreconciler.statemachine;

import de.tuberlin.esi.common.crd.NamespacedName;
import de.tuberlin.esi.common.crd.batchjob.BatchJob;

public class JobInFailedStateException extends RuntimeException {
    public JobInFailedStateException(BatchJob job) {
        super("BatchJob " + NamespacedName.of(job) + " is in failed state");
    }
}
