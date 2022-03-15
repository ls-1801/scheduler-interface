package de.tuberlin.batchjoboperator.slotsreconciler;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import de.tuberlin.batchjoboperator.common.NamespacedName;
import de.tuberlin.batchjoboperator.common.crd.slots.Slot;
import de.tuberlin.batchjoboperator.common.crd.slots.SlotOccupationStatus;
import de.tuberlin.batchjoboperator.common.crd.slots.SlotSpec;
import de.tuberlin.batchjoboperator.common.crd.slots.SlotState;
import de.tuberlin.batchjoboperator.common.crd.slots.SlotStatus;
import de.tuberlin.batchjoboperator.common.crd.slots.SlotsStatusState;
import io.fabric8.kubernetes.api.model.Node;
import io.fabric8.kubernetes.api.model.NodeList;
import io.fabric8.kubernetes.api.model.OwnerReferenceBuilder;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodBuilder;
import io.fabric8.kubernetes.api.model.PodList;
import io.fabric8.kubernetes.api.model.PodSpecBuilder;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.api.model.ResourceRequirementsBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.ControllerConfiguration;
import io.javaoperatorsdk.operator.api.reconciler.DeleteControl;
import io.javaoperatorsdk.operator.api.reconciler.EventSourceContext;
import io.javaoperatorsdk.operator.api.reconciler.EventSourceInitializer;
import io.javaoperatorsdk.operator.api.reconciler.Reconciler;
import io.javaoperatorsdk.operator.api.reconciler.UpdateControl;
import io.javaoperatorsdk.operator.processing.event.ResourceID;
import io.javaoperatorsdk.operator.processing.event.source.EventSource;
import io.javaoperatorsdk.operator.processing.event.source.informer.InformerEventSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.BooleanUtils;

import javax.annotation.Nonnull;
import java.math.BigDecimal;
import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static de.tuberlin.batchjoboperator.common.NamespacedName.getNamespace;
import static de.tuberlin.batchjoboperator.common.constants.SlotsConstants.GHOST_POD_NAME_PREFIX;
import static de.tuberlin.batchjoboperator.common.constants.SlotsConstants.SLOT_GHOSTPOD_WILL_BE_PREEMPTED_BY_NAME;
import static de.tuberlin.batchjoboperator.common.constants.SlotsConstants.SLOT_POD_GENERATION_NAME;
import static de.tuberlin.batchjoboperator.common.constants.SlotsConstants.SLOT_POD_IS_GHOSTPOD_NAME;
import static de.tuberlin.batchjoboperator.common.constants.SlotsConstants.SLOT_POD_LABEL_NAME;
import static de.tuberlin.batchjoboperator.common.constants.SlotsConstants.SLOT_POD_SLOT_ID_NAME;
import static de.tuberlin.batchjoboperator.common.constants.SlotsConstants.SLOT_POD_TARGET_NODE_NAME;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

/**
 * A very simple sample controller that creates a service with a label.
 */
@ControllerConfiguration
@RequiredArgsConstructor
@Slf4j
public class SlotReconciler implements Reconciler<Slot>, EventSourceInitializer<Slot> {


    private final KubernetesClient kubernetesClient;

    private Map<String, Set<ResourceID>> slots;
    private Map<String, ResourceID> slotsByName;


    @Override
    public DeleteControl cleanup(Slot resource, Context context) {
        log.info("Cleaning up for: {}", resource.getMetadata().getName());
        return Reconciler.super.cleanup(resource, context);
    }


    @Override
    public UpdateControl<Slot> reconcile(Slot resource, Context context) {
        return reconcileInternal(resource, context);
    }


