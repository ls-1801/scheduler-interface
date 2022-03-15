package de.tuberlin.batchjoboperator.schedulingreconciler.external;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.Set;

@Value
@Jacksonized
@Builder
public class ExternalSlotSchedulingItems {
    Set<Integer> slots;
    String name;
}
