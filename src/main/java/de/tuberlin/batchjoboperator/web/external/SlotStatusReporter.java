package de.tuberlin.batchjoboperator.web.external;

import de.tuberlin.batchjoboperator.crd.slots.Slot;
import de.tuberlin.batchjoboperator.crd.slots.SlotOccupationStatus;
import de.tuberlin.batchjoboperator.crd.slots.SlotState;
import de.tuberlin.batchjoboperator.crd.slots.SlotsStatusState;
import de.tuberlin.batchjoboperator.reconciler.slots.ApplicationPodView;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.KubernetesClient;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@AllArgsConstructor
public class SlotStatusReporter {
    private final KubernetesClient client;
    private Slot slots;

    private void patch() {
        slots = client.resources(Slot.class).patchStatus(slots);
    }

    public void schedulingInProgress(List<NamespacedName> jobs) {
        slots.getStatus().setCurrentScheduling(jobs);
        slots.getStatus().setState(SlotsStatusState.IN_PROGRESS);
        slots.getStatus().setSchedulingInProgressTimestamp(LocalDateTime.now().toString());
        patch();
    }

    public void reserveSlot(SlotOccupationStatus slot, Pod pod) {
        if (!this.slots.getStatus().getSlots().contains(slot)) {
            throw new RuntimeException("Trying to reserve slot, that is not part of the StatusReporter");
        }
        var view = ApplicationPodView.wrap(pod);

        slot.setState(SlotState.RESERVED);
        slot.setReservedFor(view.getName());
        patch();
    }

    public void abortWithProblem(Exception e) {
        slots.getStatus().setCurrentScheduling(null);
        slots.getStatus().setState(SlotsStatusState.ERROR);
        slots.getStatus().addProblem("During Application CR creation: " + e.getMessage());
        patch();
    }
}