    private UpdateControl<Slot> reconcileInternal(Slot resource, Context context) {
        addToResourceMap(resource);
        PodsPerNode ghostPodsPerNode;
        PodList ghostPods;
        try {
            ghostPods = kubernetesClient.pods().inNamespace(getNamespace(resource))
                                        .withLabel(SLOT_POD_LABEL_NAME, resource.getMetadata().getName())
                                        .list();
            ghostPodsPerNode = PodsPerNode.groupByNode(ghostPods);
        } catch (SchedulingInProgressException ex) {
            log.info("Scheduling is in progress!");
            return UpdateControl.noUpdate();
        }

        // Manual preemption is necessary if scheduler preemption is not triggered. This happens if enough resource
        // are available on a node to place jobs, that should preempt the ghostpod, next to it on the node.
        var podsThatNeedToBePreempted = ghostPodsPerNode.getPreemptedSlots();
        podsThatNeedToBePreempted.getPods().stream()
                                 .peek(pod -> log.debug("Deleting Preempted Pod: {}", NamespacedName.of(pod)))
                                 .forEach(pod -> kubernetesClient.pods().inNamespace(getNamespace(resource))
                                                                 .delete(pod));
        ghostPodsPerNode = ghostPodsPerNode.removePreemptedSlots(podsThatNeedToBePreempted);


        // Calculate Desired State
        var nodesWithLabel = kubernetesClient.nodes().withLabel(resource.getSpec().getNodeLabel()).list();
        var desiredPods = desiredPods(nodesWithLabel, resource);

        var nodeNameToNodeId = nodesWithLabel.getItems().stream().collect(
                ImmutableMap.toImmutableMap(
                        node -> node.getMetadata().getName(),
                        node -> Integer.parseInt(node.getMetadata().getLabels().get(resource.getSpec().getNodeLabel()))
                )
        );


        // Bring to desired State
        // First Verify
        var problems = SlotProblems.builder();
        var nonGhostPods = kubernetesClient.pods().inAnyNamespace().withoutLabel(SLOT_POD_LABEL_NAME).list();
        var nodeNonGhostRequestMap = ClusterRequestedResources.aggregate(nonGhostPods.getItems());
        reportProblemIfNotEnoughAllocatableResourcesPerNode(problems, nodesWithLabel, nodeNonGhostRequestMap, resource);
        if (problems.build().anyProblems()) {
            kubernetesClient.pods().inNamespace(getNamespace(resource)).delete(ghostPods.getItems());
            return problems.build().updateStatusIfRequired(resource);
        }

        var notDesiredPods = ghostPodsPerNode.diff(desiredPods);
        log.info("Deleting not desired Pods: {}", notDesiredPods.getPods());
        notDesiredPods.getPods()
                      .forEach(pod -> kubernetesClient.pods().inNamespace(getNamespace(resource)).delete(pod));
//        waitUntilDeletionOfPods(notDesiredPods.getPods());

        var desiredButNotExistingPods = desiredPods.diff(ghostPodsPerNode);
        log.info("Creating desired but not existing Pods: {}", desiredButNotExistingPods.getPods());
        desiredButNotExistingPods.getPods()
                                 .forEach(pod -> kubernetesClient.pods().inNamespace(getNamespace(resource))
                                                                 .create(pod));

        var actualDesiredState = ghostPodsPerNode.diff(notDesiredPods).union(desiredButNotExistingPods);
        log.info("Updating Preemption status for desired and already existing Pods: {}", actualDesiredState.getPods());
        actualDesiredState.getPods().forEach(this::checkPreemptionStatus);
        updateStatus(resource, actualDesiredState, nodeNameToNodeId);
        return problems.build().updateStatusIfRequired(resource);
    }


    private SlotState deriveStateFromPod(@Nonnull ApplicationPodView pod) {

        var isGhostPod = pod.getLabel(SLOT_POD_IS_GHOSTPOD_NAME);
        var willBePreemptedBy = pod.getLabel(SLOT_GHOSTPOD_WILL_BE_PREEMPTED_BY_NAME);

        if (isGhostPod.isEmpty()) {
            log.error("Pod: {} does not have a {} label!", ApplicationPodView.wrap(pod), SLOT_POD_IS_GHOSTPOD_NAME);
            return SlotState.ERROR;
        }

        if (isGhostPod.map(BooleanUtils::toBooleanObject).isEmpty()) {
            log.error("Pod: {} does not have a valid {} label value: {}!",
                    ApplicationPodView.wrap(pod),
                    SLOT_POD_IS_GHOSTPOD_NAME,
                    isGhostPod.get());
            return SlotState.ERROR;
        }

        var isGhostPodBoolean = isGhostPod.map(BooleanUtils::toBooleanObject).get();

        if (BooleanUtils.isTrue(isGhostPodBoolean) && willBePreemptedBy.isEmpty()) {
            return SlotState.FREE;
        }

        if (BooleanUtils.isTrue(isGhostPodBoolean) && willBePreemptedBy.isPresent()) {
            return SlotState.RESERVED;
        }

        if (BooleanUtils.isFalse(isGhostPodBoolean)) {
            return SlotState.OCCUPIED;
        }

        //Unreachable
        log.error("Pod: {} is in an unhandled state, IsGhostPod: {}, preemptor: {}!",
                ApplicationPodView.wrap(pod),
                isGhostPodBoolean,
                willBePreemptedBy.orElse(null));
        return SlotState.ERROR;

    }


    private UpdateControl<Slot> updateStatus(@Nonnull Slot resource, @Nonnull PodsPerNode desiredPods,
                                             ImmutableMap<String, Integer> nodeNameToNodeId) {
        var status = desiredPods.getPods().stream()
                                .map(pod -> new SlotOccupationStatus(
                                        deriveStateFromPod(pod),
                                        pod.getName(),
                                        pod.getNodeName(),
                                        nodeNameToNodeId.get(pod.getNodeName()),
                                        pod.getSlotId(),
                                        pod.getMetadata().getUid())).collect(Collectors.toList());
        resource.getStatus().setSlots(status);
        resource.getStatus().setState(deriveSlotsStateFromSlots(resource.getStatus(), status));
        return UpdateControl.updateStatus(resource);
    }

