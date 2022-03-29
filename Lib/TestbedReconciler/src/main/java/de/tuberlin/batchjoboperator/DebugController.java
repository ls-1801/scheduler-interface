package de.tuberlin.batchjoboperator;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.tuberlin.batchjoboperator.common.crd.batchjob.BatchJob;
import de.tuberlin.batchjoboperator.common.crd.batchjob.BatchJobState;
import de.tuberlin.batchjoboperator.common.crd.slots.Slot;
import de.tuberlin.batchjoboperator.testbedreconciler.ClusterAllocatableResources;
import de.tuberlin.batchjoboperator.testbedreconciler.ClusterAvailableResources;
import de.tuberlin.batchjoboperator.testbedreconciler.ClusterRequestedResources;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.text.MessageFormat;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
public class DebugController {

    private final KubernetesClient client;
    private final ObjectMapper mapper;
    private final String namespace;

    @GetMapping(value = "/debug/reset-all-jobs")
    public List<String> resetAllJobs(@RequestParam("allNamespaces") boolean allNamespaces) {
        var namespacedClient = allNamespaces ? client.resources(BatchJob.class) :
                client.resources(BatchJob.class).inNamespace(namespace);

        return namespacedClient.list().getItems().stream()
                               .filter(job -> job.getStatus().getState() == BatchJobState.FailedState)
                               .map(job -> {
                                   namespacedClient.delete(job);
                                   var newJob = new BatchJob();
                                   newJob.setSpec(job.getSpec());
                                   newJob.setMetadata(new ObjectMetaBuilder()
                                           .withName(job.getMetadata().getName())
                                           .withNamespace(job.getMetadata().getNamespace())
                                           .build()
                                   );
                                   namespacedClient.create(newJob);

                                   return newJob.getMetadata().getName();
                               })
                               .collect(Collectors.toList());


    }

    @GetMapping(value = "/debug/node-set-up")
    public List<String> setupLabelsOnNode(
            @RequestParam("count") Integer count,
            @RequestParam("name") String testbedName
    ) throws JsonProcessingException {

        var requested = ClusterRequestedResources.aggregate(client.pods().inAnyNamespace().list().getItems());
        var testbed = client.resources(Slot.class).inNamespace(namespace).withName(testbedName).get();
        if (testbed == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    MessageFormat.format("Testbed {0} not found!", testbedName));
        }

        var allocatable = ClusterAllocatableResources.aggregate(client.nodes().list().getItems());

        var free = ClusterAvailableResources.diff(allocatable, requested);

        var nodesWithFittingCapacity = client.nodes().list().getItems()
                                             .stream()
                                             .filter(n -> free.getAvailableResources(n, "cpu")
                                                              .compareTo(Quantity.getAmountInBytes(
                                                                      testbed
                                                                              .getSpec()
                                                                              .getResourcesPerSlot()
                                                                              .get("cpu"
                                                                              ))) >= 0)
                                             .sorted(Comparator.comparing(n -> requested.getRequestedResources(n,
                                                     "cpu")))
                                             .limit(count)
                                             .collect(Collectors.toList());

        if (nodesWithFittingCapacity.size() < count) {
            var freeString = mapper.writeValueAsString(free);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    MessageFormat.format("Not enough resources available for Testbed {0}!\n{1}",
                            testbedName,
                            freeString));
        }

        for (int i = 0; i < nodesWithFittingCapacity.size(); i++) {
            nodesWithFittingCapacity.get(i).getMetadata().getLabels()
                                    .put(testbed.getSpec().getNodeLabel(), i + "");
        }

        try {
            nodesWithFittingCapacity.forEach(n -> client.nodes().patch(n));
        } catch (KubernetesClientException kce) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "cannot patch Nodes!");
        }

        return nodesWithFittingCapacity.stream().map(n -> n.getMetadata().getName()).collect(Collectors.toList());

    }
}
