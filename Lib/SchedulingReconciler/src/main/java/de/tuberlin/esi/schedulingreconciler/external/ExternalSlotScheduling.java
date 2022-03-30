package de.tuberlin.esi.schedulingreconciler.external;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import java.util.List;

@Value
@Jacksonized
@Builder
public class ExternalSlotScheduling {
    @NotBlank
    String mode;
    @NotEmpty
    List<ExternalSlotSchedulingItems> jobs;
}
