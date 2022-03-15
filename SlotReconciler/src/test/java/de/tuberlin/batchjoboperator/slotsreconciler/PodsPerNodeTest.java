package de.tuberlin.batchjoboperator.slotsreconciler;

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodBuilder;
import io.fabric8.kubernetes.api.model.Quantity;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static de.tuberlin.batchjoboperator.common.constants.SlotsConstants.SLOT_POD_IS_GHOSTPOD_NAME;
import static de.tuberlin.batchjoboperator.common.constants.SlotsConstants.SLOT_POD_SLOT_ID_NAME;
import static org.assertj.core.api.Assertions.assertThat;

class PodsPerNodeTest {

    public static final String NODE_1 = "node-1";
    public static final String NODE_2 = "node-2";

    Map<String, Quantity> defaultResourceRequest() {
        return Map.of("cpu", new Quantity("200m"), "memory", new Quantity("300M"));
    }

    Pod createGhostPod(int slot, String node) {
        return new PodBuilder()
                .withNewMetadata()
                .withName("ghost-pod-on-" + node + "-" + slot)
                .withNamespace("default")
                .withLabels(Map.of(
                        SLOT_POD_IS_GHOSTPOD_NAME, "true",
                        SLOT_POD_SLOT_ID_NAME, slot + "")
                )
                .endMetadata()
                .withNewSpec()
                .addNewContainer()
                .withNewResources().addToRequests(defaultResourceRequest()).endResources()
                .endContainer()
                .withNodeName(node)
                .endSpec()
                .build();
    }

    Pod nonGhostPod(int slot, String node, String name) {
        return new PodBuilder()
                .withNewMetadata()
                .withName(name)
                .withNamespace("default")
                .withLabels(Map.of(
                        SLOT_POD_IS_GHOSTPOD_NAME, "false",
                        SLOT_POD_SLOT_ID_NAME, slot + "")
                )
                .endMetadata()
                .withNewSpec()
                .addNewContainer()
                .withNewResources().addToRequests(defaultResourceRequest()).endResources()
                .endContainer()
                .withNodeName(node)
                .endSpec()
                .build();
    }

    @Test
    void desiredState() {
        var observedState = PodsPerNode.groupByNode(List.of(
                createGhostPod(0, NODE_1),
                createGhostPod(1, NODE_2),
                createGhostPod(2, NODE_1),
                createGhostPod(3, NODE_2)));

        var desired = PodsPerNode.groupByNode(List.of(
                createGhostPod(0, NODE_1),
                createGhostPod(1, NODE_2),
                createGhostPod(2, NODE_1),
                createGhostPod(3, NODE_2)));

        var desiredButNotObserved = desired.diff(observedState);
        var observedButNotDesired = observedState.diff(desired);

        assertThat(desiredButNotObserved.getPods()).isEmpty();
        assertThat(observedButNotDesired.getPods()).isEmpty();
    }

    @Test
    void missingPods() {
        var observedState = PodsPerNode.groupByNode(List.of(
                createGhostPod(0, NODE_1),
                createGhostPod(2, NODE_1)));

        var desired = PodsPerNode.groupByNode(List.of(
                createGhostPod(0, NODE_1),
                createGhostPod(1, NODE_2),
                createGhostPod(2, NODE_1),
                createGhostPod(3, NODE_2)));

        var desiredButNotObserved = desired.diff(observedState);
        var observedButNotDesired = observedState.diff(desired);

        assertThat(desiredButNotObserved.getPods()).hasSize(2);
        assertThat(observedButNotDesired.getPods()).isEmpty();
    }

    @Test
    void podsWithInvalidSlots() {
        var observedState = PodsPerNode.groupByNode(List.of(
                createGhostPod(1, NODE_1),
                createGhostPod(2, NODE_1)));

        var desired = PodsPerNode.groupByNode(List.of(
                createGhostPod(0, NODE_1),
                createGhostPod(1, NODE_2),
                createGhostPod(2, NODE_1),
                createGhostPod(3, NODE_2)));

        var desiredButNotObserved = desired.diff(observedState);
        var observedButNotDesired = observedState.diff(desired);

        assertThat(desiredButNotObserved.getPods()).hasSize(3);
        assertThat(observedButNotDesired.getPods()).hasSize(1);
    }

    @Test
    void tooManyPods() {
        var observedState = PodsPerNode.groupByNode(List.of(
                createGhostPod(0, NODE_1),
                createGhostPod(1, NODE_2),
                createGhostPod(2, NODE_1),
                createGhostPod(3, NODE_2)));

        var desired = PodsPerNode.groupByNode(List.of(
                createGhostPod(0, NODE_1),
                createGhostPod(1, NODE_1)));

        var desiredButNotObserved = desired.diff(observedState);
        var observedButNotDesired = observedState.diff(desired);

        assertThat(desiredButNotObserved.getPods()).hasSize(1);
        assertThat(observedButNotDesired.getPods()).hasSize(3);
    }

    @Test
    void testPreemption() {
        var observedState = PodsPerNode.groupByNode(List.of(
                createGhostPod(0, NODE_1),
                nonGhostPod(0, NODE_1, "spark-1"),
                createGhostPod(1, NODE_2),
                createGhostPod(2, NODE_1),
                createGhostPod(3, NODE_2)));

        var desired = PodsPerNode.groupByNode(List.of(
                createGhostPod(0, NODE_1),
                createGhostPod(1, NODE_2),
                createGhostPod(2, NODE_1),
                createGhostPod(3, NODE_2)));


        var preempted = observedState.getPreemptedSlots();
        assertThat(preempted.getPods()).hasSize(1);
        var observedWithOutPreempted = observedState.removePreemptedSlots(preempted);
        assertThat(observedWithOutPreempted.getPods()).hasSize(4);

        var desiredButNotObserved = desired.diff(observedWithOutPreempted);
        var observedButNotDesired = observedWithOutPreempted.diff(desired);

        assertThat(desiredButNotObserved.getPods()).hasSize(0);
        assertThat(observedButNotDesired.getPods()).hasSize(0);
    }
}