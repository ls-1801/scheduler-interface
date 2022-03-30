package de.tuberlin.batchjoboperator.integrationtests;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import de.tuberlin.batchjoboperator.batchjobreconciler.reconciler.conditions.BatchJobConditionDeserializer;
import de.tuberlin.batchjoboperator.common.crd.NamespacedName;
import de.tuberlin.batchjoboperator.common.crd.batchjob.BatchJob;
import de.tuberlin.batchjoboperator.common.crd.batchjob.BatchJobState;
import de.tuberlin.batchjoboperator.common.crd.batchjob.BatchJobStatus;
import de.tuberlin.batchjoboperator.common.crd.batchjob.CreationRequest;
import de.tuberlin.batchjoboperator.common.crd.scheduling.Scheduling;
import de.tuberlin.batchjoboperator.common.crd.scheduling.SchedulingSpec;
import de.tuberlin.batchjoboperator.common.crd.scheduling.SchedulingState;
import de.tuberlin.batchjoboperator.common.crd.slots.Slot;
import de.tuberlin.batchjoboperator.common.crd.slots.SlotIDsAnnotationString;
import de.tuberlin.batchjoboperator.common.crd.slots.SlotOccupationStatus;
import de.tuberlin.batchjoboperator.common.crd.slots.SlotSpec;
import de.tuberlin.batchjoboperator.common.crd.slots.SlotState;
import de.tuberlin.batchjoboperator.common.crd.slots.SlotStatus;
import de.tuberlin.batchjoboperator.common.crd.slots.SlotsStatusState;
import de.tuberlin.batchjoboperator.common.reconcilers.CustomClonerConfigurationService;
import de.tuberlin.batchjoboperator.schedulingreconciler.statemachine.SchedulingJobConditionDeserializer;
import io.fabric8.kubernetes.api.model.Node;
import io.fabric8.kubernetes.api.model.NodeBuilder;
import io.fabric8.kubernetes.api.model.NodeSelectorRequirement;
import io.fabric8.kubernetes.api.model.NodeSelectorTerm;
import io.fabric8.kubernetes.api.model.NodeSelectorTermBuilder;
import io.fabric8.kubernetes.api.model.PodBuilder;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.api.model.apiextensions.v1.CustomResourceDefinition;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.server.mock.KubernetesServer;
import io.fabric8.kubernetes.client.utils.Serialization;
import io.javaoperatorsdk.operator.Operator;
import io.javaoperatorsdk.operator.api.reconciler.Reconciler;
import k8s.flinkoperator.FlinkCluster;
import k8s.sparkoperator.SparkApplication;
import lombok.Builder;
import lombok.SneakyThrows;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Condition;
import org.assertj.core.api.HamcrestCondition;
import org.assertj.core.groups.Tuple;
import org.assertj.core.util.Lists;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.platform.commons.util.ClassLoaderUtils;

