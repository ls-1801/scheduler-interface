package de.tuberlin.batchjoboperator.crd.slots;

import io.javaoperatorsdk.operator.api.ObservedGenerationAwareStatus;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@ToString
public class SlotStatus extends ObservedGenerationAwareStatus {
    private SlotsStatusState state;
    @Nullable
    private Map<String, List<String>> nodeProblems;

    @Nullable
    private List<String> problems;

    @Nullable
    private List<SlotOccupationStatus> slots;
}
