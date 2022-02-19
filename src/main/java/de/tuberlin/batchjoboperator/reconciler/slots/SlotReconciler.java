package de.tuberlin.batchjoboperator.reconciler.slots;

import com.google.common.collect.ImmutableList;
import de.tuberlin.batchjoboperator.crd.slots.Slot;
import de.tuberlin.batchjoboperator.crd.slots.SlotOccupationStatus;
import de.tuberlin.batchjoboperator.crd.slots.SlotSpec;
import de.tuberlin.batchjoboperator.crd.slots.SlotState;
import io.fabric8.kubernetes.api.model.HasMetadata;
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
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.WatcherException;
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
import org.apache.commons.lang3.BooleanUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.math.BigDecimal;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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
public class SlotReconciler implements Reconciler<Slot>, EventSourceInitializer<Slot> {

    public static final String PREFIX = "batchjob.tuberlin.de/";
    //    private static final String GHOST_POD_LABEL_NAME = "batchjob.tuberlin.de/ghostpod";
//    private static final String GHOST_POD_LABEL_VALUE = "true";
//
//    private static final String GHOST_POD_GENERATION_ANNOTATION_NAME =
//            "batchjob.tuberlin.de/latest-observerd-resource-version";
//    private static final String GHOST_POD_LABEL_TARGET_NODE_NAME = "batchjob.tuberlin.de/target-node";
//    public static final String GHOST_POD_LABEL_POD_ID_NAME = "batchjob.tuberlin.de/pod-id";
    public static final String SLOT_POD_LABEL_NAME = PREFIX + "slots-resource-name";
    public static final String SLOT_POD_IS_GHOSTPOD_NAME = PREFIX + "slots-is-ghost-pod";
    public static final String SLOT_GHOSTPOD_WILL_BE_PREEMPTED_NAME = PREFIX + "slots-ghost-pod-will-be-preempted";
    public static final String SLOT_POD_TARGET_NODE_NAME = PREFIX + "slots-target-node";
    public static final String SLOT_POD_SLOT_ID_NAME = PREFIX + "slots-id";
    public static final String SLOT_POD_GENERATION_NAME = PREFIX + "slots-latest-observed-resource-version";
    private static final Logger log = LoggerFactory.getLogger(SlotReconciler.class);
    private static final String GHOST_POD_NAME_PREFIX = "ghostpod";


    private final KubernetesClient kubernetesClient;

    private Map<String, Set<ResourceID>> slots;
    private Map<String, ResourceID> slotsByName;


    @Override
    public DeleteControl cleanup(Slot resource, Context context) {
        log.info("Cleaning up for: {}", resource.getMetadata().getName());
        return Reconciler.super.cleanup(resource, context);
    }

    Function<Node, String> getNodeName() {
        return hm -> hm.getMetadata().getName();
    }

