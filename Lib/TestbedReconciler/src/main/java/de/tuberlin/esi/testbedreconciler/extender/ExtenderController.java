package de.tuberlin.esi.testbedreconciler.extender;

import com.google.common.primitives.Ints;
import de.tuberlin.esi.common.crd.testbed.SlotIDsAnnotationString;
import de.tuberlin.esi.common.crd.testbed.SlotOccupationStatus;
import de.tuberlin.esi.common.crd.testbed.SlotState;
import de.tuberlin.esi.common.crd.testbed.Testbed;
import de.tuberlin.esi.testbedreconciler.problems.NoFreeSlotsException;
import de.tuberlin.esi.testbedreconciler.problems.PreemptionNotApplicableException;
import de.tuberlin.esi.testbedreconciler.reconciler.ApplicationPodView;
import io.fabric8.kubernetes.api.model.NodeListBuilder;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.KubernetesClient;
import lombok.RequiredArgsConstructor;
import lombok.Synchronized;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static de.tuberlin.esi.common.constants.SchedulingConstants.EXTENDER_RETRY_COUNTER_ANNOTATION_NAME;
import static de.tuberlin.esi.common.constants.SlotsConstants.SLOT_GHOSTPOD_WILL_BE_PREEMPTED_BY_NAME;
import static de.tuberlin.esi.common.constants.SlotsConstants.SLOT_IDS_NAME;
import static de.tuberlin.esi.common.constants.SlotsConstants.SLOT_POD_LABEL_NAME;
import static de.tuberlin.esi.common.constants.SlotsConstants.SLOT_POD_SLOT_ID_NAME;
import static de.tuberlin.esi.common.constants.SlotsConstants.SLOT_POD_TARGET_NODE_NAME;
import static de.tuberlin.esi.common.crd.NamespacedName.getName;
import static de.tuberlin.esi.common.crd.NamespacedName.getNamespace;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;

@RestController
@RequestMapping("/extender")
@Slf4j
@RequiredArgsConstructor
public class ExtenderController {
    public static final Integer EXTENDER_RETRY_COUNTER_BEFORE_DELETION = 3;
    private final KubernetesClient client;

    private Optional<SlotOccupationStatus> findSlotById(Testbed slots, int position) {
        if (slots.getStatus().getSlots() == null) {
            throw new RuntimeException("Slots are not ready");
        }

        return slots.getStatus().getSlots().stream().filter(slot -> position == slot.getPosition()).findFirst();
    }

    private void debugLogSlots(Testbed slots) {
        log.debug("name:         {}", getName(slots));
        log.debug("namespace     {}", getNamespace(slots));
        log.debug("state:        {}", slots.getStatus().getState());
        log.debug("slots:        {}",
                slots.getStatus().getSlots().stream().map(occ -> occ.getState() == SlotState.FREE)
                     .collect(Collectors.toList()));

        log.debug("reservations: {}", slots.getStatus().getSlots().stream().map(occ -> occ.getReservedFor())
                                           .collect(Collectors.toList()));


    }

    @Nullable
    private Testbed getSlotsForPod(Pod pod) {
        return client.resources(Testbed.class)
                     .inNamespace(getNamespace(pod))
                     .withName(ApplicationPodView.wrap(pod).getLabel(SLOT_POD_LABEL_NAME)
                                                 .orElseThrow(() -> new RuntimeException("Not " +
                                                         "Interested in Pod")))
                     .get();
    }

    private ApplicationPodView refreshPod(Pod pod) {
        var p = client.pods().inNamespace(pod.getMetadata().getNamespace()).withName(pod.getMetadata().getName()).get();
        return ApplicationPodView.wrap(p);
    }

