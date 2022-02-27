package de.tuberlin.batchjoboperator.web.external;

import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Streams;
import de.tuberlin.batchjoboperator.crd.batchjob.BatchJob;
import de.tuberlin.batchjoboperator.crd.slots.Slot;
import de.tuberlin.batchjoboperator.crd.slots.SlotOccupationStatus;
import de.tuberlin.batchjoboperator.crd.slots.SlotsStatusState;
import io.fabric8.kubernetes.api.model.Node;
import io.fabric8.kubernetes.client.KubernetesClient;
import lombok.Builder;
import lombok.Value;
import org.apache.commons.lang3.tuple.Pair;

import java.text.MessageFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Builder
@Value
public class VerifiedReleaseFromQueueRequest {
    ImmutableList<BatchJob> jobs;
    ImmutableList<Node> nodes;
    Slot slot;
    ReleaseFromQueueRequest request;
    Supplier<Map<BatchJob, List<SlotOccupationStatus>>> jobSlotPairing =
            Suppliers.memoize(this::getJobSlotPairingInternal);
    Supplier<Map<String, String>> nodesSlotIds = Suppliers.memoize(this::getNodesSlotIdsInternal);

    public static VerifiedReleaseFromQueueRequest verifyRequest(ReleaseFromQueueRequest request, String namespace,
                                                                KubernetesClient client) {
        var builder = VerifiedReleaseFromQueueRequest.builder();

        builder.request(request);
        var slot = client.resources(Slot.class)
                         .inNamespace(namespace)
                         .withName(request.getSlotName()).get();

        if (slot == null) {
            throw new RuntimeException("Slot not found");
        }

        if (slot.getStatus().getSlots() == null) {
            throw new RuntimeException("Slot does not appear to be in a valid state");
        }
        builder.slot(slot);


        if (slot.getStatus().getSlots().size() < request.getJobs().size()) {
            throw new RuntimeException(MessageFormat.format("Not enough slots available required: {0}, available {1}",
                    request.getJobs().size(), slot.getStatus().getSlots().size()));
        }

        if (slot.getStatus().getState() == SlotsStatusState.IN_PROGRESS)
            throw new RuntimeException("Slot is in Progress");

        var distinctJobs = ImmutableSet.copyOf(request.getJobs());
        if (distinctJobs.isEmpty()) {
            throw new RuntimeException("Requires at least one job");
        }

        var actualJobs = distinctJobs.stream().map(requestJob ->
                                             client.resources(BatchJob.class)
                                                   .inNamespace(requestJob.getNamespace())
                                                   .withName(requestJob.getName()).get())
                                     .filter(Objects::nonNull)
                                     .collect(ImmutableList.toImmutableList());

        if (actualJobs.size() != distinctJobs.size()) {
            throw new RuntimeException("Jobs not found");
        }
        builder.jobs(actualJobs);

        var nodes = client.nodes().withLabel(slot.getSpec().getNodeLabel(), null)
                          .list().getItems();

        if (nodes.isEmpty()) {
            throw new RuntimeException("Slots Resource does not manage any nodes");
        }

        builder.nodes(ImmutableList.copyOf(nodes));

        return builder.build();
    }

    private Map<String, String> getNodesSlotIdsInternal() {
        return nodes.stream()
                    .filter(n -> n.getMetadata().getLabels().get(slot.getSpec().getNodeLabel()) != null)
                    .collect(ImmutableMap.toImmutableMap(
                            n -> n.getMetadata().getName(),
                            n -> n.getMetadata().getLabels().get(slot.getSpec().getNodeLabel())));
    }

    private Map<BatchJob, List<SlotOccupationStatus>> getJobSlotPairingInternal() {
        var map =
                Streams.zip(
                               request.getJobs().stream(),
                               slot.getStatus().getSlots().stream(),
                               Pair::of)
                       .limit(request.getJobs().size())
                       .collect(Collectors.groupingBy(p -> jobs.stream()
                                                               .filter(job ->
                                                                       job.getMetadata().getName()
                                                                          .equals(p.getKey()
                                                                                   .getName()) &&
                                                                               job.getMetadata()
                                                                                  .getNamespace()
                                                                                  .equals(p.getKey()
                                                                                           .getNamespace()))
                                                               .findFirst().get(),
                               Collectors.mapping(Pair::getValue, ImmutableList.toImmutableList())
                       ));

        return ImmutableMap.copyOf(map);
    }


    public Map<String, String> getNodesSlotIds() {
        return nodesSlotIds.get();
    }

    public Map<BatchJob, List<SlotOccupationStatus>> getJobSlotPairing() {
        return jobSlotPairing.get();
    }


}
