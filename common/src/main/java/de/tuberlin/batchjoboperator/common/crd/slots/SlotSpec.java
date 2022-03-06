package de.tuberlin.batchjoboperator.common.crd.slots;


import io.fabric8.kubernetes.api.model.Quantity;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.Map;

@Value
@Builder(toBuilder = true)
@Jacksonized
public class SlotSpec {
    int slotsPerNode;
    String nodeLabel;
    Map<String, Quantity> resourcesPerSlot;
}
