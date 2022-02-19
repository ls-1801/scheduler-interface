package de.tuberlin.batchjoboperator.web.external;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import de.tuberlin.batchjoboperator.crd.batchjob.BatchJob;
import de.tuberlin.batchjoboperator.crd.batchjob.BatchJobState;
import de.tuberlin.batchjoboperator.crd.slots.Slot;
import de.tuberlin.batchjoboperator.crd.slots.SlotOccupationStatus;
import de.tuberlin.batchjoboperator.crd.slots.SlotState;
import de.tuberlin.batchjoboperator.reconciler.batchjob.spark.SparkApplicationManagerService;
import de.tuberlin.batchjoboperator.reconciler.slots.ClusterRequestResources;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.api.model.ResourceRequirementsBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.k8s.flinkoperator.FlinkCluster;
import io.k8s.flinkoperator.V1beta1FlinkClusterImageSpec;
import io.k8s.flinkoperator.V1beta1FlinkClusterSpec;
import io.k8s.flinkoperator.V1beta1FlinkClusterSpecBatchScheduler;
import io.k8s.flinkoperator.V1beta1FlinkClusterSpecJobManager;
import io.k8s.flinkoperator.V1beta1FlinkClusterSpecJobManagerPorts;
import io.k8s.flinkoperator.V1beta1FlinkClusterSpecTaskManager;
import io.k8s.sparkoperator.SparkApplication;
import io.k8s.sparkoperator.V1beta2SparkApplicationSpecBatchSchedulerOptions;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static de.tuberlin.batchjoboperator.config.Constants.MANAGED_BY_LABEL_NAME;
import static de.tuberlin.batchjoboperator.config.Constants.MANAGED_BY_LABEL_VALUE;
import static de.tuberlin.batchjoboperator.config.Constants.SCHEDULED_NODE_ANNOTATION_NAME;
import static de.tuberlin.batchjoboperator.reconciler.slots.SlotReconciler.SLOT_POD_IS_GHOSTPOD_NAME;
import static de.tuberlin.batchjoboperator.reconciler.slots.SlotReconciler.SLOT_POD_LABEL_NAME;
import static de.tuberlin.batchjoboperator.reconciler.slots.SlotReconciler.SLOT_POD_SLOT_ID_NAME;
import static de.tuberlin.batchjoboperator.reconciler.slots.SlotReconciler.SLOT_POD_TARGET_NODE_NAME;

@RestController
@RequestMapping("/scheduler")
@RequiredArgsConstructor
@Log4j2
public class ExternalSchedulerController {

    private final KubernetesClient client;
    private final SparkApplicationManagerService service;
    private final boolean failOnProblem = true;


    @GetMapping(value = "/debug/node-set-up")
    public List<String> setupLabelsOnNode(@RequestParam("count") Integer count) {

        var resources = ClusterRequestResources.aggregate(client.pods().list().getItems());
        var slots = client.resources(Slot.class).inNamespace(getNamespace()).list();
        if (slots.getItems().size() != 1) {
            throw new RuntimeException("Only a single Slot is supported");
        }
        var slot = slots.getItems().get(0);

        var nodesWithFittingCapacity = client.nodes().list().getItems()
                                             .stream()
                                             .filter(n -> resources.getRequestedResources(n, "cpu")
                                                                   .compareTo(Quantity.getAmountInBytes(
                                                                           slot
                                                                                   .getSpec()
                                                                                   .getResourcesPerSlot()
                                                                                   .get("cpu"
                                                                                   ))) <= 0)
                                             .sorted(Comparator.comparing(n -> resources.getRequestedResources(n,
                                                     "cpu")))
                                             .limit(count)
                                             .collect(Collectors.toList());

        if (nodesWithFittingCapacity.size() < count) {
            throw new RuntimeException("Not enough nodes with enough capacity");
        }

        for (int i = 0; i < nodesWithFittingCapacity.size(); i++) {
            nodesWithFittingCapacity.get(i).getMetadata().getLabels()
                                    .put(slot.getSpec().getNodeLabel(), i + "");
        }

        nodesWithFittingCapacity.forEach(n -> client.nodes().patch(n));

        return nodesWithFittingCapacity.stream().map(n -> n.getMetadata().getName()).collect(Collectors.toList());

    }

