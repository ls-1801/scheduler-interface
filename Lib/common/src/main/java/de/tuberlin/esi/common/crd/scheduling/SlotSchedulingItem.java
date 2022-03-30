package de.tuberlin.esi.common.crd.scheduling;

import de.tuberlin.esi.common.crd.NamespacedName;
import lombok.Data;

import java.util.Set;

@Data
public class SlotSchedulingItem {
    private NamespacedName name;
    private Set<Integer> slotIds;
}