    private SlotsStatusState deriveSlotsStateFromSlots(SlotStatus resourceStatus,
                                                       List<SlotOccupationStatus> slotsStatus) {
        var anyReserved = slotsStatus.stream().anyMatch(slot -> slot.getState() == SlotState.RESERVED);
        var allFree = slotsStatus.stream().allMatch(slot -> slot.getState() == SlotState.FREE);
        var occupiedCount = slotsStatus.stream().filter(slot -> slot.getState() == SlotState.OCCUPIED).count();
        var previousState = resourceStatus.getState();


        if (previousState.equals(SlotsStatusState.IN_PROGRESS)) {
            var timestamp = Optional.ofNullable(resourceStatus.getSchedulingInProgressTimestamp())
                                    .map(LocalDateTime::parse);
            if (timestamp.isPresent() && LocalDateTime.now().plus(5 * 60, ChronoUnit.SECONDS)
                                                      .isAfter(timestamp.get())) {
                log.info("Slot in progress state has timed out, since no progress has been made in the last 5 minutes");
                return SlotsStatusState.ERROR;
            }

            if (!anyReserved && allFree) {
                log.info("Slot remains in state busy, although no reservation or occupations where made");
                return SlotsStatusState.IN_PROGRESS;
            }

            if (anyReserved) {
                log.info("Slots with reservation, Slots are busy");
                return SlotsStatusState.IN_PROGRESS;
            }
        }

        if (resourceStatus.getCurrentScheduling() != null && occupiedCount == resourceStatus.getCurrentScheduling()
                                                                                            .size()) {
            log.info("Current scheduling was successful, all jobs are running");
            return SlotsStatusState.RUNNING;
        }

        if (occupiedCount > 0) {
            log.info("Current scheduling was successful, all jobs are running, some have already finished");
            return SlotsStatusState.RUNNING;
        }

        if (allFree) {
            log.info("All slots are free, put into ready state");
            return SlotsStatusState.SUCCESS;
        }

        log.error("Could not determine Slots state");
        return SlotsStatusState.ERROR;
    }

    private void checkPreemptionStatus(ApplicationPodView pod) {
        var preemptorLabel = pod.getLabel(SLOT_GHOSTPOD_WILL_BE_PREEMPTED_BY_NAME);

        if (preemptorLabel.isEmpty())
            return;

        var preemptor = preemptorLabel.get();

        var preemptorPod = kubernetesClient.pods().inNamespace(getNamespace(pod)).withName(preemptor).get();

        if (preemptorPod == null) {
            log.debug("Preemptor Pod does not exist");
            pod.getMetadata().getLabels().remove(SLOT_GHOSTPOD_WILL_BE_PREEMPTED_BY_NAME);
            kubernetesClient.pods().inNamespace(getNamespace(pod)).patch(pod);
        }
    }

    private void reportProblemIfNotEnoughAllocatableResourcesPerNode(
            SlotProblems.SlotProblemsBuilder builder,
            NodeList nodeList,
            ClusterRequestedResources clusterRequestResources,
            Slot slot
    ) {
        nodeList.getItems().forEach(node -> {
            var problems = nodeEligibleForSlots(node, clusterRequestResources, slot.getSpec());
            builder.addNodeProblems(node, problems);
        });
    }


    private void addToResourceMap(Slot resource) {
        var label = resource.getSpec().getNodeLabel();
        slotsByName.put(resource.getMetadata().getName(), ResourceID.fromResource(resource));
        slots.computeIfAbsent(label, (k) -> new HashSet<>()).add(ResourceID.fromResource(resource));
    }

    private PodsPerNode desiredPods(NodeList nodes, Slot slot) {

        var desiredPods = nodes.getItems().stream()
                               .flatMap(node ->
                                       IntStream.range(0, slot.getSpec().getSlotsPerNode())
                                                .mapToObj(id -> createGhostPodForNode(node, slot, id))
                               ).collect(toList());

        return PodsPerNode.groupByNode(desiredPods);
    }

