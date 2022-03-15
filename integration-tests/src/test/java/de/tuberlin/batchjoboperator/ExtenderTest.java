package de.tuberlin.batchjoboperator;

import de.tuberlin.batchjoboperator.common.NamespacedName;
import de.tuberlin.batchjoboperator.common.crd.slots.Slot;
import de.tuberlin.batchjoboperator.common.crd.slots.SlotIDsAnnotationString;
import de.tuberlin.batchjoboperator.common.crd.slots.SlotSpec;
import de.tuberlin.batchjoboperator.common.crd.slots.SlotStatus;
import de.tuberlin.batchjoboperator.common.crd.slots.SlotsStatusState;
import de.tuberlin.batchjoboperator.extender.ExtenderController;
import de.tuberlin.batchjoboperator.slotsreconciler.ApplicationPodView;
import de.tuberlin.batchjoboperator.slotsreconciler.SlotReconciler;
import io.fabric8.kubernetes.api.model.NodeSelectorRequirement;
import io.fabric8.kubernetes.api.model.NodeSelectorTermBuilder;
import io.fabric8.kubernetes.api.model.Quantity;
import io.javaoperatorsdk.operator.api.reconciler.Reconciler;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static de.tuberlin.batchjoboperator.common.constants.SlotsConstants.SLOT_IDS_NAME;
import static de.tuberlin.batchjoboperator.common.constants.SlotsConstants.SLOT_POD_IS_GHOSTPOD_NAME;
import static de.tuberlin.batchjoboperator.common.constants.SlotsConstants.SLOT_POD_LABEL_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

public class ExtenderTest extends BaseReconcilerTest {

    protected MockKubeScheduler scheduler;
    protected ExtenderController uut;

    @Override
    @Before
    public void setUp() throws java.io.FileNotFoundException {
        super.setUp();
        uut = new ExtenderController(client);
        scheduler = new MockKubeScheduler(client, uut);
        scheduler.start();
    }

    @Override
    @After
    public void tearDown() {
        scheduler.stop();
        super.tearDown();
    }

    @Nonnull
    @Override
    protected List<Reconciler> createReconcilers() {
        return Collections.singletonList(new SlotReconciler(client));
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
                                                                                                      "300M")
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

        createPodInSlots(1, TEST_SLOT_NAME_1, "Executor-", TEST_NODE_LABEL_1, "500m");


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
                                     .filter(p -> "Executor-0" .equals(ApplicationPodView.wrap(p).getName()))
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
        slotClient.inNamespace(NAMESPACE).create(createSlot(TEST_SLOT_NAME_1, SlotSpec.builder()
                                                                                      .nodeLabel(TEST_NODE_LABEL_1)
                                                                                      .slotsPerNode(3)
                                                                                      .resourcesPerSlot(Map.of(
                                                                                              "cpu", new Quantity(
                                                                                                      "500m"),
                                                                                              "memory", new Quantity(
                                                                                                      "300M")
                                                                                      ))
                                                                                      .build(), null));

        slotClient.inNamespace(NAMESPACE).create(createSlot(TEST_SLOT_NAME_2, SlotSpec.builder()
                                                                                      .nodeLabel(TEST_NODE_LABEL_2)
                                                                                      .slotsPerNode(2)
                                                                                      .resourcesPerSlot(Map.of(
                                                                                              "cpu", new Quantity(
                                                                                                      "1000m"),
                                                                                              "memory", new Quantity(
                                                                                                      "300M")
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

        createPodInSlots(1, TEST_SLOT_NAME_1, "Executor-", TEST_NODE_LABEL_1, "500m");
        createPodInSlots(2, TEST_SLOT_NAME_2, "Profiler-", TEST_NODE_LABEL_2, "1000m");


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


    void createPodInSlots(Set<Integer> slotIds, List<String> nodeLabelValues, String slotName, String prefix,
                          String labelName, String cpu) {
        for (int i = 0; i < slotIds.size(); i++) {
            createPod(new NamespacedName(prefix + i, NAMESPACE),
                    Map.of(
                            SLOT_POD_LABEL_NAME, slotName,
                            SLOT_POD_IS_GHOSTPOD_NAME, "false",
                            SLOT_IDS_NAME, SlotIDsAnnotationString.ofIds(slotIds).toString()),
                    null,
                    Map.of("cpu", new Quantity(cpu), "memory", new Quantity("300M")),
                    List.of(
                            new NodeSelectorTermBuilder()
                                    .withMatchExpressions(new NodeSelectorRequirement(
                                            labelName,
                                            "In",
                                            nodeLabelValues))
                                    .build()

                    )
            );
        }
    }


    void createPodInSlots(Set<Integer> slotIds, String slotsName, String prefix, String labelName, String cpu) {
        var labelValues = slotIds.stream().map(i -> i % TEST_NODE_NAMES.length).map(String::valueOf)
                                 .distinct().collect(Collectors.toList());
        createPodInSlots(slotIds, labelValues, slotsName, prefix, labelName, cpu);
    }

    void createPodInSlots(int replication, String slotsName, String prefix, String labelName, String cpu) {
        var slotIds = IntStream.range(0, replication).boxed().collect(Collectors.toSet());
        createPodInSlots(slotIds, slotsName, prefix, labelName, cpu);
    }
}
