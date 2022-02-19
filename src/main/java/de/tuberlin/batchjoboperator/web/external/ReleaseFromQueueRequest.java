package de.tuberlin.batchjoboperator.web.external;

import lombok.Value;

import java.util.List;

@Value
class ReleaseFromQueueRequest {
    List<NamespacedName> jobs;
    String slotName;
}