    private Pod createGhostPodForNode(Node node, Slot slot, int id) {
        var resourceRequirements = new ResourceRequirementsBuilder()
                .withRequests(slot.getSpec().getResourcesPerSlot())
                .build();

        var ownerReference = new OwnerReferenceBuilder()
                .withName(slot.getMetadata().getName())
                .withApiVersion(slot.getApiVersion())
                .withUid(slot.getMetadata().getUid())
                .withController(Boolean.TRUE)
                .withKind(slot.getKind())
                .build();

        var name = podNameStrategy(node.getMetadata().getName(), slot, id);

        var pod = new PodBuilder().withNewMetadata()
                                  .withName(name)
                                  .withNamespace(getNamespace(slot))
                                  .withLabels(Map.of(
                                          SLOT_POD_LABEL_NAME, slot.getMetadata().getName(),
                                          SLOT_POD_TARGET_NODE_NAME, node.getMetadata().getName(),
                                          SLOT_POD_SLOT_ID_NAME, "" + id,
                                          SLOT_POD_IS_GHOSTPOD_NAME, "true"))
                                  .withAnnotations(Map.of(SLOT_POD_GENERATION_NAME,
                                          slot.getMetadata().getGeneration() + ""))
                                  .withOwnerReferences(ownerReference)
                                  .endMetadata()
                                  .withNewSpec()
                                  .addNewContainer()
                                  .withName("container")
                                  .withImage("nginx")
                                  .withResources(resourceRequirements)
                                  .endContainer()
                                  .endSpec()
                                  .build();

        var identifiablePod = makeNodeIdentifiable(node, slot, pod);

        return identifiablePod;
    }

    private String podNameStrategy(String nodeName, Slot slot, int id) {
        return MessageFormat.format("{0}-{1}-on-{2}-{3}",
                GHOST_POD_NAME_PREFIX,
                slot.getMetadata().getName(),
                nodeName,
                id
        );
    }

    private Pod makeNodeIdentifiable(Node node, Slot slot, Pod pod) {
        var spec = new PodSpecBuilder(pod.getSpec())
                .withNodeName(node.getMetadata().getName())
                .build();

        return new PodBuilder(pod).withSpec(spec).build();
    }


    private List<SlotProblems.Problem> nodeEligibleForSlots(Node node, ClusterRequestedResources nodeRequestMap,
                                                            SlotSpec spec) {
        Function<String, Optional<SlotProblems.Problem>> enoughResource = (String resourceName) -> {
            var inUse = nodeRequestMap.getRequestedResources(node, resourceName);
            var capacity = node.getStatus().getAllocatable().get(resourceName);
            var free = Quantity.getAmountInBytes(capacity).subtract(inUse);
            var required =
                    Quantity.getAmountInBytes(spec.getResourcesPerSlot().get(resourceName))
                            .multiply(BigDecimal.valueOf(spec.getSlotsPerNode()));

            if (free.compareTo(required) < 0) {
                return Optional.of(new SlotProblems.NotEnoughRequestedResourcesProblem(resourceName, required, free));
            }

            return Optional.empty();
        };

        return spec.getResourcesPerSlot().keySet().stream()
                   .map(enoughResource)
                   .filter(Optional::isPresent)
                   .map(Optional::get)
                   .collect(ImmutableList.toImmutableList());
    }

    @Override
    public List<EventSource> prepareEventSources(EventSourceContext<Slot> context) {

        var slotsInCluster = kubernetesClient.resources(Slot.class)
                                             .inAnyNamespace().list().getItems();

        this.slots =
                slotsInCluster.stream().collect(
                        groupingBy(slot -> slot.getSpec()
                                               .getNodeLabel(),
                                mapping(ResourceID::fromResource, toSet())));

        this.slotsByName = slotsInCluster.stream().collect(
                toMap(slot -> slot.getMetadata().getName(), ResourceID::fromResource)
        );


        var nodes = kubernetesClient.nodes()
                                    .runnableInformer(0);

        var podInformer = kubernetesClient.pods()
                                          .inAnyNamespace()
                                          .withLabel(SLOT_POD_LABEL_NAME)
                                          .runnableInformer(0);

        return List.of(
                new InformerEventSource<>(podInformer, (pod) -> {
                    var view = ApplicationPodView.wrap(pod);
                    log.info("Pod triggered reconciliation: {}", view);

                    return view.getLabel(SLOT_POD_LABEL_NAME)
                               .map(slotsByName::get)
                               .map(Set::of)
                               .orElse(Collections.emptySet());
                }),
                new InformerEventSource<>(nodes, (n) -> {
                    var node = ApplicationNodeView.wrap(n);
                    var slotsByLabel = node.getLabels().keySet().stream()
                                           .flatMap(labelName -> this.slots.getOrDefault(labelName,
                                                                             Collections.emptySet())
                                                                           .stream())
                                           .collect(Collectors.toSet());


                    if (!slotsByLabel.isEmpty()) {
                        log.info("Node with label, triggered reconciliation: {}", node.getMetadata().getName());
                        return slotsByLabel;
                    }

                    log.info("Node without label, triggered reconciliation: {}", node.getMetadata().getName());
                    // No label found on node maybe pods need to be deleted
                    return this.slots.values().stream().flatMap(Collection::stream).collect(toSet());
                })
        );
    }
}