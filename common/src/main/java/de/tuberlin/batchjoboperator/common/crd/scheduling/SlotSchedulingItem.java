package de.tuberlin.batchjoboperator.common.crd.scheduling;

import de.tuberlin.batchjoboperator.common.crd.NamespacedName;
import lombok.Data;

import java.util.Set;

@Data
public class SlotSchedulingItem {
    private NamespacedName name;
    private Set<Integer> slotIds;
}
