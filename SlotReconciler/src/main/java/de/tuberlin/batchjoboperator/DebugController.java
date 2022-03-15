package de.tuberlin.batchjoboperator;

import de.tuberlin.batchjoboperator.common.crd.slots.Slot;
import de.tuberlin.batchjoboperator.slotsreconciler.ClusterAllocatableResources;
import de.tuberlin.batchjoboperator.slotsreconciler.ClusterAvailableResources;
import de.tuberlin.batchjoboperator.slotsreconciler.ClusterRequestedResources;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.client.KubernetesClient;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
public class DebugController {

    private final KubernetesClient client;

    @GetMapping(value = "/debug/node-set-up")
    public List<String> setupLabelsOnNode(@RequestParam("count") Integer count) {

        var requested = ClusterRequestedResources.aggregate(client.pods().list().getItems());
        var slots = client.resources(Slot.class).inNamespace("default").list();
        if (slots.getItems().size() != 1) {
            throw new RuntimeException("Only a single Slot is supported");
        }
        var slot = slots.getItems().get(0);

        var allocatable = ClusterAllocatableResources.aggregate(client.nodes().list().getItems());

        var free = ClusterAvailableResources.diff(allocatable, requested);

        var nodesWithFittingCapacity = client.nodes().list().getItems()
                                             .stream()
                                             .filter(n -> free.getAvailableResources(n, "cpu")
                                                              .compareTo(Quantity.getAmountInBytes(
                                                                      slot
                                                                              .getSpec()
                                                                              .getResourcesPerSlot()
                                                                              .get("cpu"
                                                                              ))) >= 0)
                                             .sorted(Comparator.comparing(n -> requested.getRequestedResources(n,
                                                     "cpu")))
                                             .limit(count)
                                             .collect(Collectors.toList());

        if (nodesWithFittingCapacity.size() < count) {
            throw new RuntimeException("Not enough nodes with enough capacity");
        }

        for (int i = 0; i < nodesWithFittingCapacity.size(); i++) {
            nodesWithFittingCapacity.get(i).getMetadata().getLabels()
                                    .put(slot.getSpec().getNodeLabel(), i + "");
        }

        nodesWithFittingCapacity.forEach(n -> client.nodes().patch(n));

        return nodesWithFittingCapacity.stream().map(n -> n.getMetadata().getName()).collect(Collectors.toList());

    }
}
