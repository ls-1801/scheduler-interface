package de.tuberlin.batchjoboperator.schedulingreconciler.reconciler;

import de.tuberlin.batchjoboperator.common.NamespacedName;
import de.tuberlin.batchjoboperator.common.crd.batchjob.BatchJob;
import de.tuberlin.batchjoboperator.common.crd.slots.Slot;
import de.tuberlin.batchjoboperator.common.crd.slots.SlotState;
import io.fabric8.kubernetes.client.KubernetesClient;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.text.MessageFormat;

@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class AwaitNumberOfSlotsAvailableCondition extends SchedulingJobCondition {
    @Getter
    private final String condition = AWAITING_NUMBER_OF_SLOTS_AVAILABLE;

    @Getter
    @Setter
    private int numberOfSlotRequired;

    @Getter
    @Setter
    private NamespacedName slotsName;

    public AwaitNumberOfSlotsAvailableCondition(NamespacedName name, NamespacedName slotsName,
                                                int numberOfSlotRequired) {
        super(name);
        this.slotsName = slotsName;
        this.numberOfSlotRequired = numberOfSlotRequired;
    }

    @Override
    boolean updateInternal(KubernetesClient client, BatchJob job) {
        var slots =
                client.resources(Slot.class).inNamespace(slotsName.getNamespace()).withName(slotsName.getName()).get();
        if (slots == null)
            return error(MessageFormat.format("Jobs slots {0}/{1} not found", slotsName.getName(),
                    slotsName.getNamespace()));

        if (slots.getStatus().getSlots() == null) {
            return error(MessageFormat.format("Jobs slots {0}/{1} is not Ready", slotsName.getName(),
                    slotsName.getNamespace()));
        }

        return slots.getStatus().getSlots().stream().map(occ -> occ.getState() == SlotState.FREE)
                    .count() >= numberOfSlotRequired;
    }

    @Override
    public boolean preventRunnable() {
        return true;
    }
}
