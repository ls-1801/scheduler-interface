package de.tuberlin.batchjoboperator.schedulingreconciler.external;

import de.tuberlin.batchjoboperator.common.crd.scheduling.SchedulingState;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;
import org.apache.commons.lang3.BooleanUtils;

import javax.annotation.Nullable;
import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.NotBlank;
import java.util.List;

@Value
@Jacksonized
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ExternalScheduling {
    @NotBlank
    String name;

    SchedulingState state;

    ExternalBatchJobSchedulingStatusWithoutDuplicates jobStatus;

    @NotBlank
    String testBed;

    @Nullable
    List<String> queue;

    @Nullable
    ExternalSlotScheduling slots;

    @AssertTrue(message = "Scheduling is either SlotBased OR QueueBased")
    private boolean isEitherSlotOrQueue() {
        return BooleanUtils.xor(new boolean[]{queue != null, slots != null});
    }

}
