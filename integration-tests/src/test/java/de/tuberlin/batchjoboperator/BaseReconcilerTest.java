package de.tuberlin.batchjoboperator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import de.tuberlin.batchjoboperator.batchjobreconciler.CustomClonerConfigurationService;
import de.tuberlin.batchjoboperator.batchjobreconciler.reconciler.conditions.BatchJobConditionDeserializer;
import de.tuberlin.batchjoboperator.common.NamespacedName;
import de.tuberlin.batchjoboperator.common.crd.batchjob.BatchJob;
import de.tuberlin.batchjoboperator.common.crd.batchjob.BatchJobState;
import de.tuberlin.batchjoboperator.common.crd.scheduling.Scheduling;
import de.tuberlin.batchjoboperator.common.crd.scheduling.SchedulingSpec;
import de.tuberlin.batchjoboperator.common.crd.slots.Slot;
import de.tuberlin.batchjoboperator.common.crd.slots.SlotOccupationStatus;
import de.tuberlin.batchjoboperator.common.crd.slots.SlotSpec;
import de.tuberlin.batchjoboperator.common.crd.slots.SlotState;
import de.tuberlin.batchjoboperator.common.crd.slots.SlotStatus;
import de.tuberlin.batchjoboperator.common.crd.slots.SlotsStatusState;
import de.tuberlin.batchjoboperator.schedulingreconciler.statemachine.SchedulingJobConditionDeserializer;
import io.fabric8.kubernetes.api.model.Node;
import io.fabric8.kubernetes.api.model.NodeBuilder;
import io.fabric8.kubernetes.api.model.NodeSelectorTerm;
import io.fabric8.kubernetes.api.model.PodBuilder;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.api.model.apiextensions.v1.CustomResourceDefinition;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.server.mock.KubernetesServer;
import io.fabric8.kubernetes.client.utils.Serialization;
import io.javaoperatorsdk.operator.Operator;
import io.javaoperatorsdk.operator.api.reconciler.Reconciler;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Condition;
import org.assertj.core.api.HamcrestCondition;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.Rule;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import javax.annotation.Nonnull;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static de.tuberlin.batchjoboperator.common.constants.SchedulingConstants.ACTIVE_SCHEDULING_LABEL_NAME;
import static de.tuberlin.batchjoboperator.common.constants.SchedulingConstants.ACTIVE_SCHEDULING_LABEL_NAMESPACE;
import static de.tuberlin.batchjoboperator.common.crd.slots.SlotsStatusState.SUCCESS;
import static de.tuberlin.batchjoboperator.common.util.General.enumerate;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.hasEntry;

@Slf4j
public abstract class BaseReconcilerTest {
    protected static final String TEST_NODE_LABEL_1 = "test-node-label-1";
    protected static final String TEST_NODE_LABEL_2 = "test-node-label-2";
    protected static final String TEST_SLOT_NAME_1 = "test-slot-1";
    protected static final String TEST_SLOT_NAME_2 = "test-slot-2";
    protected static final String[] TEST_NODE_NAMES = new String[]{"node-1", "node-2", "node-3", "node-4"};
    protected static final int TIMEOUT_DURATION_IN_SECONDS = 1;
    protected static final String NAMESPACE = "test-namespace";
    protected static final String GHOST_POD_PATTER = "test-ghost-pod-{0}-{1}";
    protected static final String TEST_SCHEDULING = "test-scheduling";

    protected static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(BatchJobConditionDeserializer.getModule())
            .registerModule(SchedulingJobConditionDeserializer.getModule())
            .registerModule(new JavaTimeModule());

    static {
        Serialization.jsonMapper()
                     .registerModule(BatchJobConditionDeserializer.getModule())
                     .registerModule(SchedulingJobConditionDeserializer.getModule());
    }


    @Rule
    public KubernetesServer server = new KubernetesServer(false, true);
    protected KubernetesClient client;
    private Operator operator;

    @BeforeEach
    void setUp() throws FileNotFoundException {
        server.before();
        registerCRDs();

        client = server.getClient();

        Arrays.stream(TEST_NODE_NAMES).forEach(this::createNode);

        var configService = new CustomClonerConfigurationService(MAPPER);
        operator = new Operator(server.getClient(), configService);
        createReconcilers().forEach(reconciler -> {
            var controllerConfiguration = configService.getConfigurationFor(reconciler);
            controllerConfiguration.setConfigurationService(configService);
            operator.register(reconciler);
        });

        operator.start();
    }

    @Nonnull
    protected abstract List<Reconciler> createReconcilers();

