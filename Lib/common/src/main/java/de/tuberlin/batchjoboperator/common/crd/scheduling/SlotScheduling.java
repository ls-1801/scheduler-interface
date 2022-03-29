package de.tuberlin.batchjoboperator.common.crd.scheduling;

import lombok.Data;

import java.util.List;

@Data
public class SlotScheduling {
    private SlotSchedulingMode mode;
    private List<SlotSchedulingItem> jobs;
}
