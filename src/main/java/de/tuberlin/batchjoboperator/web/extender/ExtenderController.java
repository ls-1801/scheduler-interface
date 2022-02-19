package de.tuberlin.batchjoboperator.web.extender;

import de.tuberlin.batchjoboperator.reconciler.slots.ApplicationPodView;
import io.fabric8.kubernetes.api.model.NodeListBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.Map;

import static de.tuberlin.batchjoboperator.reconciler.slots.SlotReconciler.SLOT_POD_IS_GHOSTPOD_NAME;
import static de.tuberlin.batchjoboperator.reconciler.slots.SlotReconciler.SLOT_POD_SLOT_ID_NAME;
import static de.tuberlin.batchjoboperator.reconciler.slots.SlotReconciler.SLOT_POD_TARGET_NODE_NAME;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;

@RestController
@RequestMapping("/extender")
@Log4j2
@RequiredArgsConstructor
public class ExtenderController {

    private final KubernetesClient client;

    @PostMapping("/filter")
    public ExtenderFilterResult filter(@RequestBody ExtenderArgs request) {
        log.info("Filter Request: {}", request);
        var targetNodeName = request.getPod().getMetadata().getLabels().get(SLOT_POD_TARGET_NODE_NAME);
        if (targetNodeName == null)
            return ExtenderFilterResult.error("Unknown Target Node");

        var targetNode = request.getNodes().getItems().stream()
                                .filter(node -> targetNodeName.equals(node.getMetadata().getName()))
                                .findAny();

        if (targetNode.isEmpty()) {
            return ExtenderFilterResult.error("Target Node does not exist");
        }


        return ExtenderFilterResult.builder()
                                   .nodes(new NodeListBuilder().withItems(targetNode.get()).build())
                                   .nodeNames(Collections.singletonList(targetNodeName))
                                   .build();
    }

    private String getNamespace() {
        return "default";
    }

    @PostMapping("/preemption")
    public ExtenderPreemptionResult preemption(@RequestBody ExtenderPreemptionArgs request) {
        log.info("Preempt Request: {}", request);
        var requestPod = ApplicationPodView.wrap(request.getPod());
        var targetNodeName = requestPod.getLabel(SLOT_POD_TARGET_NODE_NAME);

        var targetId = requestPod.getSlotId();
        if (targetNodeName.isEmpty() || targetId == null) {
            log.info("No TargetNodeName or ID");
            return new ExtenderPreemptionResult(emptyMap());
        }

        var podList = client.pods().inNamespace(getNamespace()).withLabels(
                                    Map.of(SLOT_POD_IS_GHOSTPOD_NAME, "true", SLOT_POD_TARGET_NODE_NAME,
                                            targetNodeName.get(), SLOT_POD_SLOT_ID_NAME,
                                            targetId + ""))
                            .list();

        if (podList.getItems().size() != 1) {
            log.info("Expected a single Slot with id {} on node {}", targetId, targetNodeName);
            return new ExtenderPreemptionResult(emptyMap());
        }

        var targetPod = podList.getItems().get(0);
        var metaTarget = new MetaPod(targetPod.getMetadata().getUid());
        log.info("preempting {}", targetPod.getMetadata().getName());
        return new ExtenderPreemptionResult(Map.of(targetNodeName.get(), new MetaVictims(singletonList(metaTarget),
                0L)));
    }
}
