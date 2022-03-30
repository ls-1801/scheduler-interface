package de.tuberlin.esi.integrationtests;

import de.tuberlin.esi.common.crd.NamespacedName;
import de.tuberlin.esi.common.crd.slots.Slot;
import de.tuberlin.esi.common.crd.slots.SlotSpec;
import de.tuberlin.esi.common.crd.slots.SlotStatus;
import de.tuberlin.esi.common.crd.slots.SlotsStatusState;
import de.tuberlin.esi.testbedreconciler.extender.ExtenderController;
import de.tuberlin.esi.testbedreconciler.reconciler.ApplicationPodView;
import de.tuberlin.esi.testbedreconciler.reconciler.TestbedReconciler;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.javaoperatorsdk.operator.api.reconciler.Reconciler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static de.tuberlin.esi.common.constants.SlotsConstants.SLOT_POD_LABEL_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

public class ExtenderTest extends BaseReconcilerTest {

    protected MockKubeScheduler scheduler;
    protected ExtenderController uut;

    @BeforeEach
    public void setUp() throws java.io.FileNotFoundException {
        super.setUp();
        uut = new ExtenderController(client);
        scheduler = new MockKubeScheduler(client, uut);
        scheduler.start();
    }

    @AfterEach
    public void tearDown() {
        scheduler.stop();
    }

    @Nonnull
    @Override
    protected List<Reconciler> createReconcilers(Supplier<KubernetesClient> clientSupplier) {
        return Collections.singletonList(new TestbedReconciler(clientSupplier.get(), NAMESPACE, true));
    }

    @Override
    protected void registerCRDs() {
        createCRDFromResource("slots.batchjob.gcr.io-v1.yml");
    }

    private Slot createSlot(String name, @Nonnull SlotSpec spec, @Nullable SlotStatus status) {
        var slot = new Slot();
        slot.getMetadata().setNamespace(NAMESPACE);
        slot.getMetadata().setName(name);
        slot.getMetadata().setUid(UUID.randomUUID().toString());

        slot.setSpec(spec);
        slot.setStatus(status);

        return slot;
    }

    @Test
    public void testFilter() {
        var slotClient = client.resources(Slot.class);
        slotClient.inNamespace(NAMESPACE).create(createSlot(TEST_SLOT_NAME_1, SlotSpec.builder()
                                                                                      .nodeLabel(TEST_NODE_LABEL_1)
                                                                                      .slotsPerNode(3)
                                                                                      .resourcesPerSlot(Map.of(
                                                                                              "cpu", new Quantity(
                                                                                                      "500m"),
                                                                                              "memory", new Quantity(
                                                                                                      "4Gi")
                                                                                      ))
                                                                                      .build(), null));

        addLabelToNode(TEST_NODE_NAMES[0], TEST_NODE_LABEL_1, "0");
        addLabelToNode(TEST_NODE_NAMES[1], TEST_NODE_LABEL_1, "1");
        addLabelToNode(TEST_NODE_NAMES[2], TEST_NODE_LABEL_1, "2");
        addLabelToNode(TEST_NODE_NAMES[3], TEST_NODE_LABEL_1, "3");

        await().atMost(TIMEOUT_DURATION_IN_SECONDS, TimeUnit.SECONDS).untilAsserted(() -> {
            var updatedSlot = getSlots();
            assertThat(updatedSlot.getStatus().getObservedGeneration()).isGreaterThan(0);
            assertThat(updatedSlot.getStatus().getState()).isEqualTo(SlotsStatusState.SUCCESS);
            assertThat(updatedSlot.getStatus().getSlots()).isNotNull().hasSize(12);
            updatedSlot.getStatus().getSlots().forEach(occ -> assertThat(occ.getPodUId()).isNotNull());
        });

        createPod(new NamespacedName("nonSlotPod", NAMESPACE),
                Collections.emptyMap(),
                TEST_NODE_NAMES[1],
                Map.of("cpu", new Quantity("400m"), "memory", new Quantity("300M"))
        );

        createPodInSlots(PodInSlotsConfiguration.builder()
                                                .replication(1)
                                                .slotName(TEST_SLOT_NAME_1)
                                                .prefix("Executor-")
                                                .labelName(TEST_NODE_LABEL_1)
                                                .cpu("500m")
                                                .build());


        await().atMost(TIMEOUT_DURATION_IN_SECONDS, TimeUnit.SECONDS).untilAsserted(() -> {
            var pod = client.pods().inNamespace(NAMESPACE).withName("Executor-0").get();
            assertThat(pod.getSpec().getNodeName()).isEqualTo(TEST_NODE_NAMES[0]);
        });

        await().atMost(TIMEOUT_DURATION_IN_SECONDS, TimeUnit.SECONDS).untilAsserted(() -> {
            var updatedSlot = getSlots();
            assertThat(updatedSlot.getStatus().getObservedGeneration()).isGreaterThan(0);
            assertThat(updatedSlot.getStatus().getState()).isEqualTo(SlotsStatusState.RUNNING);
            assertThat(updatedSlot.getStatus().getSlots()).isNotNull().hasSize(12);
            updatedSlot.getStatus().getSlots().forEach(occ -> assertThat(occ.getPodUId()).isNotNull());


            var updatedPodList =
                    client.pods().inNamespace(NAMESPACE).withLabel(SLOT_POD_LABEL_NAME, TEST_SLOT_NAME_1).list();

            assertThat(updatedPodList.getItems().stream()
                                     .filter(p -> "Executor-0".equals(ApplicationPodView.wrap(p).getName()))
                                     .filter(p -> !ApplicationPodView.wrap(p).isGhostPod()))
                    .hasSize(1);

            assertThat(updatedPodList.getItems().stream()
                                     .filter(p -> ApplicationPodView.wrap(p).isGhostPod()))
                    .hasSize(11);

            assertThat(client.pods().inNamespace(NAMESPACE).withName("nonSlotPod").get())
                    .isNotNull();
        });
    }

