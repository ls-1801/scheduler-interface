package de.tuberlin.batchjoboperator.schedulingreconciler.external;

import com.fasterxml.jackson.annotation.JsonValue;
import de.tuberlin.batchjoboperator.common.crd.slots.SlotOccupationStatus;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class SlotsByNode {
    @JsonValue
    Map<String, Set<SlotOccupationStatus>> value;

    public SlotsByNode(Set<SlotOccupationStatus> occupationStatuses) {
        this.value = occupationStatuses.stream().collect(Collectors.groupingBy(SlotOccupationStatus::getNodeName,
                Collectors.toSet()));
    }
}
