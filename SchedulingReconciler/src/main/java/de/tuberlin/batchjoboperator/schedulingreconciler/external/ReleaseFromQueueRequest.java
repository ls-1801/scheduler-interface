package de.tuberlin.batchjoboperator.schedulingreconciler.external;

import de.tuberlin.batchjoboperator.common.NamespacedName;
import lombok.Value;

import java.util.List;

@Value
class ReleaseFromQueueRequest {
    List<NamespacedName> jobs;
    String slotName;
}
