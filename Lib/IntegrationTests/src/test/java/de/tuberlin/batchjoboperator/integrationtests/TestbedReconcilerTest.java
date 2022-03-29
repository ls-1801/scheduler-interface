package de.tuberlin.batchjoboperator.integrationtests;

import de.tuberlin.batchjoboperator.common.crd.NamespacedName;
import de.tuberlin.batchjoboperator.common.crd.slots.Slot;
import de.tuberlin.batchjoboperator.common.crd.slots.SlotSpec;
import de.tuberlin.batchjoboperator.common.crd.slots.SlotStatus;
import de.tuberlin.batchjoboperator.common.crd.slots.SlotsStatusState;
import de.tuberlin.batchjoboperator.testbedreconciler.ApplicationPodView;
import de.tuberlin.batchjoboperator.testbedreconciler.TestbedReconciler;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.server.mock.EnableKubernetesMockClient;
import io.javaoperatorsdk.operator.api.reconciler.Reconciler;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.BooleanUtils;
import org.assertj.core.util.Lists;
import org.assertj.core.util.Sets;
import org.junit.jupiter.api.Test;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static de.tuberlin.batchjoboperator.common.constants.SlotsConstants.SLOT_POD_IS_GHOSTPOD_NAME;
import static de.tuberlin.batchjoboperator.common.constants.SlotsConstants.SLOT_POD_LABEL_NAME;
import static de.tuberlin.batchjoboperator.common.constants.SlotsConstants.SLOT_POD_SLOT_ID_NAME;
import static de.tuberlin.batchjoboperator.common.constants.SlotsConstants.SLOT_POD_TARGET_NODE_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@Slf4j
@EnableKubernetesMockClient(https = false, crud = true)
class TestbedReconcilerTest extends BaseReconcilerTest {

    private Slot createSlot(@Nonnull SlotSpec spec, @Nullable SlotStatus status) {
        var slot = new Slot();
        slot.getMetadata().setNamespace(NAMESPACE);
        slot.getMetadata().setName(TEST_SLOT_NAME_1);
        slot.getMetadata().setUid(UUID.randomUUID().toString());

        slot.setSpec(spec);
        slot.setStatus(status);

        return slot;
    }