    private boolean waitUntilDeletionOfPods(List<ApplicationPodView> pods) {
        if (pods.isEmpty())
            return true;


        final CountDownLatch deleteLatch = new CountDownLatch(pods.size());
        pods.forEach(pod -> {
            kubernetesClient.pods().delete(pod);
            kubernetesClient.pods().inNamespace(pod.getMetadata().getNamespace()).withName(pod.getMetadata().getName())
                            .watch(new Watcher<>() {
                                @Override
                                public void eventReceived(Action action, Pod resource) {
                                    if (action == Action.DELETED) {
                                        deleteLatch.countDown();
                                    }
                                }

                                @Override
                                public void onClose(WatcherException cause) {

                                }
                            });
        });

        try {
            return deleteLatch.await(10, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            return false;
        }
    }




    /*
     * TODO:
     *  - Deployment of pods inside a slot
     *  - Status should reflect what is going on in the Cluster
     *      - Overview of empty Slots and occupied Slots
     *      - Problems
     *      - In Progress
     *  - Clarify the Way Slots are Implemented:
     *      - The Slots CRD controls only Nodes which have the specified label inside the Spec
     *      - The Slots CRD make sure that there are always the specified amount of pods with the correct amount of
     *        request resources deployed on the cluster
     *      - A Slot can either be a ghost-pod: which is a pod that is not doing anything just reserving resources or
     *        an application pod
     *      - ghost pods can be preempted anytime an application pod wants to take its slot
     *      - pods t
     *
     *  -
     */

    /**
     * The Idea of a Slot is to reserve Resources on a node that can later be used for a batch job
     */
    @Override
    public UpdateControl<Slot> reconcile(Slot resource, Context context) {
        log.info("Reconciling: {}", resource.getMetadata().getName());
        addToResourceMap(resource);
        PodsPerNode ghostPodsPerNode;
        PodList ghostPods;
        try {
            ghostPods = kubernetesClient.pods().withLabel(SLOT_POD_LABEL_NAME, resource.getMetadata().getName())
                                        .list();
            ghostPodsPerNode = PodsPerNode.groupByNode(ghostPods);
        } catch (SchedulingInProgressException ex) {
            log.info("Scheduling is in progress!");
            return UpdateControl.noUpdate();
        }

        // Observe Current State


        var podsThatNeedToBePreempted = ghostPodsPerNode.preemptEmptySlots();
        waitUntilDeletionOfPods(podsThatNeedToBePreempted.getPods());
        ghostPodsPerNode = ghostPodsPerNode.diff(podsThatNeedToBePreempted);


        // Calculate Desired State
        var nodesWithLabel = kubernetesClient.nodes().withLabel(resource.getSpec().getNodeLabel()).list();
        var desiredPods = desiredPods(nodesWithLabel, resource);


        // Bring to desired State
        // First Verify
        var problems = SlotProblems.builder();
        var nonGhostPods = kubernetesClient.pods().withoutLabel(SLOT_POD_LABEL_NAME).list();
        var nodeNonGhostRequestMap = ClusterRequestResources.aggregate(nonGhostPods.getItems());
        reportProblemIfNotEnoughAllocatableResourcesPerNode(problems, nodesWithLabel, nodeNonGhostRequestMap, resource);
        if (problems.build().anyProblems()) {
            kubernetesClient.pods().inAnyNamespace().delete(ghostPods.getItems());
            return problems.build().updateStatusIfRequired(resource);
        }

        var notDesiredPods = ghostPodsPerNode.diff(desiredPods);
        var desiredButNotExistingPods = desiredPods.diff(ghostPodsPerNode);

        waitUntilDeletionOfPods(notDesiredPods.getPods());

        desiredButNotExistingPods.getPods()
                                 .forEach(pod -> kubernetesClient.pods().inNamespace(getNamespace()).create(pod));


        var actualDesiredState = ghostPodsPerNode.diff(notDesiredPods).diff(desiredButNotExistingPods);
        updateStatus(resource, actualDesiredState);
        return problems.build().updateStatusIfRequired(resource);
    }


    private SlotState deriveStateFromPod(@Nonnull ApplicationPodView pod) {

        var isGhostPod = pod.getLabel(SLOT_POD_IS_GHOSTPOD_NAME);
        var willBePreempted = pod.getLabel(SLOT_GHOSTPOD_WILL_BE_PREEMPTED_NAME);

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


        if (willBePreempted.isPresent() && willBePreempted.map(BooleanUtils::toBooleanObject).isEmpty()) {
            log.error("Pod: {} does not have a valid {} label value: {}!",
                    ApplicationPodView.wrap(pod),
                    SLOT_GHOSTPOD_WILL_BE_PREEMPTED_NAME,
                    willBePreempted.get());
            return SlotState.ERROR;
        }

        var willBePreemptedBoolean = willBePreempted.map(BooleanUtils::toBooleanObject).orElse(null);
        var isGhostPodBoolean = isGhostPod.map(BooleanUtils::toBooleanObject).get();

        if (BooleanUtils.isTrue(isGhostPodBoolean) && BooleanUtils.isNotTrue(willBePreemptedBoolean)) {
            return SlotState.FREE;
        }

        if (BooleanUtils.isTrue(isGhostPodBoolean) && BooleanUtils.isTrue(willBePreemptedBoolean)) {
            return SlotState.RESERVED;
        }

        if (BooleanUtils.isFalse(isGhostPodBoolean)) {
            return SlotState.OCCUPIED;
        }

        //Unreachable
        log.error("Pod: {} is in an unhandled state, IsGhostPod: {}, willBePreempted: {}!",
                ApplicationPodView.wrap(pod),
                isGhostPodBoolean,
                willBePreemptedBoolean);
        return SlotState.ERROR;

    }


    private UpdateControl<Slot> updateStatus(@Nonnull Slot resource, @Nonnull PodsPerNode desiredPods) {
        var status = desiredPods.getPods().stream()
                                .map(pod -> new SlotOccupationStatus(
                                        deriveStateFromPod(pod),
                                        pod.getName(),
                                        pod.getNodeName(),
                                        pod.getSlotId() + "")).collect(Collectors.toList());
        resource.getStatus().setSlots(status);
        return UpdateControl.updateStatus(resource);
    }

    private void reportProblemIfNotEnoughAllocatableResourcesPerNode(
            SlotProblems.SlotProblemsBuilder builder,
            NodeList nodeList,
            ClusterRequestResources clusterRequestResources,
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
                                  .withNamespace(getNamespace())
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

    private String getNamespace() {
        return "default";
    }

    <T extends HasMetadata> ResourceID toResourceID(T resource) {
        return new ResourceID(resource.getMetadata().getName(), resource.getMetadata().getNamespace());
    }


    private List<SlotProblems.Problem> nodeEligibleForSlots(Node node, ClusterRequestResources nodeRequestMap,
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

        var terminatingPodInformer = kubernetesClient.pods()
                                                     .withLabel(SLOT_POD_LABEL_NAME)
                                                     .runnableInformer(0);

        return List.of(
                new InformerEventSource<>(terminatingPodInformer, (pod) -> {
                    var view = ApplicationPodView.wrap(pod);
                    log.info("Pod triggered reconciliation: {}", view);

                    return view.getLabel(SLOT_POD_LABEL_NAME)
                               .map(slotsByName::get)
                               .map(Set::of)
                               .orElse(Collections.emptySet());
                }),
                new InformerEventSource<>(nodes, (node) -> {
                    var slotsByLabel = node.getMetadata().getLabels().keySet().stream()
                                           .flatMap(labelName -> this.slots.getOrDefault(labelName,
                                                                             Collections.emptySet())
                                                                           .stream())
                                           .collect(Collectors.toSet());
                    if (!slotsByLabel.isEmpty())
                        return slotsByLabel;

                    // No label found on node maybe pods need to be deleted
                    return this.slots.values().stream().flatMap(Collection::stream).collect(toSet());
                })
        );
    }
}