    @GetMapping(value = "/queue")
    public List<NamespacedName> getQueue() {
        var list = client.resources(BatchJob.class)
                         //Sadly this does not seem to be supported at the moment
                         //.withField("Status.State", BatchJobState.InQueueState.name())
                         .list();

        return list.getItems().stream()
                   .filter(job -> BatchJobState.InQueueState.equals(job.getStatus().getState()))
                   .map(NamespacedName::of)
                   .collect(Collectors.toList());

    }

    @PostMapping(value = "/release-from-queue")
    public void releaseFromQueue(@RequestBody ReleaseFromQueueRequest request) throws IOException {

        var slot = client.resources(Slot.class)
                         .inNamespace(getNamespace())
                         .withName(request.getSlotName()).get();

        if (slot == null) {
            throw new RuntimeException("Slot not found");
        }

        var actualJobs = request.getJobs().stream().map(requestJob ->
                                        client.resources(BatchJob.class)
                                              .inNamespace(requestJob.getNamespace())
                                              .withName(requestJob.getName()).get())
                                .filter(Objects::nonNull)
                                .collect(Collectors.toList());


        if (actualJobs.size() != request.getJobs().size()) {
            throw new RuntimeException("Jobs not found");
        }

        if (slot.getStatus().getSlots() == null) {
            throw new RuntimeException("Slot does not appear to be in a valid state");
        }

        var freeSlots = slot.getStatus().getSlots().stream()
                            .sorted(Comparator.comparingInt(occupation -> Integer.parseInt(occupation.getId())))
                            .filter(slotOccupationStatus -> slotOccupationStatus.getState() == SlotState.FREE)
                            .limit(request.getJobs().size())
                            .collect(Collectors.toList());

        var nodes = client.nodes().withLabel(slot.getSpec().getNodeLabel(), null)
                          .list().getItems().stream()
                          .filter(node -> node.getMetadata().getLabels().get(slot.getSpec().getNodeLabel()) != null)
                          .collect(Collectors.toMap(
                                  node -> node.getMetadata().getName(),
                                  node -> node.getMetadata().getLabels()
                                              .get(slot.getSpec().getNodeLabel())));

        if (freeSlots.size() != actualJobs.size() || nodes.isEmpty()) {
            throw new RuntimeException("Not enough free slots");
        }

        for (int i = 0; i < actualJobs.size(); i++) {
            var job = actualJobs.get(i);
            var freeSlot = freeSlots.get(i);

            if (job.getSpec().getSparkSpec() != null) {
                var sparkApp = createSparkAppFromSlot(slot, freeSlot, job, nodes);
                client.resources(SparkApplication.class)
                      .inNamespace(getNamespace())
                      .create(sparkApp);
            }
            else if (job.getSpec().getFlinkSpec() != null) {
                var flinkApp = createFlinkAppFromSlot(slot, freeSlot, job, nodes);
                var mapper = new ObjectMapper(new YAMLFactory())
                        .configure(SerializationFeature.INDENT_OUTPUT, true);
                mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

                log.info(mapper.writeValueAsString(flinkApp));

                client.resources(FlinkCluster.class)
                      .inNamespace(getNamespace())
                      .create(flinkApp);
            }


        }

    }

    private V1beta1FlinkClusterSpecJobManager createJobManagerSpec() {
        return new V1beta1FlinkClusterSpecJobManager()
                .resources(new ResourceRequirementsBuilder()
                        .withLimits(Map.of("memory", Quantity.parse("1024Mi"), "cpu", Quantity.parse("200m")))
                        .build())
                .ports(new V1beta1FlinkClusterSpecJobManagerPorts().ui(8081));
    }