    protected abstract void registerCRDs();

    @AfterEach
    void tearDown() {
        operator.stop();
        server.after();
    }


    @SneakyThrows
    protected void createCRDFromResource(String filename) {
        CustomResourceDefinition crd =
                server.getClient().apiextensions().v1().customResourceDefinitions()
                      .load(new FileInputStream("src/main/resources/" + filename))
                      .get();
        CustomResourceDefinition createdCrd = server.getClient().apiextensions().v1()
                                                    .customResourceDefinitions()
                                                    .create(crd);

        assertThat(createdCrd).isNotNull();
    }

    protected void updateSlots(NamespacedName name,
                               Set<Integer> busySlots,
                               Set<Integer> reserveSlots,
                               SlotsStatusState state) {

        var updated = client.resources(Slot.class).inNamespace(name.getNamespace()).withName(name.getName())
                            .editStatus((slots) -> {
                                log.info("Free Slots Before: {}",
                                        slots.getStatus().getSlots().stream()
                                             .map(occ -> occ.getState() == SlotState.FREE)
                                             .collect(Collectors.toList()));

                                enumerate(slots.getStatus().getSlots())
                                        .forEach((pair) -> {
                                            var slot = pair.getKey();
                                            var id = pair.getValue();
                                            slot.setState(SlotState.FREE);
                                            if (busySlots.contains(id)) {
                                                slot.setState(SlotState.OCCUPIED);
                                            }
                                            if (reserveSlots.contains(id)) {
                                                slot.setState(SlotState.RESERVED);
                                            }
                                        });
                                slots.getStatus().setState(state);
                                return slots;
                            });

        log.info("Free Slots Before: {}",
                updated.getStatus().getSlots().stream().map(occ -> occ.getState() == SlotState.FREE)
                       .collect(Collectors.toList()));

    }

    protected void updateSlots(Set<Integer> busySlots, Set<Integer> reserveSlots, SlotsStatusState state) {
        updateSlots(new NamespacedName(TEST_SLOT_NAME_1, NAMESPACE), busySlots, reserveSlots, state);
    }


    protected NamespacedName defaultSlot() {
        return new NamespacedName(TEST_SLOT_NAME_1, NAMESPACE);
    }

    protected NamespacedName defaultScheduling() {
        return new NamespacedName(TEST_SCHEDULING, NAMESPACE);
    }

    protected void claimSlot(NamespacedName scheduling) {
        claimSlot(defaultSlot(), scheduling);
    }

    protected void claimSlot(NamespacedName slotsName, NamespacedName scheduling) {
        client.resources(Slot.class).inNamespace(slotsName.getNamespace()).withName(slotsName.getName())
              .editStatus((slots) -> {

                  if (slots.getMetadata().getLabels() == null) {
                      slots.getMetadata().setLabels(new HashMap<>());
                  }

                  slots.getMetadata().setLabels(Map.of(
                          ACTIVE_SCHEDULING_LABEL_NAME, scheduling.getName(),
                          ACTIVE_SCHEDULING_LABEL_NAMESPACE, scheduling.getNamespace()
                  ));
                  return slots;
              });
    }

    protected void assertJobStatus(String jobName, BatchJobState state) {
        await().atMost(TIMEOUT_DURATION_IN_SECONDS, TimeUnit.SECONDS).untilAsserted(() -> {
            var job = getJob(jobName);
            assertThat(job).extracting("status.state")
                           .as("Job is not in expected state")
                           .isEqualTo(state);
        });
    }

    protected BatchJob getJob(String name) {
        var job = client.resources(BatchJob.class).inNamespace(NAMESPACE).withName(name).get();
        assertThat(job).isNotNull();
        return job;
    }

    protected Condition<Map<String, String>> keysWithValues(String... keysAndValue) {
        assertThat(keysAndValue.length % 2).as("Even number of arguments").isEqualTo(0);

        var list = new Matcher[keysAndValue.length / 2];

        for (int i = 0; i < keysAndValue.length; i += 2) {
            list[i / 2] = hasEntry(keysAndValue[i], keysAndValue[i + 1]);
        }

        return HamcrestCondition.matching(Matchers.<Map<String, String>>allOf(list));
    }


    protected Scheduling createScheduling(List<String> jobNames) {
        return createScheduling(defaultScheduling(), defaultSlot(), jobNames, NAMESPACE);
    }