    @Test
    void testPreemption() {
        var slotClient = client.resources(Slot.class);
        slotClient.inNamespace(NAMESPACE).create(createSlot(SlotSpec.builder()
                                                                    .nodeLabel(TEST_NODE_LABEL_1)
                                                                    .slotsPerNode(2)
                                                                    .resourcesPerSlot(Map.of(
                                                                            "cpu", new Quantity("1000m"),
                                                                            "memory", new Quantity("300M")
                                                                    ))
                                                                    .build(), null));

        addLabelToNode(TEST_NODE_NAMES[0], TEST_NODE_LABEL_1, "0");
        addLabelToNode(TEST_NODE_NAMES[1], TEST_NODE_LABEL_1, "1");


        await().atMost(TIMEOUT_DURATION_IN_SECONDS, TimeUnit.SECONDS).untilAsserted(() -> {
            var updatedSlot = getSlots();
            assertThat(updatedSlot.getStatus().getObservedGeneration()).isGreaterThan(0);
            assertThat(updatedSlot.getStatus().getState()).isEqualTo(SlotsStatusState.SUCCESS);
            assertThat(updatedSlot.getStatus().getSlots()).isNotNull().hasSize(4);
        });

        var podList = client.pods().inNamespace(NAMESPACE).withLabel(SLOT_POD_LABEL_NAME, TEST_SLOT_NAME_1).list();
        assertThat(podList.getItems()).hasSize(4);

        //Expect 2 pods on each node, one with id 0 and one with id 1
        assertThat(podList.getItems().stream().filter(pod -> TEST_NODE_NAMES[0].equals(pod.getSpec().getNodeName())))
                .hasSize(2)
                .satisfiesAnyOf((pods) -> {
                    assertThat(pods.stream()
                                   .map(pod -> ApplicationPodView.wrap(pod)
                                                                 .getLabel(SLOT_POD_SLOT_ID_NAME).get())
                                   .collect(Collectors.toList()))
                            .isEqualTo(Lists.list("0", "1"));

                });

        assertThat(podList.getItems().stream().filter(pod -> TEST_NODE_NAMES[1].equals(pod.getSpec().getNodeName())))
                .hasSize(2)
                .satisfiesAnyOf((pods) -> {
                    assertThat(pods.stream()
                                   .map(pod -> ApplicationPodView.wrap(pod)
                                                                 .getLabel(SLOT_POD_SLOT_ID_NAME)
                                                                 .get())
                                   .collect(Collectors.toList()))
                            .isEqualTo(Lists.list("0", "1"));

                });
        // Expect all 4 pods are ghostpods
        assertThat(podList.getItems().stream()
                          .map(pod -> ApplicationPodView.wrap(pod).getLabel(SLOT_POD_IS_GHOSTPOD_NAME)
                                                        .map(BooleanUtils::toBooleanObject))
                          .filter(Optional::isPresent)
                          .map(Optional::get)
                          .filter(b -> b).count())
                .isEqualTo(4);

        // Test preemption
        // Pod should trigger the preemption of ghostpod-node-1-0
        createPod(new NamespacedName("Executor", NAMESPACE), Map.of(
                        SLOT_POD_SLOT_ID_NAME, "0",
                        SLOT_POD_LABEL_NAME, TEST_SLOT_NAME_1,
                        SLOT_POD_IS_GHOSTPOD_NAME, "false",
                        SLOT_POD_TARGET_NODE_NAME, TEST_NODE_NAMES[0]),
                TEST_NODE_NAMES[0],
                Map.of("cpu", new Quantity("1000m"), "memory", new Quantity("300M"))
        );

        await().atMost(TIMEOUT_DURATION_IN_SECONDS, TimeUnit.SECONDS).untilAsserted(() -> {
            var updatedPodList =
                    client.pods().inNamespace(NAMESPACE).withLabel(SLOT_POD_LABEL_NAME, TEST_SLOT_NAME_1).list();
            assertThat(updatedPodList.getItems()).hasSize(4);

            //Expect 2 pods on each node, one with id 0 and one with id 1
            assertThat(updatedPodList.getItems().stream()
                                     .filter(pod -> TEST_NODE_NAMES[0].equals(pod.getSpec().getNodeName())))
                    .hasSize(2)
                    .satisfiesAnyOf((pods) -> {
                        assertThat(pods.stream()
                                       .map(pod -> ApplicationPodView.wrap(pod)
                                                                     .getLabel(SLOT_POD_SLOT_ID_NAME).get())
                                       .collect(Collectors.toSet()))
                                .isEqualTo(Sets.set("0", "1"));

                    });

            assertThat(updatedPodList.getItems().stream()
                                     .filter(pod -> TEST_NODE_NAMES[1].equals(pod.getSpec().getNodeName())))
                    .hasSize(2)
                    .satisfiesAnyOf((pods) -> {
                        assertThat(pods.stream()
                                       .map(pod -> ApplicationPodView.wrap(pod)
                                                                     .getLabel(SLOT_POD_SLOT_ID_NAME)
                                                                     .get())
                                       .collect(Collectors.toSet()))
                                .isEqualTo(Sets.set("0", "1"));

                    });
            // Expect all 4 pods are ghostpods
            assertThat(updatedPodList.getItems().stream()
                                     .map(pod -> ApplicationPodView.wrap(pod).getLabel(SLOT_POD_IS_GHOSTPOD_NAME)
                                                                   .map(BooleanUtils::toBooleanObject))
                                     .filter(Optional::isPresent)
                                     .map(Optional::get)
                                     .filter(b -> b).count())
                    .isEqualTo(3);

            assertThat(updatedPodList.getItems().stream()
                                     .filter(p -> "Executor".equals(ApplicationPodView.wrap(p).getName()))
                                     .filter(p -> !ApplicationPodView.wrap(p).isGhostPod()))
                    .hasSize(1);

        });

    }


