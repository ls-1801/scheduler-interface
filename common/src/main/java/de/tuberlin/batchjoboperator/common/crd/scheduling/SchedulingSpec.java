package de.tuberlin.batchjoboperator.common.crd.scheduling;

import de.tuberlin.batchjoboperator.common.NamespacedName;
import lombok.Data;
import org.springframework.lang.Nullable;

import java.util.List;

@Data
public class SchedulingSpec {
    @Nullable
    private List<NamespacedName> queueBased;

    @Nullable
    private SlotScheduling slotBased;

    private NamespacedName slots;
}
