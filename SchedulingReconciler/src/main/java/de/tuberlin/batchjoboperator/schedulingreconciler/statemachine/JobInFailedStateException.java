package de.tuberlin.batchjoboperator.schedulingreconciler.statemachine;

import de.tuberlin.batchjoboperator.common.crd.NamespacedName;
import de.tuberlin.batchjoboperator.common.crd.batchjob.BatchJob;

public class JobInFailedStateException extends RuntimeException {
    public JobInFailedStateException(BatchJob job) {
        super("BatchJob " + NamespacedName.of(job) + " is in failed state");
    }
}