    @Test
    void testScalingUpAndDoneWhenNodesAreChanging() {

        var slotClient = client.resources(Slot.class);
        slotClient.inNamespace(NAMESPACE).create(createSlot(SlotSpec.builder()
                                                                    .nodeLabel(TEST_NODE_LABEL_1)
                                                                    .slotsPerNode(2)
                                                                    .resourcesPerSlot(Map.of(
                                                                            "cpu", new Quantity("1000m"),
                                                                            "memory", new Quantity("300M")
                                                                    ))
                                                                    .build(), null));


        await().atMost(TIMEOUT_DURATION_IN_SECONDS, TimeUnit.SECONDS).untilAsserted(() -> {
            var updatedSlot = getSlots();
            assertThat(updatedSlot.getStatus().getObservedGeneration()).isGreaterThan(0);
            assertThat(updatedSlot.getStatus().getState()).isEqualTo(SlotsStatusState.SUCCESS);
        });

        var updatedNode = addLabelToNode(TEST_NODE_NAMES[0], TEST_NODE_LABEL_1, "0");
        client.nodes().patch(updatedNode);

        await().atMost(TIMEOUT_DURATION_IN_SECONDS, TimeUnit.SECONDS).untilAsserted(() -> {
            var updatedSlot = getSlots();
            assertThat(updatedSlot.getStatus().getObservedGeneration()).isGreaterThan(0);
            assertThat(updatedSlot.getStatus().getState()).isEqualTo(SlotsStatusState.SUCCESS);
            assertThat(updatedSlot.getStatus().getSlots()).isNotNull().hasSize(2);
        });

        var updatedNode2 = addLabelToNode(TEST_NODE_NAMES[1], TEST_NODE_LABEL_1, "1");
        client.nodes().patch(updatedNode2);

        await().atMost(TIMEOUT_DURATION_IN_SECONDS, TimeUnit.SECONDS).untilAsserted(() -> {
            var updatedSlot = getSlots();
            assertThat(updatedSlot.getStatus().getObservedGeneration()).isGreaterThan(0);
            assertThat(updatedSlot.getStatus().getState()).isEqualTo(SlotsStatusState.SUCCESS);
            assertThat(updatedSlot.getStatus().getSlots()).isNotNull().hasSize(4);
        });

        var updatedNode3 = addLabelToNode(TEST_NODE_NAMES[2], TEST_NODE_LABEL_1, "2");
        client.nodes().patch(updatedNode3);
        var updatedNode4 = addLabelToNode(TEST_NODE_NAMES[3], TEST_NODE_LABEL_1, "3");
        client.nodes().patch(updatedNode4);

        await().atMost(TIMEOUT_DURATION_IN_SECONDS, TimeUnit.SECONDS).untilAsserted(() -> {
            var updatedSlot = getSlots();
            assertThat(updatedSlot.getStatus().getObservedGeneration()).isGreaterThan(0);
            assertThat(updatedSlot.getStatus().getState()).isEqualTo(SlotsStatusState.SUCCESS);
            assertThat(updatedSlot.getStatus().getSlots()).isNotNull().hasSize(8);
        });

        var updatedNode5 = removeLabelFromNode(TEST_NODE_NAMES[2], TEST_NODE_LABEL_1);
        var updatedNode6 = removeLabelFromNode(TEST_NODE_NAMES[3], TEST_NODE_LABEL_1);
        client.nodes().patch(updatedNode5);
        client.nodes().patch(updatedNode6);

        await().atMost(TIMEOUT_DURATION_IN_SECONDS, TimeUnit.SECONDS).untilAsserted(() -> {
            var updatedSlot = getSlots();
            assertThat(updatedSlot.getStatus().getObservedGeneration()).isGreaterThan(0);
            assertThat(updatedSlot.getStatus().getState()).isEqualTo(SlotsStatusState.SUCCESS);
            assertThat(updatedSlot.getStatus().getSlots()).isNotNull().hasSize(4);
        });

    }

    @Nonnull
    @Override
    protected List<Reconciler> createReconcilers(Supplier<KubernetesClient> clientSupplier) {
        return Collections.singletonList(new TestbedReconciler(clientSupplier.get(), NAMESPACE));
    }

    @Override
    protected void registerCRDs() {
        createCRDFromResource("slots.batchjob.gcr.io-v1.yml");
    }
}