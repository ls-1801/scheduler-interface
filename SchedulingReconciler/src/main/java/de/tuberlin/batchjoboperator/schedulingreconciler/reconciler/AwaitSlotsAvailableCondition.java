package de.tuberlin.batchjoboperator.schedulingreconciler.reconciler;

import de.tuberlin.batchjoboperator.common.NamespacedName;
import de.tuberlin.batchjoboperator.common.crd.batchjob.BatchJob;
import de.tuberlin.batchjoboperator.common.crd.slots.Slot;
import de.tuberlin.batchjoboperator.common.crd.slots.SlotState;
import io.fabric8.kubernetes.client.KubernetesClient;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.text.MessageFormat;
import java.util.Set;

@NoArgsConstructor
public class AwaitSlotsAvailableCondition extends SchedulingJobCondition {
    @Getter
    private final String condition = AWAITING_SLOTS_AVAILABLE;

    @Getter
    @Setter
    private NamespacedName slotsName;

    @Getter
    @Setter
    private Set<Integer> slotIds;

    public AwaitSlotsAvailableCondition(NamespacedName name, NamespacedName slotsName, Set<Integer> slotIds) {
        super(name);
        this.slotsName = slotsName;
        this.slotIds = slotIds;
    }

    @Override
    boolean updateInternal(KubernetesClient client, BatchJob name) {
        var slots =
                client.resources(Slot.class).inNamespace(slotsName.getNamespace()).withName(slotsName.getName()).get();
        if (slots == null)
            return error(MessageFormat.format("Jobs slots {0}/{1} not found", slotsName.getName(),
                    slotsName.getNamespace()));

        if (slots.getStatus().getSlots() == null) {
            return error(MessageFormat.format("Jobs slots {0}/{1} is not Ready", slotsName.getName(),
                    slotsName.getNamespace()));
        }

        return slots.getStatus().getSlots().stream()
                    .filter(occ -> slotIds.contains(occ.getPosition()))
                    .map(occ -> occ.getState() == SlotState.FREE)
                    .count() == slotIds.size();
    }

    @Override
    public boolean preventRunnable() {
        return true;
    }
}