    protected Scheduling createScheduling(NamespacedName schedulingName, NamespacedName slots, List<String> jobNames,
                                          String jobsNamespace) {
        var scheduling = new Scheduling();
        scheduling.getMetadata().setName(schedulingName.getName());
        scheduling.getMetadata().setNamespace(schedulingName.getNamespace());

        var spec = new SchedulingSpec();
        spec.setSlots(slots);
        spec.setQueueBased(jobNames.stream()
                                   .map(jobName -> new NamespacedName(jobName, jobsNamespace))
                                   .collect(Collectors.toList()));
        scheduling.setSpec(spec);
        return client.resources(Scheduling.class).inNamespace(schedulingName.getNamespace()).create(scheduling);
    }

    protected Slot getSlots(String name) {
        return client.resources(Slot.class).inNamespace(NAMESPACE).withName(name).get();
    }

    protected Slot getSlots() {
        return getSlots(TEST_SLOT_NAME_1);
    }

    protected Slot createSlot() {
        return createSlot(defaultSlot());
    }

    protected Slot createSlot(NamespacedName name) {
        var slot = new Slot();
        slot.getMetadata().setName(name.getName());
        slot.getMetadata().setNamespace(name.getNamespace());


        var status = new SlotStatus();
        var spec = SlotSpec.builder()
                           .nodeLabel(TEST_NODE_LABEL_1)
                           .slotsPerNode(4)
                           .resourcesPerSlot(Map.of("cpu", new Quantity("900m"), "memory", new Quantity("4Gi")))
                           .build();

        status.setState(SUCCESS);

        var slots = enumerate(Arrays.asList(TEST_NODE_NAMES)).flatMap(pair -> {
            var nodeName = pair.getKey();
            var nodeId = pair.getValue();
            return IntStream.range(0, 4).mapToObj(slotPositionOnNode -> new SlotOccupationStatus(
                    SlotState.FREE,
                    MessageFormat.format(GHOST_POD_PATTER, nodeName, slotPositionOnNode + ""),
                    nodeName,
                    nodeId,
                    slotPositionOnNode,
                    UUID.randomUUID().toString()
            ));
        }).collect(Collectors.toList());

        status.setSlots(slots);
        slot.setStatus(status);
        slot.setSpec(spec);
        slot = client.resources(Slot.class).inNamespace(name.getNamespace()).create(slot);

        slot.setStatus(status);
        slot = client.resources(Slot.class).inNamespace(name.getNamespace()).replaceStatus(slot);
        assertThat(slot.getStatus().getSlots()).isNotNull();

        return slot;
    }

    protected void createNode(String name) {
        var node = new NodeBuilder()
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

        client.nodes().create(node);
    }

    protected Node addLabelToNode(String nodeName, String label, String value) {
        var node = client.nodes().withName(nodeName).get();
        assertThat(node).as("Node " + nodeName + " does not exist!").isNotNull();

        node.getMetadata().getLabels().put(label, value);
        return client.nodes().patch(node);
    }

    protected Node removeLabelFromNode(String nodeName, String label) {
        var node = client.nodes().withName(nodeName).get();
        assertThat(node).as("Node " + nodeName + " does not exist!").isNotNull();

        node.getMetadata().getLabels().remove(label);
        return client.nodes().patch(node);
    }

    protected void createPod(NamespacedName podName, Map<String, String> labels, String nodeName) {
        createPod(podName, labels, nodeName, emptyMap());
    }

    protected void createPod(NamespacedName podName, Map<String, String> labels, String nodeName,
                             Map<String, Quantity> resourceRequests) {
        createPod(podName, labels, nodeName, resourceRequests, emptyList());
    }

    protected void createPod(NamespacedName podName, Map<String, String> labels, String nodeName,
                             Map<String, Quantity> resourceRequests, List<NodeSelectorTerm> nodeSelectorTerms) {


        client.pods().inNamespace(podName.getNamespace()).create(new PodBuilder()
                .withNewMetadata()
                .withNamespace(podName.getNamespace())
                .withName(podName.getName())
                .withLabels(labels)
                .endMetadata()
                .withNewSpec()
                .withNewAffinity()
                .withNewNodeAffinity()
                .withNewRequiredDuringSchedulingIgnoredDuringExecution()
                .withNodeSelectorTerms(nodeSelectorTerms)
                .endRequiredDuringSchedulingIgnoredDuringExecution()
                .endNodeAffinity()
                .endAffinity()
                .withNodeName(nodeName)
                .addNewContainer()
                .withNewResources()
                .addToRequests(resourceRequests)
                .endResources()
                .endContainer()
                .endSpec()
                .build());

    }


}
