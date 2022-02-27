package de.tuberlin.batchjoboperator.reconciler;

import de.tuberlin.batchjoboperator.crd.slots.Slot;
import de.tuberlin.batchjoboperator.crd.slots.SlotSpec;
import de.tuberlin.batchjoboperator.crd.slots.SlotStatus;
import de.tuberlin.batchjoboperator.crd.slots.SlotsStatusState;
import de.tuberlin.batchjoboperator.reconciler.slots.ApplicationPodView;
import de.tuberlin.batchjoboperator.reconciler.slots.SlotReconciler;
import io.fabric8.kubernetes.api.model.Node;
import io.fabric8.kubernetes.api.model.NodeBuilder;
import io.fabric8.kubernetes.api.model.PodBuilder;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.api.model.apiextensions.v1.CustomResourceDefinition;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.server.mock.EnableKubernetesMockClient;
import io.fabric8.kubernetes.client.server.mock.KubernetesServer;
import io.javaoperatorsdk.operator.Operator;
import io.javaoperatorsdk.operator.config.runtime.DefaultConfigurationService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.BooleanUtils;
import org.assertj.core.util.Lists;
import org.assertj.core.util.Sets;
import org.junit.Rule;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static de.tuberlin.batchjoboperator.reconciler.slots.SlotReconciler.SLOT_POD_IS_GHOSTPOD_NAME;
import static de.tuberlin.batchjoboperator.reconciler.slots.SlotReconciler.SLOT_POD_LABEL_NAME;
import static de.tuberlin.batchjoboperator.reconciler.slots.SlotReconciler.SLOT_POD_SLOT_ID_NAME;
import static de.tuberlin.batchjoboperator.reconciler.slots.SlotReconciler.SLOT_POD_TARGET_NODE_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@Slf4j
@EnableKubernetesMockClient(https = false, crud = true)
class SlotReconcilerTest {

    public static final String TEST_NODE_LABEL = "test-node-label";
    public static final String TEST_SLOT_NAME = "Test-Slot";
    private static final int TIMEOUT_DURATION_IN_SECONDS = 2;
    @Rule
    public KubernetesServer server = new KubernetesServer(false, true);
    private KubernetesClient client;
    private Operator operator;

    @BeforeEach
    void setUp() throws FileNotFoundException {
        server.before();
        CustomResourceDefinition slotCRD =
                server.getClient().apiextensions().v1().customResourceDefinitions()
                      .load(new FileInputStream("src/main/resources/slots.batchjob.gcr.io-v1.yml"))
                      .get();
        CustomResourceDefinition createdSlotCRD = server.getClient().apiextensions().v1()
                                                        .customResourceDefinitions()
                                                        .create(slotCRD);
        Stream.of(1, 2, 3, 4)
              .forEach(id -> server.getClient().nodes().create(createNode("node-" + id)));

        assertThat(createdSlotCRD).isNotNull();

        client = server.getClient();

        operator = new Operator(server.getClient(), DefaultConfigurationService.instance());
        operator.register(new SlotReconciler(client));
        operator.start();
    }

    @AfterEach
    void tearDown() {
        operator.stop();
        server.after();
    }


    private String namespace() {
        return "default";
    }

    private Slot createSlot(@Nonnull SlotSpec spec, @Nullable SlotStatus status) {
        var slot = new Slot();
        slot.getMetadata().setNamespace(namespace());
        slot.getMetadata().setName(TEST_SLOT_NAME);
        slot.getMetadata().setUid(UUID.randomUUID().toString());

        slot.setSpec(spec);
        slot.setStatus(status);

        return slot;
    }

    private Node createNode(String name) {
        return new NodeBuilder()
                .withNewMetadata()
                .withName(name)
                .withAnnotations(Map.of())
                .withLabels(Map.of())
                .endMetadata()
                .withNewStatus()
                .addToAllocatable(Map.of(
                        "cpu", new Quantity("2000m"),
                        "memory", new Quantity("4Gi")
                ))
                .endStatus()
                .build();
    }

    private Node addLabelToNode(Node n, String label, String value) {
        n.getMetadata().getLabels().put(label, value);
        return n;
    }

    private Node removeLabelFromNode(Node n, String label) {
        n.getMetadata().getLabels().remove(label);
        return n;
    }

