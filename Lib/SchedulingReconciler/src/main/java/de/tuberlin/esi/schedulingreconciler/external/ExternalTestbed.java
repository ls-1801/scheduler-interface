package de.tuberlin.esi.schedulingreconciler.external;

import de.tuberlin.esi.common.crd.slots.SlotOccupationStatus;
import de.tuberlin.esi.common.crd.slots.SlotsStatusState;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.Set;

@Data
@NoArgsConstructor
public class ExternalTestbed {
    private String name;
    private SlotsStatusState state;
    private int numberOfNodes;
    private int numberOfSlotsPerNode;
    private Map<String, Set<SlotOccupationStatus>> slotsByNode;
}
