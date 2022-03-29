package de.tuberlin.batchjoboperator.schedulingreconciler.external;

import de.tuberlin.batchjoboperator.common.crd.slots.SlotOccupationStatus;
import de.tuberlin.batchjoboperator.common.crd.slots.SlotsStatusState;
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