    private V1beta1FlinkClusterSpecTaskManager createTaskManagerSpec(Slot slot, SlotOccupationStatus freeSlot,
                                                                     Map<String, String> nodes) {

        var labelsMap = Map.of(
                SLOT_POD_LABEL_NAME, slot.getMetadata().getName(),
                SLOT_POD_TARGET_NODE_NAME, freeSlot.getNodeName(),
                SLOT_POD_SLOT_ID_NAME, "" + freeSlot.getId(),
                SLOT_POD_IS_GHOSTPOD_NAME, "false");

        return new V1beta1FlinkClusterSpecTaskManager()
                .resources(new ResourceRequirementsBuilder()
                        .withRequests(slot.getSpec().getResourcesPerSlot())
                        .withLimits(slot.getSpec().getResourcesPerSlot())
                        .build())
                .podLabels(labelsMap)
                .replicas(1)
                .nodeSelector(Map.of(slot.getSpec().getNodeLabel(), nodes.get(freeSlot.getNodeName())));

    }

    private FlinkCluster createFlinkAppFromSlot(Slot slot, SlotOccupationStatus freeSlot, BatchJob job, Map<String,
            String> nodes) {
        var flink = new FlinkCluster();
        flink.getMetadata().setName(job.getMetadata().getName());
        flink.getMetadata().setNamespace(getNamespace());

        var clusterSpec = new V1beta1FlinkClusterSpec()
                .batchScheduler(new V1beta1FlinkClusterSpecBatchScheduler()
                        .name("external"))
                .image(new V1beta1FlinkClusterImageSpec().name("flink:1.8.2"))
                .jobManager(createJobManagerSpec())
                .taskManager(createTaskManagerSpec(slot, freeSlot, nodes))
                .job(job.getSpec().getFlinkSpec())
                .flinkProperties(Map.of("taskmanager.numberOfTaskSlots", "1"));

        flink.setSpec(clusterSpec);

        return flink;
    }

    private String getNamespace() {
        return "default";
    }

    @GetMapping(value = "/nodes")
    public List<NodeName> getNodes() {
        return client.nodes().list()
                     .getItems().stream()
                     .map(n -> new NodeName(n.getMetadata().getName()))
                     .collect(Collectors.toList());
    }

    private Predicate<Pair<String, ?>> nodeExist(Set<String> nodes) {
        return e -> {
            if (!nodes.contains(e.getKey()) && failOnProblem) {
                throw new RuntimeException(String.format("Node %s does not exist", e.getKey()));
            }
            return nodes.contains(e.getKey());
        };
    }

    private Predicate<Pair<?, NamespacedName>> jobExists(Set<NamespacedName> jobs) {
        return e -> {
            if (!jobs.contains(e.getValue()) && failOnProblem) {
                throw new RuntimeException(String.format("Job %s in Namespace %s does not exist", e.getValue()
                                                                                                   .getName(),
                        e.getValue()
                         .getNamespace()));
            }
            return jobs.contains(e.getValue());
        };
    }

    private Predicate<Pair<?, NamespacedName>> jobInQueue(Map<NamespacedName, BatchJob> jobs) {
        return e -> {
            if (!BatchJobState.InQueueState.equals(jobs.get(e.getValue()).getStatus().getState()) && failOnProblem) {
                throw new RuntimeException(String.format("Job %s in Namespace %s is not in the Queue", e.getValue()
                                                                                                        .getName(),
                        e.getValue()
                         .getNamespace()));
            }
            return BatchJobState.InQueueState.equals(jobs.get(e.getValue()).getStatus().getState());
        };
    }

    private <T> Function<Pair<String, List<T>>, Stream<Pair<String, T>>> nodeJobPairing() {
        return p -> p.getValue().stream().map(t -> Pair.of(p.getKey(), t));
    }

    private SparkApplication createSparkApplication(SparkApplication sparkApplication) {
        return client.resources(SparkApplication.class)
                     .inNamespace("default")
                     .create(sparkApplication);
    }