    @Test
    void testPreemption() {
        var slotClient = client.resources(Slot.class);
        slotClient.inNamespace(namespace()).create(createSlot(SlotSpec.builder()
                                                                      .nodeLabel(TEST_NODE_LABEL)
                                                                      .slotsPerNode(2)
                                                                      .resourcesPerSlot(Map.of(
                                                                              "cpu", new Quantity("200m"),
                                                                              "memory", new Quantity("300M")
                                                                      ))
                                                                      .build(), null));
        var updatedNode2 = addLabelToNode(client.nodes().withName("node-2").get(), TEST_NODE_LABEL, "1");
        client.nodes().patch(updatedNode2);
        var updatedNode = addLabelToNode(client.nodes().withName("node-1").get(), TEST_NODE_LABEL, "0");
        client.nodes().patch(updatedNode);

        await().atMost(TIMEOUT_DURATION_IN_SECONDS, TimeUnit.SECONDS).untilAsserted(() -> {
            var updatedSlot = slotClient.inNamespace(namespace()).withName(TEST_SLOT_NAME).get();
            assertThat(updatedSlot.getStatus().getObservedGeneration()).isGreaterThan(0);
            assertThat(updatedSlot.getStatus().getState()).isEqualTo(SlotsStatusState.SUCCESS);
            assertThat(updatedSlot.getStatus().getSlots()).isNotNull().hasSize(4);
        });

        var podList = client.pods().inNamespace(namespace()).withLabel(SLOT_POD_LABEL_NAME, TEST_SLOT_NAME).list();
        assertThat(podList.getItems()).hasSize(4);

        //Expect 2 pods on each node, one with id 0 and one with id 1
        assertThat(podList.getItems().stream().filter(pod -> "node-1".equals(pod.getSpec().getNodeName())))
                .hasSize(2)
                .satisfiesAnyOf((pods) -> {
                    assertThat(pods.stream()
                                   .map(pod -> ApplicationPodView.wrap(pod)
                                                                 .getLabel(SLOT_POD_SLOT_ID_NAME).get())
                                   .collect(Collectors.toList()))
                            .isEqualTo(Lists.list("0", "1"));

                });

        assertThat(podList.getItems().stream().filter(pod -> "node-2".equals(pod.getSpec().getNodeName())))
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
        client.pods().inNamespace(namespace()).create(new PodBuilder()
                .withNewMetadata()
                .withName("Executor")
                .withLabels(Map.of(
                        SLOT_POD_SLOT_ID_NAME, "0",
                        SLOT_POD_LABEL_NAME, TEST_SLOT_NAME,
                        SLOT_POD_IS_GHOSTPOD_NAME, "false",
                        SLOT_POD_TARGET_NODE_NAME, "node-1"))
                .endMetadata()
                .withNewSpec()
                .withNodeName("node-1")
                .addNewContainer()
                .withNewResources()
                .addToRequests(Map.of("cpu", new Quantity("200m"), "memory", new Quantity("300M")))
                .endResources()
                .endContainer()
                .endSpec()
                .build());

        await().atMost(TIMEOUT_DURATION_IN_SECONDS, TimeUnit.SECONDS).untilAsserted(() -> {
            var updatedPodList =
                    client.pods().inNamespace(namespace()).withLabel(SLOT_POD_LABEL_NAME, TEST_SLOT_NAME).list();
            assertThat(updatedPodList.getItems()).hasSize(4);

            //Expect 2 pods on each node, one with id 0 and one with id 1
            assertThat(updatedPodList.getItems().stream().filter(pod -> "node-1".equals(pod.getSpec().getNodeName())))
                    .hasSize(2)
                    .satisfiesAnyOf((pods) -> {
                        assertThat(pods.stream()
                                       .map(pod -> ApplicationPodView.wrap(pod)
                                                                     .getLabel(SLOT_POD_SLOT_ID_NAME).get())
                                       .collect(Collectors.toSet()))
                                .isEqualTo(Sets.set("0", "1"));

                    });

            assertThat(updatedPodList.getItems().stream().filter(pod -> "node-2".equals(pod.getSpec().getNodeName())))
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
                                     .filter(p -> !ApplicationPodView.wrap(p).getLabel(SLOT_POD_IS_GHOSTPOD_NAME)
                                                                     .map(BooleanUtils::toBooleanObject).orElse(true)))
                    .hasSize(1);

        });

    }

    @Test
    void testScalingUpAndDoneWhenNodesAreChanging() {

        var slotClient = client.resources(Slot.class);
        slotClient.inNamespace(namespace()).create(createSlot(SlotSpec.builder()
                                                                      .nodeLabel(TEST_NODE_LABEL)
                                                                      .slotsPerNode(2)
                                                                      .resourcesPerSlot(Map.of(
                                                                              "cpu", new Quantity("200m"),
                                                                              "memory", new Quantity("300M")
                                                                      ))
                                                                      .build(), null));


        await().atMost(TIMEOUT_DURATION_IN_SECONDS, TimeUnit.SECONDS).untilAsserted(() -> {
            var updatedSlot = slotClient.inNamespace(namespace()).withName(TEST_SLOT_NAME).get();
            assertThat(updatedSlot.getStatus().getObservedGeneration()).isGreaterThan(0);
            assertThat(updatedSlot.getStatus().getState()).isEqualTo(SlotsStatusState.SUCCESS);
        });

        var updatedNode = addLabelToNode(client.nodes().withName("node-1").get(), TEST_NODE_LABEL, "0");
        client.nodes().patch(updatedNode);

        await().atMost(TIMEOUT_DURATION_IN_SECONDS, TimeUnit.SECONDS).untilAsserted(() -> {
            var updatedSlot = slotClient.inNamespace(namespace()).withName(TEST_SLOT_NAME).get();
            assertThat(updatedSlot.getStatus().getObservedGeneration()).isGreaterThan(0);
            assertThat(updatedSlot.getStatus().getState()).isEqualTo(SlotsStatusState.SUCCESS);
            assertThat(updatedSlot.getStatus().getSlots()).isNotNull().hasSize(2);
        });

        var updatedNode2 = addLabelToNode(client.nodes().withName("node-2").get(), TEST_NODE_LABEL, "1");
        client.nodes().patch(updatedNode2);

        await().atMost(TIMEOUT_DURATION_IN_SECONDS, TimeUnit.SECONDS).untilAsserted(() -> {
            var updatedSlot = slotClient.inNamespace(namespace()).withName(TEST_SLOT_NAME).get();
            assertThat(updatedSlot.getStatus().getObservedGeneration()).isGreaterThan(0);
            assertThat(updatedSlot.getStatus().getState()).isEqualTo(SlotsStatusState.SUCCESS);
            assertThat(updatedSlot.getStatus().getSlots()).isNotNull().hasSize(4);
        });

        var updatedNode3 = addLabelToNode(client.nodes().withName("node-3").get(), TEST_NODE_LABEL, "2");
        client.nodes().patch(updatedNode3);
        var updatedNode4 = addLabelToNode(client.nodes().withName("node-4").get(), TEST_NODE_LABEL, "3");
        client.nodes().patch(updatedNode4);

        await().atMost(TIMEOUT_DURATION_IN_SECONDS, TimeUnit.SECONDS).untilAsserted(() -> {
            var updatedSlot = slotClient.inNamespace(namespace()).withName(TEST_SLOT_NAME).get();
            assertThat(updatedSlot.getStatus().getObservedGeneration()).isGreaterThan(0);
            assertThat(updatedSlot.getStatus().getState()).isEqualTo(SlotsStatusState.SUCCESS);
            assertThat(updatedSlot.getStatus().getSlots()).isNotNull().hasSize(8);
        });

        var updatedNode5 = removeLabelFromNode(client.nodes().withName("node-3").get(), TEST_NODE_LABEL);
        var updatedNode6 = removeLabelFromNode(client.nodes().withName("node-4").get(), TEST_NODE_LABEL);
        client.nodes().patch(updatedNode5);
        client.nodes().patch(updatedNode6);

        await().atMost(TIMEOUT_DURATION_IN_SECONDS, TimeUnit.SECONDS).untilAsserted(() -> {
            var updatedSlot = slotClient.inNamespace(namespace()).withName(TEST_SLOT_NAME).get();
            assertThat(updatedSlot.getStatus().getObservedGeneration()).isGreaterThan(0);
            assertThat(updatedSlot.getStatus().getState()).isEqualTo(SlotsStatusState.SUCCESS);
            assertThat(updatedSlot.getStatus().getSlots()).isNotNull().hasSize(4);
        });

    }
}