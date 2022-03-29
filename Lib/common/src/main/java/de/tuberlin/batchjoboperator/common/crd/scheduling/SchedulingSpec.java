package de.tuberlin.batchjoboperator.common.crd.scheduling;

import de.tuberlin.batchjoboperator.common.crd.NamespacedName;
import lombok.Data;

import javax.annotation.Nullable;
import java.util.List;

@Data
public class SchedulingSpec {
    @Nullable
    private List<NamespacedName> queueBased;

    @Nullable
    private SlotScheduling slotBased;

    private NamespacedName slots;
}
