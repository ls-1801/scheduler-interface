package de.tuberlin.batchjoboperator.schedulingreconciler.external;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.List;

@Value
@Jacksonized
@Builder
public class ExternalSlotScheduling {
    String mode;
    List<ExternalSlotSchedulingItems> jobs;
}