    @PostMapping("/filter")
    public ExtenderFilterResult filter(@RequestBody ExtenderFilterArgs request) {
        log.info("Filter Request: {}", request);
        var requestPod = refreshPod(request.getPod());
        var nodeNames = request.getNodes().getItems().stream()
                               .map(n -> n.getMetadata().getName())
                               .collect(Collectors.toSet());
        log.info("Potential Nodes: {}", nodeNames);

        if (requestPod.getLabel(SLOT_POD_TARGET_NODE_NAME).isEmpty()) {
            log.info("Pod Label \"{}\" was not set yet", SLOT_POD_TARGET_NODE_NAME);
            try {
                // Return Value is not interesting
                // preemptionInternal will throw and Exception if no slots are available on any of the potential nodes
                this.preemptionInternal(requestPod, nodeNames);
            } catch (NoFreeSlotsException ex) {
                // The filter requests only gets nodes where requested resources are available.
                // However, if the preemptionInternal method finds no free (or reserved) on said node,
                // then the node cannot be used for the requested pod.
                // Returning an empty list of nodes, will trigger preemption on other nodes
                log.info("Preemption did not find any slots on nodes: {}", nodeNames);
                return ExtenderFilterResult.builder()
                                           .nodes(new NodeListBuilder().build())
                                           .nodeNames(emptyList())
                                           .build();
            }
        }

        requestPod = refreshPod(requestPod);


        var targetNodeName =
                requestPod.getLabel(SLOT_POD_TARGET_NODE_NAME)
                          .orElseThrow(() -> new RuntimeException("Preemption did not set the node label"));
        var targetNode =
                request.getNodes().getItems().stream()
                       .filter(n -> n.getMetadata().getName().equals(targetNodeName))
                       .findFirst().orElseThrow(() -> new RuntimeException("Target node not found!"));

        return ExtenderFilterResult.builder()
                                   .nodes(new NodeListBuilder().withItems(targetNode).build())
                                   .nodeNames(singletonList(targetNodeName))
                                   .build();
    }

    private Predicate<SlotOccupationStatus> isFree() {
        return slot -> {
            if (slot.getState() == SlotState.FREE) {
                log.debug("Slot #{} is free thus applicable for preemption", slot.getPosition());
                return true;
            }
            return false;
        };
    }

    private Predicate<SlotOccupationStatus> isReservedForPod(ApplicationPodView requestPod) {
        return slot -> {
            if (slot.getState() == SlotState.RESERVED && requestPod.getMetadata().getName()
                                                                   .equals(slot.getReservedFor())) {
                log.debug("Slot #{} is already reserved for pod", slot.getPosition());
                return true;
            }
            return false;
        };
    }

    /**
     * 1. Extract the Slot target Ids from the SLOT_IDS_NAME label
     * 2. If the pod already has a reserved slot return none
     * 3. If none of the slots on the given set of nodes are free throw an exception
     * 4. find the first free slot on any of the given nodes and mark it as reserved for the pod
     * 5. find the pod that was originally inside the slot and mark it as preempted
     *
     * @param requestPod preemptor pod
     * @param nodes      set of nodes to look for slots
     * @return SlotOccupationStatus of the preempted pod or none of pod already has a reserved slot
     */
    @Synchronized
    private Optional<SlotOccupationStatus> preemptionInternal(ApplicationPodView requestPod, Set<String> nodes) {
        var slotIdsCSL =
                requestPod.getLabel(SLOT_IDS_NAME).orElseThrow(() -> new PreemptionNotApplicableException(requestPod));

        var slotIds = SlotIDsAnnotationString.parse(slotIdsCSL).getSlotIds();
        var slots = verifyTestbedAreReady(getSlotsForPod(requestPod));
        debugLogSlots(slots);

        var reservedSlotOpt = slotIds.stream()
                                     .map(id -> findSlotById(slots, id))
                                     .filter(Optional::isPresent)
                                     .map(Optional::get)
                                     .filter(isReservedForPod(requestPod))
                                     .findFirst();

        if (reservedSlotOpt.isPresent()) {
            updatePodWithSlotId(requestPod, reservedSlotOpt.get());
            log.info("Preemption found an existing reservation for pod: {} on Slot {}#", requestPod,
                    reservedSlotOpt.get().getPosition());
            return reservedSlotOpt;
        }

        var firstFreeSlot = slotIds.stream()
                                   .map(id -> findSlotById(slots, id))
                                   .filter(Optional::isPresent)
                                   .map(Optional::get)
                                   .filter(isFree())
                                   .filter(slot -> nodes.contains(slot.getNodeName()))
                                   .findFirst()
                                   .orElseThrow(() -> NoFreeSlotsException.create(requestPod, slotIds, slots));

        if (firstFreeSlot.getPodUId() == null) {
            throw new RuntimeException("Pod in Slot not Ready");
        }

        // Save old status with old pod
        var oldSlot = new SlotOccupationStatus();
        BeanUtils.copyProperties(firstFreeSlot, oldSlot);
        log.info("Preempting: {} ({})", firstFreeSlot.getPodName(), firstFreeSlot.getPodUId());

        // Update slot status
        markSlotAsPreemptedBy(firstFreeSlot, requestPod);
        updatePodWithSlotId(requestPod, firstFreeSlot);
        reserveSlot(slots, firstFreeSlot, requestPod);
        log.debug("#".repeat(80));
        log.debug("#".repeat(33) + " AFTER UPDATE " + "#".repeat(33));
        log.debug("#".repeat(80));
        debugLogSlots(slots);

        return Optional.of(oldSlot);
    }

