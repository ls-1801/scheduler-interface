package de.tuberlin.batchjoboperator.crd.slots;


import io.fabric8.kubernetes.api.model.Quantity;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
public class SlotSpec {
    private int slotsPerNode;
    private String nodeLabel;
    private Map<String, Quantity> resourcesPerSlot;
}
