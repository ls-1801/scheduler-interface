package de.tuberlin.batchjoboperator.slotsreconciler;

import de.tuberlin.batchjoboperator.common.crd.slots.Slot;
import io.fabric8.kubernetes.client.KubernetesClient;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import static de.tuberlin.batchjoboperator.common.NamespacedName.getNamespace;

@AllArgsConstructor
@Slf4j
public class SlotStatusReporter {
    private final KubernetesClient client;
    private Slot slots;

    private void patch() {
        slots = client.resources(Slot.class)
                      .inNamespace(getNamespace(slots))
                      .patchStatus(slots);
    }



}