    @PostMapping("/preemption")
    public ExtenderPreemptionResult preemption(@RequestBody ExtenderPreemptionArgs request) {
        log.info("Preemption Request: {}", request);
        var requestPod = refreshPod(request.getPod());
        log.info("Potential Nodes: {}", request.getNodeNameToVictims().keySet());

        var freeSlot = preemptionInternal(requestPod, request.getNodeNameToVictims().keySet());

        if (freeSlot.isEmpty()) {
            // Preemption did not find a Free Slot, retry once, and after that the pod is deleted, and the Testbed can
            // recover. Pods will most-likely be restarted.
            var retries =
                    requestPod.setAnnotation(EXTENDER_RETRY_COUNTER_ANNOTATION_NAME).map(Ints::tryParse).orElse(0);
            log.warn("Preemption did not find free slots. Retries: {}", retries);
            if (retries > EXTENDER_RETRY_COUNTER_BEFORE_DELETION) {
                client.pods().inNamespace(requestPod.getNamespace()).withName(requestPod.getName()).delete();
            }
            else {
                client.pods().inNamespace(requestPod.getNamespace()).withName(requestPod.getName()).edit(editPod -> {
                    ApplicationPodView.wrap(editPod).setAnnotation(EXTENDER_RETRY_COUNTER_ANNOTATION_NAME,
                            "" + retries + 1);

                    return editPod;
                });
            }

            return new ExtenderPreemptionResult(emptyMap());
        }

        return new ExtenderPreemptionResult(Map.of(freeSlot.get().getNodeName(),
                new MetaVictims(singletonList(new MetaPod(freeSlot.get().getPodUId())), 0L)));
    }


    @Nonnull
    private SlotOccupationStatus verifySlotStatus(@Nullable SlotOccupationStatus slot) {
        if (slot == null) {
            throw new RuntimeException("Slots do not exist");
        }

        if (slot.getPodName() == null || slot.getNodeName() == null) {
            throw new RuntimeException("Slot is not ready");
        }

        return slot;
    }

    @Nonnull
    private Testbed verifyTestbedAreReady(@Nullable Testbed slots) {
        if (slots == null) {
            throw new RuntimeException("Testbed do not exist");
        }

        if (slots.getStatus().getSlots() == null) {
            throw new RuntimeException("Testbed not ready");
        }

        slots.getStatus().getSlots().forEach(this::verifySlotStatus);

        return slots;
    }

    private void markSlotAsPreemptedBy(SlotOccupationStatus slotToBePreempted, Pod preemptor) {
        log.debug("Marking pod {} as preempted", slotToBePreempted.getPodName());
        client.pods().inNamespace(getNamespace(preemptor)).withName(slotToBePreempted.getPodName()).edit(editPod -> {
            editPod.getMetadata().getLabels().put(SLOT_GHOSTPOD_WILL_BE_PREEMPTED_BY_NAME,
                    preemptor.getMetadata().getName());
            return editPod;
        });
    }

    private void updatePodWithSlotId(Pod requestPod, SlotOccupationStatus status) {
        client.pods().inNamespace(getNamespace(requestPod)).withName(requestPod.getMetadata().getName())
              .edit(editPod -> {
                  editPod.getMetadata().getLabels().putAll(Map.of(
                          SLOT_POD_SLOT_ID_NAME, status.getSlotPositionOnNode() + "",
                          SLOT_POD_TARGET_NODE_NAME, status.getNodeName()
                  ));

                  return editPod;
              });
    }

    public void reserveSlot(Testbed slots, SlotOccupationStatus slot, Pod pod) {
        if (!slots.getStatus().getSlots().contains(slot)) {
            throw new RuntimeException("Trying to reserve slot, that is not part of the StatusReporter");
        }
        var view = ApplicationPodView.wrap(pod);

        client.resources(Testbed.class).inNamespace(getNamespace(slots)).withName(getName(slots))
              .editStatus((editSlots) -> {
                  var slotToBeReserved = editSlots.getStatus().getSlots().stream()
                                                  .filter(editSlot -> editSlot.getPosition() == slot.getPosition())
                                                  .findFirst().orElseThrow(() -> new RuntimeException("Unexpected"));

                  if (slotToBeReserved.getState() != SlotState.FREE) {
                      throw new RuntimeException("Unexpected, Slot should be free");
                  }

                  slotToBeReserved.setState(SlotState.RESERVED);
                  slotToBeReserved.setReservedFor(view.getName());

                  return editSlots;


              });
    }
}
