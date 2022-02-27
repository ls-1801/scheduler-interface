package de.tuberlin.batchjoboperator.web.external;

import de.tuberlin.batchjoboperator.crd.batchjob.BatchJob;
import de.tuberlin.batchjoboperator.crd.batchjob.BatchJobState;
import de.tuberlin.batchjoboperator.crd.slots.Slot;
import de.tuberlin.batchjoboperator.crd.slots.SlotOccupationStatus;
import de.tuberlin.batchjoboperator.crd.slots.SlotStatus;
import de.tuberlin.batchjoboperator.reconciler.slots.ClusterAllocatableResources;
import de.tuberlin.batchjoboperator.reconciler.slots.ClusterAvailableResources;
import de.tuberlin.batchjoboperator.reconciler.slots.ClusterRequestResources;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesResourceList;
import io.fabric8.kubernetes.api.model.Node;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.client.KubernetesClient;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import static de.tuberlin.batchjoboperator.web.external.VerifiedReleaseFromQueueRequest.verifyRequest;

@RestController
@RequestMapping("/scheduler")
@RequiredArgsConstructor
@Slf4j
public class ExternalSchedulerController {

    private final KubernetesClient client;
    private final boolean failOnProblem = true;

    private final Lock schedulingInProgressLock = new ReentrantLock();


    @GetMapping(value = "/debug/node-set-up")
    public List<String> setupLabelsOnNode(@RequestParam("count") Integer count) {

        var requested = ClusterRequestResources.aggregate(client.pods().list().getItems());
        var slots = client.resources(Slot.class).inNamespace(getNamespace()).list();
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

    @GetMapping(value = "/queue")
    public List<NamespacedName> getQueue() {
        var list = client.resources(BatchJob.class)
                         //Sadly this does not seem to be supported at the moment
                         //.withField("Status.State", BatchJobState.InQueueState.name())
                         .list();

        return list.getItems().stream()
                   .filter(job -> BatchJobState.InQueueState.equals(job.getStatus().getState()))
                   .map(NamespacedName::of)
                   .collect(Collectors.toList());

    }

    private <T extends HasMetadata> T verifySingleResource(KubernetesResourceList<T> list) {
        if (list.getItems().size() != 1) {
            throw new RuntimeException("Multiple Resources exist but a single is expected");
        }

        return list.getItems().get(0);
    }

    @GetMapping(value = "/slots")
    public List<ExternalSlotPerNodeView> getSlots() {
        var list = client.resources(Slot.class)
                         .list();
        var slot = verifySingleResource(list);

        return slot.getStatus().getSlots().stream()
                   .collect(Collectors.groupingBy(SlotOccupationStatus::getNodeName, Collectors.counting()))
                   .entrySet()
                   .stream()
                   .map(e -> new ExternalSlotPerNodeView(e.getKey(), e.getValue()))
                   .collect(Collectors.toList());
    }

    @GetMapping(value = "/nodes")
    public List<ExternalNodeView> getNodes() {
        var nodes = client.nodes().list();

        return nodes.getItems().stream()
                    .map(ExternalNodeView::new)
                    .collect(Collectors.toList());
    }

    private void executeScheduling(VerifiedReleaseFromQueueRequest request) {
        var reporter = new SlotStatusReporter(client, request.getSlot());

        reporter.schedulingInProgress(request.getRequest().getJobs());
        try {
            request.getJobSlotPairing()
                   .forEach((job, freeSlots) ->
                           ApplicationBuilder.forJob(job, request)
                                             .inNamespace(getNamespace())
                                             .inSlots(freeSlots)
                                             .create(client));
        } catch (Exception e) {
            log.error("Problem during Application CR creation", e);
            reporter.abortWithProblem(e);
            throw e;
        }
    }

    @PostMapping(value = "/release-from-queue")
    public void releaseFromQueue(@RequestBody ReleaseFromQueueRequest releaseFromQueueRequest) {
        if (schedulingInProgressLock.tryLock()) {
            try {
                var request = verifyRequest(releaseFromQueueRequest, getNamespace(), client);
                executeScheduling(request);
            } finally {
                schedulingInProgressLock.unlock();
            }
        }
        else {
            throw new RuntimeException("Scheduling in progress!");
        }

    }

    @GetMapping(value = "/state")
    public SlotStatus stateOfApplications() {
        return verifySingleResource(client.resources(Slot.class).list()).getStatus();
    }

    private String getNamespace() {
        return "default";
    }

    @Value
    @RequiredArgsConstructor
    public static class ExternalSlotPerNodeView {
        String nodeHostName;
        Long slotsPerNode;
    }

    @Value
    public static class ExternalNodeView {
        String nodeHostName;
        BigDecimal availableVirtualCores;

        public ExternalNodeView(Node node) {
            this.nodeHostName = node.getMetadata().getName();
            this.availableVirtualCores =
                    Quantity.getAmountInBytes(node.getStatus().getAllocatable().get("cpu"));
        }
    }
}