    @PostMapping(consumes = "application/json")
    public List<SparkApplication> applyScheduling(@RequestBody Map<String, List<NamespacedName>> schedulingDecision) {
        var nodes = client.nodes().list()
                          .getItems().stream()
                          .map(n -> n.getMetadata().getName())
                          .collect(Collectors.toSet());

        var jobs = client.resources(BatchJob.class)
                         .inAnyNamespace().list().getItems().stream()
                         .collect(Collectors.toMap(NamespacedName::of, Function.identity()));


        return schedulingDecision.entrySet()
                                 .stream().map(e -> Pair.of(e.getKey(), e.getValue()))
                                 // Stream of (Name of Node, List of NamespacedName of BatchJob)
                                 .filter(nodeExist(nodes))
                                 // Only valid nodes left
                                 .flatMap(nodeJobPairing())
                                 // Stream of (Name of Node, NamespacedName of BatchJob)
                                 .filter(jobExists(jobs.keySet()))
                                 // Only Valid jobs left
                                 .filter(jobInQueue(jobs))
                                 .map(pair -> Pair.of(pair.getKey(), jobs.get(pair.getValue())))
                                 .map(pair -> Pair.of(pair.getValue(), createSparkAppOnNode(pair.getKey(),
                                         pair.getValue())))
                                 .map(pair -> createSparkApplication(pair.getValue()))
                                 .collect(Collectors.toList());
    }

    private SparkApplication createSparkAppOnNode(String nodeName, BatchJob job) {
        var sparkApp = new SparkApplication();
        sparkApp.setSpec(job.getSpec().getSparkSpec());
        sparkApp.getMetadata().setName(job.getMetadata().getName());
        sparkApp.addOwnerReference(job);

        try {
            sparkApp.getMetadata().setLabels(Map.of(MANAGED_BY_LABEL_NAME, MANAGED_BY_LABEL_VALUE));
            sparkApp.getSpec().getDriver().getAnnotations().put(SCHEDULED_NODE_ANNOTATION_NAME, nodeName);
            sparkApp.getSpec().getExecutor().getAnnotations().put(SCHEDULED_NODE_ANNOTATION_NAME, nodeName);
        } catch (Exception e) {
            log.error("Something", e);
        }

        return sparkApp;

    }

    private SparkApplication createSparkAppFromSlot(Slot slot, SlotOccupationStatus freeSlot, BatchJob job,
                                                    Map<String, String> nodes) {
        var sparkApp = new SparkApplication();
        sparkApp.setSpec(job.getSpec().getSparkSpec());
        sparkApp.getMetadata().setName(job.getMetadata().getName());
        sparkApp.addOwnerReference(job);

        try {
            sparkApp.getMetadata().setLabels(Map.of(MANAGED_BY_LABEL_NAME, MANAGED_BY_LABEL_VALUE));
//            sparkApp.getSpec().getDriver().getAnnotations().put(SCHEDULED_NODE_ANNOTATION_NAME, slot.getNodeName());

            var nodeSelector = new HashMap<String, String>();
            nodeSelector.put(slot.getSpec().getNodeLabel(), nodes.get(freeSlot.getNodeName()));

            var options = new V1beta2SparkApplicationSpecBatchSchedulerOptions();
            options.setPriorityClassName("high-priority");
            sparkApp.getSpec().setBatchSchedulerOptions(options);

            var labelsMap = Map.of(
                    SLOT_POD_LABEL_NAME, slot.getMetadata().getName(),
                    SLOT_POD_TARGET_NODE_NAME, freeSlot.getNodeName(),
                    SLOT_POD_SLOT_ID_NAME, "" + freeSlot.getId(),
                    SLOT_POD_IS_GHOSTPOD_NAME, "false");

            sparkApp.getSpec().getExecutor().getAnnotations()
                    .put(SCHEDULED_NODE_ANNOTATION_NAME, freeSlot.getNodeName());
            sparkApp.getSpec().getExecutor()
                    .setCoreRequest(slot.getSpec().getResourcesPerSlot().get("cpu").toString());
            sparkApp.getSpec().getExecutor()
                    .getLabels().putAll(labelsMap);
            sparkApp.getSpec().getExecutor()
                    .setNodeSelector(nodeSelector);

            //TODO: SparkOperator has an configurable MemoryOverhead Factor of 1.75
            //      Hardcoding 512m results in a memory request of 896Mi
            sparkApp.getSpec().getExecutor()
                    .setMemory("512m");
            sparkApp.getSpec().getExecutor()
                    .setSchedulerName("my-scheduler");
            sparkApp.getSpec().getExecutor()
                    .setInstances(1);
            sparkApp.getSpec().getExecutor()
                    .setCores(1);

        } catch (Exception e) {
            log.error("Something", e);
        }

        return sparkApp;

    }
}