import java.io.FileNotFoundException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static de.tuberlin.batchjoboperator.common.constants.CommonConstants.FLINK_POD_LABEL;
import static de.tuberlin.batchjoboperator.common.constants.CommonConstants.SPARK_POD_LABEL;
import static de.tuberlin.batchjoboperator.common.constants.SchedulingConstants.ACTIVE_SCHEDULING_LABEL_NAME;
import static de.tuberlin.batchjoboperator.common.constants.SchedulingConstants.ACTIVE_SCHEDULING_LABEL_NAMESPACE;
import static de.tuberlin.batchjoboperator.common.constants.SlotsConstants.SLOT_IDS_NAME;
import static de.tuberlin.batchjoboperator.common.constants.SlotsConstants.SLOT_POD_IS_GHOSTPOD_NAME;
import static de.tuberlin.batchjoboperator.common.constants.SlotsConstants.SLOT_POD_LABEL_NAME;
import static de.tuberlin.batchjoboperator.common.constants.SlotsConstants.SLOT_POD_SLOT_ID_NAME;
import static de.tuberlin.batchjoboperator.common.constants.SlotsConstants.SLOT_POD_TARGET_NODE_NAME;
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
    protected static final int TIMEOUT_DURATION_IN_SECONDS = 2;
    protected static final String NAMESPACE = "test-namespace";
    protected static final String GHOST_POD_PATTER = "test-ghost-pod-{0}-{1}";
    protected static final String TEST_SCHEDULING = "test-scheduling";

    protected static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    static {
        MAPPER.registerModule(SchedulingJobConditionDeserializer.getModule())
              .registerModule(BatchJobConditionDeserializer.getModule());
        Serialization.jsonMapper().registerModule(SchedulingJobConditionDeserializer.getModule())
                     .registerModule(BatchJobConditionDeserializer.getModule());
    }


    public KubernetesServer server;
    protected KubernetesClient client;
    private List<Operator> operators;

    protected static Condition<Map<String, String>> keysWithValues(String... keysAndValue) {
        assertThat(keysAndValue.length % 2).as("Even number of arguments").isEqualTo(0);

        var list = new Matcher[keysAndValue.length / 2];

        for (int i = 0; i < keysAndValue.length; i += 2) {
            list[i / 2] = hasEntry(keysAndValue[i], keysAndValue[i + 1]);
        }

        return HamcrestCondition.matching(Matchers.<Map<String, String>>allOf(list));
    }

    protected abstract List<Reconciler> createReconcilers(Supplier<KubernetesClient> clientSupplier);

    protected abstract void registerCRDs();

    @AfterEach
    void tearDown() {
        operators.forEach(Operator::stop);
        server.after();
    }


    @SneakyThrows
    protected void createCRDFromResource(String filename) {
        CustomResourceDefinition crd =
                server.getClient().apiextensions().v1().customResourceDefinitions()
                      .load(ClassLoaderUtils.getDefaultClassLoader().getResource("crds/" + filename))
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

    @BeforeEach
    public void setUp() throws FileNotFoundException {
        server = new KubernetesServer(false, true);
        server.before();
        var httpLogger = Logger.getLogger("okhttp3.mockwebserver.MockWebServer");
        //httpLogger.setLevel(Level.SEVERE);
        registerCRDs();
        client = server.getClient();

        Arrays.stream(TEST_NODE_NAMES).forEach(this::createNode);

        operators = createReconcilers(() -> server.getKubernetesMockServer().createClient())
                .stream()
                .map(reconciler -> {
                    var client =
                            server.getKubernetesMockServer()
                                  .createClient();
                    var configService =
                            new CustomClonerConfigurationService(MAPPER);
                    var operator =
                            new Operator(client, configService);
                    var controllerConfiguration = configService.getConfigurationFor(reconciler);
                    controllerConfiguration.setConfigurationService(configService);
                    operator.register(reconciler);
                    return operator;
                })
                .collect(Collectors.toList());

        operators.forEach(Operator::start);

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
        return createSlot(SlotConfiguration.builder().build());
    }

    protected Slot createSlot(SlotConfiguration configuration) {
        var slot = new Slot();
        slot.getMetadata().setName(configuration.getName());
        slot.getMetadata().setNamespace(configuration.getNamespace());


        var status = new SlotStatus();
        var spec = SlotSpec.builder()
                           .nodeLabel(configuration.labelName)
                           .slotsPerNode(configuration.slotsPerNode)
                           .resourcesPerSlot(configuration.resourcesPerSlot)
                           .build();

        slot.setSpec(spec);

        slot = client.resources(Slot.class).inNamespace(configuration.namespace).create(slot);
        if (configuration.mock) {
            status.setState(SUCCESS);

            var slots = enumerate(configuration.nodeNames).flatMap(pair -> {
                var nodeName = pair.getKey();
                var nodeId = pair.getValue();
                return IntStream.range(0, configuration.slotsPerNode).mapToObj(slotPositionOnNode ->
                        new SlotOccupationStatus(
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


            slot.setStatus(status);
            slot = client.resources(Slot.class).inNamespace(configuration.namespace).replaceStatus(slot);
            assertThat(slot.getStatus().getSlots()).isNotNull();
        }
        else {
            enumerate(configuration.nodeNames)
                    .forEach(p -> addLabelToNode(p.getKey(), configuration.labelName, p.getValue() + ""));
        }


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
                        "memory", new Quantity("20Gi")
                ))
                .endStatus()
                .build();

        client.nodes().create(node);
    }

    protected Node addLabelToNode(String nodeName, String label, String value) {
        var node = client.nodes().withName(nodeName).get();
        assertThat(node).as("Node " + nodeName + " does not exist!").isNotNull();

        node.getMetadata().getLabels().put(label, value);
        return client.nodes().withName(nodeName).patch(node);
    }

    protected Node removeLabelFromNode(String nodeName, String label) {
        var node = client.nodes().withName(nodeName).get();
        assertThat(node).as("Node " + nodeName + " does not exist!").isNotNull();

        node.getMetadata().getLabels().remove(label);
        return client.nodes().withName(nodeName).patch(node);
    }

    protected void assertJobConditions(String jobName, Set<String> falseCondition, Set<String> trueConditions) {
        await().atMost(TIMEOUT_DURATION_IN_SECONDS, TimeUnit.SECONDS).untilAsserted(() -> {
            var job = getJob(jobName);
            List<Tuple> conditions = new ArrayList<>();
            falseCondition.stream().map(fc -> Tuple.tuple(fc, false)).forEach(conditions::add);
            trueConditions.stream().map(tc -> Tuple.tuple(tc, true)).forEach(conditions::add);


            assertThat(job.getStatus().getConditions())
                    .extracting("condition", "value")
                    .as("Job does not have the expected conditions")
                    .containsAll(conditions);
        });
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

    protected Scheduling getScheduling(String name) {
        var scheduling = client.resources(Scheduling.class).inNamespace(NAMESPACE).withName(name).get();

        assertThat(scheduling).isNotNull();

        return scheduling;
    }

    protected void assertSchedulingState(String schedulingName, SchedulingState state) {
        await().atMost(TIMEOUT_DURATION_IN_SECONDS, TimeUnit.SECONDS).untilAsserted(() -> {
            var scheduling = getScheduling(schedulingName);
            assertThat(scheduling.getStatus().getState()).isEqualTo(state);
        });
    }

    protected void assertSchedulingConditions(String schedulingName,
                                              Set<String> falseCondition,
                                              Set<String> trueConditions) {
        await().atMost(TIMEOUT_DURATION_IN_SECONDS, TimeUnit.SECONDS).untilAsserted(() -> {
            var scheduling = getScheduling(schedulingName);
            List<Tuple> conditions = new ArrayList<>();
            falseCondition.stream().map(fc -> Tuple.tuple(fc, false)).forEach(conditions::add);
            trueConditions.stream().map(tc -> Tuple.tuple(tc, true)).forEach(conditions::add);


            assertThat(scheduling.getStatus().getConditions())
                    .extracting("condition", "value")
                    .as("Job does not have the expected conditions")
                    .containsAll(conditions);
        });
    }

    protected List<FlinkCluster> getFlinkApplications() {
        var flinks = client.resources(FlinkCluster.class).inNamespace(NAMESPACE).list();
        return flinks.getItems();
    }

    protected List<SparkApplication> getSparkApplications() {
        var sparks = client.resources(SparkApplication.class).inNamespace(NAMESPACE).list();
        return sparks.getItems();
    }

    @SneakyThrows
    protected String createJob(String filename) {
        var job = new ObjectMapper(new YAMLFactory())
                .readValue(ClassLoaderUtils.getDefaultClassLoader().getResource("jobs/" + filename), BatchJob.class);

        job.getMetadata().setNamespace(NAMESPACE);

        job = client.resources(BatchJob.class).inNamespace(NAMESPACE).create(job);
        assertThat(job).isNotNull();

        return job.getMetadata().getName();
    }

    protected void mockFlinkPods(String jobName, int replication) {
        for (int i = 0; i < replication; i++) {
            createPod(new NamespacedName("flink-TM-pod" + i, NAMESPACE), Map.of(
                    FLINK_POD_LABEL, jobName, "component", "taskmanager", // Flink
                    //Slot
                    SLOT_POD_SLOT_ID_NAME, "" + i,
                    SLOT_POD_LABEL_NAME, TEST_SLOT_NAME_1,
                    SLOT_POD_IS_GHOSTPOD_NAME, "false",
                    SLOT_POD_TARGET_NODE_NAME, TEST_NODE_NAMES[i % TEST_NODE_NAMES.length]
            ), TEST_NODE_NAMES[i % TEST_NODE_NAMES.length]);
        }
    }

    protected void mockSparkPods(String jobName, int replication) {
        for (int i = 0; i < replication; i++) {
            createPod(new NamespacedName("spark-executor-pod" + i, NAMESPACE), Map.of(
                    SPARK_POD_LABEL, jobName, "spark-role", "executor", // Spark
                    //Slot
                    SLOT_POD_SLOT_ID_NAME, "" + i,
                    SLOT_POD_LABEL_NAME, TEST_SLOT_NAME_1,
                    SLOT_POD_IS_GHOSTPOD_NAME, "false",
                    SLOT_POD_TARGET_NODE_NAME, TEST_NODE_NAMES[i % TEST_NODE_NAMES.length]
            ), TEST_NODE_NAMES[i % TEST_NODE_NAMES.length]);
        }
    }

    protected void createFlinkPods(String job1, PodInSlotsConfiguration configuration) {
        var config = configuration.toBuilder()
                                  .additionalLabels(Map.of(FLINK_POD_LABEL, job1, "component", "taskmanager"))
                                  .build();

        createPodInSlots(config);
    }


    protected void createSparkPods(String jobName, PodInSlotsConfiguration configuration) {
        var config = configuration.toBuilder()
                                  .additionalLabels(Map.of(
                                          SPARK_POD_LABEL, jobName,
                                          "spark-role", "executor"))
                                  .build();

        createPodInSlots(config);
    }

    protected void createPodInSlots(PodInSlotsConfiguration configuration) {
        for (int i = 0; i < configuration.getSlotIds().size(); i++) {

            var labels = new HashMap<>(Map.of(
                    SLOT_POD_LABEL_NAME, configuration.getSlotName(),
                    SLOT_POD_IS_GHOSTPOD_NAME, "false",
                    SLOT_IDS_NAME, SlotIDsAnnotationString.ofIds(configuration.getSlotIds()).toString()));

            labels.putAll(configuration.getAdditionalLabels());

            createPod(new NamespacedName(configuration.getPrefix() + i, configuration.getNamespace()),
                    labels,
                    null,
                    Map.of("cpu", new Quantity(configuration.getCpu()), "memory", new Quantity("4Gi")),
                    List.of(
                            new NodeSelectorTermBuilder()
                                    .withMatchExpressions(new NodeSelectorRequirement(
                                            configuration.getLabelName(),
                                            "In",
                                            configuration.getNodeLabelValues()))
                                    .build()

                    )
            );
        }
    }

    @Builder
    @Value
    protected static class SlotConfiguration {
        @Builder.Default
        String namespace = NAMESPACE;
        @Builder.Default
        String name = TEST_SLOT_NAME_1;
        @Builder.Default
        String labelName = TEST_NODE_LABEL_1;
        @Builder.Default
        int slotsPerNode = 4;
        @Builder.Default
        Map<String, Quantity> resourcesPerSlot = Map.of("cpu", new Quantity("900m"), "memory", new Quantity("4Gi"));

        @Builder.Default
        boolean mock = true;
        @Builder.Default
        List<String> nodeNames = Arrays.asList(TEST_NODE_NAMES);

    }

    @Builder(toBuilder = true)
    @Value
    protected static class PodInSlotsConfiguration {
        @Builder.Default
        String namespace = NAMESPACE;

        @Builder.Default
        String slotName = TEST_SLOT_NAME_1;

        @Builder.Default
        int replication = 4;

        @Builder.Default
        String prefix = "Executor-";

        @Builder.Default
        String labelName = TEST_NODE_LABEL_1;

        @Builder.Default
        String cpu = "1000m";

        @Builder.Default
        Map<String, String> additionalLabels = emptyMap();

        Set<Integer> slotIds;
        List<String> nodeLabelValues;

        public Set<Integer> getSlotIds() {
            if (slotIds == null) return IntStream.range(0, replication).boxed()
                                                 .collect(Collectors.toSet());
            return slotIds;
        }

        public List<String> getNodeLabelValues() {
            if (nodeLabelValues == null)
                return getSlotIds().stream().map(i -> i % TEST_NODE_NAMES.length).map(String::valueOf)
                                   .distinct().collect(Collectors.toList());

            return nodeLabelValues;
        }
    }

    protected void assertJobEnqueueWasRequested(String name) {
        await().atMost(TIMEOUT_DURATION_IN_SECONDS, TimeUnit.SECONDS).untilAsserted(() -> {
            assertThat(getJob(name))
                    .isNotNull().extracting("spec.activeScheduling")
                    .isEqualTo(new NamespacedName(TEST_SCHEDULING, NAMESPACE));
        });
    }

    protected void assertJobEnqueueWasNotRequested(String name) {
        await().atMost(TIMEOUT_DURATION_IN_SECONDS, TimeUnit.SECONDS).untilAsserted(() -> {
            assertThat(getJob(name))
                    .isNotNull().extracting("spec.activeScheduling")
                    .isNull();
        });
    }

    protected void assertApplicationCreationWasRequested(String name, String slotIdString, Integer replication) {
        var ids = SlotIDsAnnotationString.parse(slotIdString).getSlotIds();
        await().atMost(TIMEOUT_DURATION_IN_SECONDS, TimeUnit.SECONDS).untilAsserted(() -> {
            assertThat(getJob(name))
                    .isNotNull().extracting("spec.activeScheduling", "spec.creationRequest")

                    .isEqualTo(Lists.list(new NamespacedName(TEST_SCHEDULING, NAMESPACE),
                            new CreationRequest(ids, new NamespacedName(TEST_SLOT_NAME_1, NAMESPACE), ids.size())));
        });
    }

    protected void assertApplicationCreationWasNotRequested(String name) {
        await().atMost(TIMEOUT_DURATION_IN_SECONDS, TimeUnit.SECONDS).untilAsserted(() -> {
            assertThat(getJob(name))
                    .isNotNull().extracting("spec.creationRequest")
                    .isEqualTo(null);
        });
    }

    protected void changeBatchJobState(String jobName, BatchJobState state) {
        changeBatchJobState(NAMESPACE, jobName, state);
    }

    protected void changeBatchJobState(String namespace, String jobName, BatchJobState state) {
        var editJob = client.resources(BatchJob.class).inNamespace(namespace).withName(jobName).get();
        if (editJob.getStatus() == null) {
            editJob.setStatus(new BatchJobStatus());
            editJob.getStatus().setObservedGeneration(0L);
        }

        editJob.getStatus().setActiveScheduling(editJob.getSpec().getActiveScheduling());

        if (editJob.getSpec().getCreationRequest() != null) {
            editJob.getStatus().setSlots(editJob.getSpec().getCreationRequest().getSlotsName());
            editJob.getStatus().setReplication(editJob.getSpec().getCreationRequest().getReplication());
            editJob.getStatus().setSlotIds(editJob.getSpec().getCreationRequest().getSlotIds());
        }

        editJob.getStatus().setState(state);
        var observedGeneration = Optional.ofNullable(editJob.getStatus().getObservedGeneration()).orElse(0L);
        editJob.getStatus().setObservedGeneration(observedGeneration + 1);

        var job = client.resources(BatchJob.class).inNamespace(NAMESPACE).withName(jobName).updateStatus(editJob);

        assertThat(job.getStatus().getState()).isEqualTo(state);
    }

    protected Scheduling createScheduling(String name, List<String> jobNames) {
        return createScheduling(TEST_SLOT_NAME_1, NAMESPACE, name, jobNames);
    }

    protected Scheduling createScheduling(String testbedName, String namespace, String name, List<String> jobNames) {
        var scheduling = new Scheduling();
        scheduling.getMetadata().setName(name);
        scheduling.getMetadata().setNamespace(namespace);

        var spec = new SchedulingSpec();
        spec.setSlots(new NamespacedName(testbedName, namespace));
        spec.setQueueBased(jobNames.stream()
                                   .map(jobName -> new NamespacedName(jobName, namespace))
                                   .collect(Collectors.toList()));
        scheduling.setSpec(spec);
        return client.resources(Scheduling.class).inNamespace(namespace).create(scheduling);
    }


}