    @Test
    public void testFilterWithMultipleSlots() {
        var slotClient = client.resources(Slot.class);
        slotClient.inNamespace(NAMESPACE).create(
                createSlot(TEST_SLOT_NAME_1, SlotSpec.builder()
                                                     .nodeLabel(TEST_NODE_LABEL_1)
                                                     .slotsPerNode(3)
                                                     .resourcesPerSlot(Map.of(
                                                             "cpu", new Quantity("500m"),
                                                             "memory", new Quantity("4Gi")
                                                     ))
                                                     .build(), null));

        slotClient.inNamespace(NAMESPACE).create(
                createSlot(TEST_SLOT_NAME_2, SlotSpec.builder()
                                                     .nodeLabel(TEST_NODE_LABEL_2)
                                                     .slotsPerNode(2)
                                                     .resourcesPerSlot(Map.of(
                                                             "cpu", new Quantity("1000m"),
                                                             "memory", new Quantity("4Gi")
                                                     ))
                                                     .build(), null));


        addLabelToNode(TEST_NODE_NAMES[0], TEST_NODE_LABEL_1, "0");
        addLabelToNode(TEST_NODE_NAMES[1], TEST_NODE_LABEL_1, "1");
        addLabelToNode(TEST_NODE_NAMES[2], TEST_NODE_LABEL_1, "2");

        addLabelToNode(TEST_NODE_NAMES[3], TEST_NODE_LABEL_2, "0");

        await().atMost(TIMEOUT_DURATION_IN_SECONDS, TimeUnit.SECONDS).untilAsserted(() -> {
            var updatedSlot = getSlots(TEST_SLOT_NAME_1);
            assertThat(updatedSlot.getStatus().getObservedGeneration()).isGreaterThan(0);
            assertThat(updatedSlot.getStatus().getState()).isEqualTo(SlotsStatusState.SUCCESS);
            assertThat(updatedSlot.getStatus().getSlots()).isNotNull().hasSize(9);
            updatedSlot.getStatus().getSlots().forEach(occ -> assertThat(occ.getPodUId()).isNotNull());

        });

        await().atMost(TIMEOUT_DURATION_IN_SECONDS, TimeUnit.SECONDS).untilAsserted(() -> {
            var updatedSlot = getSlots(TEST_SLOT_NAME_2);
            assertThat(updatedSlot.getStatus().getObservedGeneration()).isGreaterThan(0);
            assertThat(updatedSlot.getStatus().getState()).isEqualTo(SlotsStatusState.SUCCESS);
            assertThat(updatedSlot.getStatus().getSlots()).isNotNull().hasSize(2);
            updatedSlot.getStatus().getSlots().forEach(occ -> assertThat(occ.getPodUId()).isNotNull());
        });

        createPod(new NamespacedName("nonSlotPod", NAMESPACE),
                Collections.emptyMap(),
                TEST_NODE_NAMES[1],
                Map.of("cpu", new Quantity("400m"), "memory", new Quantity("300M"))
        );

        createPodInSlots(PodInSlotsConfiguration.builder()
                                                .replication(1)
                                                .slotName(TEST_SLOT_NAME_1)
                                                .prefix("Executor-")
                                                .labelName(TEST_NODE_LABEL_1)
                                                .cpu("500m")
                                                .build());

        createPodInSlots(PodInSlotsConfiguration.builder()
                                                .replication(2)
                                                .slotName(TEST_SLOT_NAME_2)
                                                .prefix("Profiler-")
                                                .labelName(TEST_NODE_LABEL_2)
                                                .cpu("1000m")
                                                .build());


        await().atMost(TIMEOUT_DURATION_IN_SECONDS, TimeUnit.SECONDS).untilAsserted(() -> {
            var pod = client.pods().inNamespace(NAMESPACE).withName("Executor-0").get();
            assertThat(pod.getSpec().getNodeName()).isEqualTo(TEST_NODE_NAMES[0]);
        });

        await().atMost(TIMEOUT_DURATION_IN_SECONDS, TimeUnit.SECONDS).untilAsserted(() -> {
            var pod1 = client.pods().inNamespace(NAMESPACE).withName("Profiler-0").get();
            var pod2 = client.pods().inNamespace(NAMESPACE).withName("Profiler-1").get();
            assertThat(pod1.getSpec().getNodeName()).isEqualTo(TEST_NODE_NAMES[3]);
            assertThat(pod2.getSpec().getNodeName()).isEqualTo(TEST_NODE_NAMES[3]);
        });


        await().atMost(TIMEOUT_DURATION_IN_SECONDS, TimeUnit.SECONDS).untilAsserted(() -> {
            var updatedSlot = getSlots(TEST_SLOT_NAME_1);
            assertThat(updatedSlot.getStatus().getObservedGeneration()).isGreaterThan(0);
            assertThat(updatedSlot.getStatus().getState()).isEqualTo(SlotsStatusState.RUNNING);
            assertThat(updatedSlot.getStatus().getSlots()).isNotNull().hasSize(9);
            updatedSlot.getStatus().getSlots().forEach(occ -> assertThat(occ.getPodUId()).isNotNull());


            var updatedPodList =
                    client.pods().inNamespace(NAMESPACE).withLabel(SLOT_POD_LABEL_NAME, TEST_SLOT_NAME_1).list();

            assertThat(updatedPodList.getItems().stream()
                                     .filter(p -> !ApplicationPodView.wrap(p).isGhostPod()))
                    .hasSize(1);

            assertThat(updatedPodList.getItems().stream()
                                     .filter(p -> ApplicationPodView.wrap(p).isGhostPod()))
                    .hasSize(8);

            assertThat(client.pods().inNamespace(NAMESPACE).withName("nonSlotPod").get())
                    .isNotNull();
        });

        await().atMost(TIMEOUT_DURATION_IN_SECONDS, TimeUnit.SECONDS).untilAsserted(() -> {
            var updatedSlot = getSlots(TEST_SLOT_NAME_2);
            assertThat(updatedSlot.getStatus().getObservedGeneration()).isGreaterThan(0);
            assertThat(updatedSlot.getStatus().getState()).isEqualTo(SlotsStatusState.RUNNING);
            assertThat(updatedSlot.getStatus().getSlots()).isNotNull().hasSize(2);
            updatedSlot.getStatus().getSlots().forEach(occ -> assertThat(occ.getPodUId()).isNotNull());


            var updatedPodList =
                    client.pods().inNamespace(NAMESPACE).withLabel(SLOT_POD_LABEL_NAME, TEST_SLOT_NAME_2).list();

            assertThat(updatedPodList.getItems().stream()
                                     .filter(p -> !ApplicationPodView.wrap(p).isGhostPod()))
                    .hasSize(2);

            assertThat(updatedPodList.getItems().stream()
                                     .filter(p -> ApplicationPodView.wrap(p).isGhostPod()))
                    .hasSize(0);
        });
    }

}
