package de.tuberlin.batchjoboperator.reconciler.batchjob.common;

import de.tuberlin.batchjoboperator.crd.batchjob.BatchJob;
import io.javaoperatorsdk.operator.api.reconciler.UpdateControl;

public interface ApplicationManager {
    UpdateControl<BatchJob> handle(BatchJob resource);
}
