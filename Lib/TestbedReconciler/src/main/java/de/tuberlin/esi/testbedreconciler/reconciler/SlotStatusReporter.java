package de.tuberlin.esi.testbedreconciler.reconciler;

import de.tuberlin.esi.common.crd.testbed.Testbed;
import io.fabric8.kubernetes.client.KubernetesClient;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import static de.tuberlin.esi.common.crd.NamespacedName.getNamespace;

@AllArgsConstructor
@Slf4j
public class SlotStatusReporter {
    private final KubernetesClient client;
    private Testbed slots;

    private void patch() {
        slots = client.resources(Testbed.class)
                      .inNamespace(getNamespace(slots))
                      .patchStatus(slots);
    }



}
