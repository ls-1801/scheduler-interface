package de.tuberlin.esi.schedulingreconciler.external;

import de.tuberlin.esi.common.crd.NamespacedName;
import lombok.Value;

import java.util.List;

@Value
class ReleaseFromQueueRequest {
    List<NamespacedName> jobs;
    String slotName;
}
