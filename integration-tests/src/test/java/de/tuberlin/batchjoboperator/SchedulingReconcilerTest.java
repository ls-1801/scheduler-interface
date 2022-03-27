package de.tuberlin.batchjoboperator;

import de.tuberlin.batchjoboperator.batchjobreconciler.reconciler.BatchJobReconciler;
import de.tuberlin.batchjoboperator.common.crd.NamespacedName;
import de.tuberlin.batchjoboperator.common.crd.batchjob.CreationRequest;
import de.tuberlin.batchjoboperator.common.crd.scheduling.Scheduling;
import de.tuberlin.batchjoboperator.common.crd.scheduling.SchedulingSpec;
import de.tuberlin.batchjoboperator.common.crd.scheduling.SchedulingState;
import de.tuberlin.batchjoboperator.common.crd.slots.SlotIDsAnnotationString;
import de.tuberlin.batchjoboperator.common.crd.slots.SlotsStatusState;
import de.tuberlin.batchjoboperator.extender.ExtenderController;
import de.tuberlin.batchjoboperator.schedulingreconciler.SchedulingReconciler;
import de.tuberlin.batchjoboperator.testbedreconciler.TestbedReconciler;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.javaoperatorsdk.operator.api.reconciler.Reconciler;
import k8s.sparkoperator.SparkApplication;
import k8s.sparkoperator.SparkApplicationStatusState;
import k8s.sparkoperator.V1beta2SparkApplicationStatus;
import k8s.sparkoperator.V1beta2SparkApplicationStatusApplicationState;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.assertj.core.util.Lists;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static de.tuberlin.batchjoboperator.common.constants.SlotsConstants.SLOT_IDS_NAME;
import static de.tuberlin.batchjoboperator.common.constants.SlotsConstants.SLOT_POD_IS_GHOSTPOD_NAME;
import static de.tuberlin.batchjoboperator.schedulingreconciler.statemachine.SchedulingCondition.AWAIT_COMPLETION_CONDITION;
import static de.tuberlin.batchjoboperator.schedulingreconciler.statemachine.SchedulingCondition.AWAIT_JOBS_RELEASED_CONDITION;
import static de.tuberlin.batchjoboperator.schedulingreconciler.statemachine.SchedulingCondition.AWAIT_JOBS_SCHEDULED_CONDITION;
import static de.tuberlin.batchjoboperator.schedulingreconciler.statemachine.SchedulingCondition.AWAIT_NUMBER_OF_SLOTS_CONDITION;
import static de.tuberlin.batchjoboperator.schedulingreconciler.statemachine.SchedulingCondition.AWAIT_TESTBED_RELEASED_CONDITION;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@Slf4j
public class SchedulingReconcilerTest extends BaseReconcilerTest {

    private MockKubeScheduler scheduler;

    @Override
    @Before
    public void setUp() throws java.io.FileNotFoundException {
        super.setUp();
        var extender = new ExtenderController(client);
        scheduler = new MockKubeScheduler(client, extender);
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
    protected List<Reconciler> createReconcilers(Supplier<KubernetesClient> clientSupplier) {
        return List.of(
                new BatchJobReconciler(clientSupplier.get()),
                new TestbedReconciler(clientSupplier.get()),
                new SchedulingReconciler(clientSupplier.get()));
    }

    @Override
    protected void registerCRDs() {
        createCRDFromResource("batchjobs.batchjob.gcr.io-v1.yml");
        createCRDFromResource("slots.batchjob.gcr.io-v1.yml");
        createCRDFromResource("schedulings.batchjob.gcr.io-v1.yml");
        createCRDFromResource("flinkclusters.flinkoperator.k8s.io-v1.yml");
        createCRDFromResource("sparkapplications.sparkoperator.k8s.io-v1.yml");

    }


    private Scheduling createScheduling(String name, List<String> jobNames) {
        var scheduling = new Scheduling();
        scheduling.getMetadata().setName(name);
        scheduling.getMetadata().setNamespace(NAMESPACE);

        var spec = new SchedulingSpec();
        spec.setSlots(new NamespacedName(TEST_SLOT_NAME_1, NAMESPACE));
        spec.setQueueBased(jobNames.stream()
                                   .map(jobName -> new NamespacedName(jobName, NAMESPACE))
                                   .collect(Collectors.toList()));
        scheduling.setSpec(spec);
        return client.resources(Scheduling.class).inNamespace(NAMESPACE).create(scheduling);
    }


    private List<String> createQueue(Pair<String, Integer>... nameReplicationPair) {
        return Arrays.stream(nameReplicationPair)
                     .flatMap(p -> IntStream.range(0, p.getValue()).mapToObj(n -> p.getKey()))
                     .collect(Collectors.toList());
    }

    void assertJobEnqueueWasRequested(String name) {
        await().atMost(TIMEOUT_DURATION_IN_SECONDS, TimeUnit.SECONDS).untilAsserted(() -> {
            assertThat(getJob(name))
                    .isNotNull().extracting("spec.activeScheduling")
                    .isEqualTo(new NamespacedName(TEST_SCHEDULING, NAMESPACE));
        });
    }

    void assertJobEnqueueWasNotRequested(String name) {
        await().atMost(TIMEOUT_DURATION_IN_SECONDS, TimeUnit.SECONDS).untilAsserted(() -> {
            assertThat(getJob(name))
                    .isNotNull().extracting("spec.activeScheduling")
                    .isNull();
        });
    }

    void assertApplicationCreationWasRequested(String name, String slotIdString, Integer replication) {
        var ids = SlotIDsAnnotationString.parse(slotIdString).getSlotIds();
        await().atMost(TIMEOUT_DURATION_IN_SECONDS, TimeUnit.SECONDS).untilAsserted(() -> {
            assertThat(getJob(name))
                    .isNotNull().extracting("spec.activeScheduling", "spec.creationRequest")

                    .isEqualTo(Lists.list(new NamespacedName(TEST_SCHEDULING, NAMESPACE),
                            new CreationRequest(ids, new NamespacedName(TEST_SLOT_NAME_1, NAMESPACE), ids.size())));
        });
    }

    void assertApplicationCreationWasNotRequested(String name) {
        await().atMost(TIMEOUT_DURATION_IN_SECONDS, TimeUnit.SECONDS).untilAsserted(() -> {
            assertThat(getJob(name))
                    .isNotNull().extracting("spec.creationRequest")
                    .isEqualTo(null);
        });
    }

    @Test
    public void testMultipleJobsSubmittedAtOnce() {

        createSlot(SlotConfiguration.builder()
                                    .slotsPerNode(2)
                                    .mock(false)
                                    .nodeNames(List.of(TEST_NODE_NAMES[0], TEST_NODE_NAMES[1]))
                                    .build());

        var job1 = createJob("sample2.yaml");
        var job2 = createJob("sample3.yaml");

        assertJobEnqueueWasNotRequested(job1);
        assertJobEnqueueWasNotRequested(job2);

        await().atMost(TIMEOUT_DURATION_IN_SECONDS, TimeUnit.SECONDS).untilAsserted(() -> {
            var testBed = getSlots();
            assertThat(testBed.getStatus()).isNotNull();
            assertThat(testBed.getStatus().getNodeProblems()).isNull();
            assertThat(testBed.getStatus().getProblems()).isNullOrEmpty();
            assertThat(testBed.getStatus().getState()).isEqualTo(SlotsStatusState.SUCCESS);
            assertThat(testBed.getStatus().getSlots()).hasSize(4);
        });


        createScheduling(TEST_SCHEDULING,
                List.of(job2, job1, job2, job1)
        );


        assertJobEnqueueWasRequested(job1);
        assertJobEnqueueWasRequested(job2);

        assertApplicationCreationWasRequested(job2, "0_2", 2);
        assertApplicationCreationWasRequested(job1, "1_3", 2);

        assertSchedulingState(TEST_SCHEDULING, SchedulingState.AwaitingCompletionState);

        assertSchedulingConditions(TEST_SCHEDULING,
                Set.of(AWAIT_JOBS_RELEASED_CONDITION,
                        AWAIT_TESTBED_RELEASED_CONDITION,
                        AWAIT_COMPLETION_CONDITION),
                Set.of()
        );
    }


    @Test
    public void testMultipleJobsButOnlyOneCanBeSubmitted() throws InterruptedException {

        createSlot(SlotConfiguration.builder()
                                    .slotsPerNode(2)
                                    .mock(false)
                                    .nodeNames(List.of(TEST_NODE_NAMES[0]))
                                    .build());

        var job1 = createJob("sample2.yaml");
        var job2 = createJob("sample3.yaml");

        assertJobEnqueueWasNotRequested(job1);
        assertJobEnqueueWasNotRequested(job2);

        await().atMost(TIMEOUT_DURATION_IN_SECONDS, TimeUnit.SECONDS).untilAsserted(() -> {
            var testBed = getSlots();
            assertThat(testBed.getStatus()).isNotNull();
            assertThat(testBed.getStatus().getNodeProblems()).isNull();
            assertThat(testBed.getStatus().getProblems()).isNullOrEmpty();
            assertThat(testBed.getStatus().getState()).isEqualTo(SlotsStatusState.SUCCESS);
            assertThat(testBed.getStatus().getSlots()).hasSize(2);
        });


        createScheduling(TEST_SCHEDULING,
                List.of(job2, job2, job1, job1)
        );


        assertJobEnqueueWasRequested(job2);
        assertJobEnqueueWasRequested(job1);

        assertApplicationCreationWasRequested(job2, "0_1", 2);
        assertApplicationCreationWasNotRequested(job1);

        assertSchedulingState(TEST_SCHEDULING, SchedulingState.ConfirmationState);

        assertSchedulingConditions(TEST_SCHEDULING,
                Set.of(AWAIT_JOBS_SCHEDULED_CONDITION,
                        AWAIT_JOBS_RELEASED_CONDITION,
                        AWAIT_TESTBED_RELEASED_CONDITION,
                        AWAIT_COMPLETION_CONDITION),
                Set.of()
        );

        await().atMost(TIMEOUT_DURATION_IN_SECONDS, TimeUnit.SECONDS).untilAsserted(() -> {
            var spark = getSparkApplications();
            assertThat(spark).hasSize(1);
            assertThat(spark.get(0).getSpec().getExecutor().getCoreRequest())
                    .isEqualTo("900m");
            assertThat(spark.get(0).getSpec().getExecutor().getLabels())
                    .is(keysWithValues(
                            SLOT_POD_IS_GHOSTPOD_NAME, "false",
                            SLOT_IDS_NAME, "0_1"
                    ));
        });

        log.info("######################SLEEEEEEEEEEP############################");
        Thread.sleep(2000);
        log.info("######################SLEEEEEEEEEEP############################");

        createSparkPods(job2, PodInSlotsConfiguration.builder()
                                                     .prefix("Spark-Executor-")
                                                     .cpu("900m")
                                                     .slotIds(Set.of(0, 1))
                                                     .build());

        await().atMost(TIMEOUT_DURATION_IN_SECONDS, TimeUnit.SECONDS).untilAsserted(() -> {
            for (int i = 0; i < 2; i++) {
                var pod = client.pods().inNamespace(NAMESPACE).withName("Spark-Executor-" + i).get();
                assertThat(pod).isNotNull();
                assertThat(pod.getSpec().getNodeName()).isNotNull();
            }
        });
//
        client.resources(SparkApplication.class).inNamespace(NAMESPACE).withName(job2).editStatus((spark) -> {
            spark.setStatus(
                    new V1beta2SparkApplicationStatus()
                            .executorState(Map.of(
                                    "exec1", "scheduled",
                                    "exec2", "scheduled"
                            )));
            return spark;
        });

        Thread.sleep(1000);

        client.resources(SparkApplication.class).inNamespace(NAMESPACE).withName(job2).editStatus((spark) -> {
            spark.setStatus(
                    new V1beta2SparkApplicationStatus()
                            .applicationState(new V1beta2SparkApplicationStatusApplicationState()
                                    .state(SparkApplicationStatusState.RunningState))
                            .executorState(Map.of(
                                    "exec1", "running",
                                    "exec2", "running"
                            )));
            return spark;
        });

        assertSchedulingState(TEST_SCHEDULING, SchedulingState.SubmissionState);

        assertSchedulingConditions(TEST_SCHEDULING,
                Set.of(AWAIT_NUMBER_OF_SLOTS_CONDITION,
                        AWAIT_JOBS_RELEASED_CONDITION,
                        AWAIT_TESTBED_RELEASED_CONDITION,
                        AWAIT_COMPLETION_CONDITION),
                Set.of()
        );
    }

//    @Test
//    void testMultipleQueue() throws InterruptedException {
//        createJob("flink2.yaml");
//        createJob("sample2.yaml");
//        updateSlots(Collections.emptySet(), Collections.emptySet(), SUCCESS);
//
//        assertJobEnqueueWasNotRequested("batchjob-flink2");
//        assertJobEnqueueWasNotRequested("batchjob-flink");
//
//        createScheduling(TEST_SCHEDULING, createQueue(Pair.of("batchjob-flink2", 7), Pair.of("batchjob-flink", 2)));
//
//        assertJobEnqueueWasRequested("batchjob-flink2");
//        assertJobEnqueueWasRequested("batchjob-flink");
//
//        updateBatchJobStatus("batchjob-flink2", BatchJobState.InQueueState);
//        Thread.sleep(100);
//        updateBatchJobStatus("batchjob-flink", BatchJobState.InQueueState);
//        Thread.sleep(100);
//
//        assertApplicationCreationWasRequested("batchjob-flink2", "0_1_2_3_4_5_6", 7);
//        assertApplicationCreationWasNotRequested("batchjob-flink");
//
//        updateSlots(Collections.emptySet(), Set.of(0, 1, 2, 3, 4, 5, 6), RUNNING);
//        Thread.sleep(100);
//        assertApplicationCreationWasNotRequested("batchjob-flink");
//
//        updateBatchJobStatus("batchjob-flink2", BatchJobState.SubmittedState);
//        Thread.sleep(100);
//        assertApplicationCreationWasNotRequested("batchjob-flink");
//
//        updateSlots(Set.of(0, 1, 2, 3, 4, 5, 6), Collections.emptySet(), RUNNING);
//        updateBatchJobStatus("batchjob-flink2", BatchJobState.RunningState);
//        Thread.sleep(100);
//        assertApplicationCreationWasNotRequested("batchjob-flink");
//
//        updateSlots(Set.of(0, 1, 2, 3, 4, 5, 6), Collections.emptySet(), RUNNING);
//        updateBatchJobStatus("batchjob-flink2", BatchJobState.CompletedState);
//        Thread.sleep(100);
//        assertApplicationCreationWasNotRequested("batchjob-flink");
//
//        updateSlots(Set.of(), Collections.emptySet(), SUCCESS);
//        Thread.sleep(100);
//        assertApplicationCreationWasRequested("batchjob-flink", "0_1", 2);
//
//        updateSlots(Collections.emptySet(), Set.of(0, 1), RUNNING);
//        Thread.sleep(100);
//        updateBatchJobStatus("batchjob-flink", BatchJobState.SubmittedState);
//        Thread.sleep(100);
//
//        updateSlots(Set.of(0, 1), Collections.emptySet(), RUNNING);
//        Thread.sleep(100);
//        updateBatchJobStatus("batchjob-flink", BatchJobState.RunningState);
//        Thread.sleep(100);
//
//        updateBatchJobStatus("batchjob-flink", BatchJobState.CompletedState);
//        Thread.sleep(100);
//        updateSlots(Collections.emptySet(), Collections.emptySet(), SUCCESS);
//        Thread.sleep(100);
//
//        await().atMost(TIMEOUT_DURATION_IN_SECONDS, TimeUnit.SECONDS).untilAsserted(() -> {
//            var scheduling = getScheduling(TEST_SCHEDULING);
//            assertThat(scheduling.getStatus().getState()).isEqualTo(SchedulingStatusState.COMPLETED);
//        });
//
//    }
//
//    @Test
//    void newTest() throws InterruptedException {
//        createJob("flink2.yaml");
//        createJob("sample2.yaml");
//        updateBatchJobStatus("batchjob-flink2", BatchJobState.ReadyState);
//        updateSlots(Collections.emptySet(), Collections.emptySet(), SUCCESS);
//        createScheduling(TEST_SCHEDULING, List.of("batchjob-flink2", "batchjob-flink"));
//
//        await().atMost(TIMEOUT_DURATION_IN_SECONDS, TimeUnit.SECONDS).untilAsserted(() -> {
//            var scheduling = getScheduling(TEST_SCHEDULING);
//
//            assertThat(scheduling.getStatus().getState()).isEqualTo(SchedulingStatusState.INITIAL);
//
//            assertThat(scheduling.getStatus().getConditions())
//                    .extracting("condition", "name.name", "name.namespace", "value")
//                    .containsExactlyInAnyOrder(
//                            Tuple.tuple(AWAITING_JOB_ENQUEUE, "batchjob-flink2", NAMESPACE, false),
//                            Tuple.tuple(AWAITING_JOB_ENQUEUE, "batchjob-flink", NAMESPACE, false),
//                            Tuple.tuple(AWAITING_NUMBER_OF_SLOTS_AVAILABLE, "batchjob-flink2", NAMESPACE, true),
//                            Tuple.tuple(AWAITING_NUMBER_OF_SLOTS_AVAILABLE, "batchjob-flink", NAMESPACE, true)
//                    );
//
//            assertThat(getJob("batchjob-flink2"))
//                    .isNotNull().extracting("metadata.labels")
//                    .is(HamcrestCondition.matching(Matchers.<Map<String, String>>allOf(
//                            Matchers.hasEntry(ACTIVE_SCHEDULING_LABEL_NAME, TEST_SCHEDULING),
//                            Matchers.hasEntry(ACTIVE_SCHEDULING_LABEL_NAMESPACE, NAMESPACE)
//                    )));
//
//            assertThat(getJob("batchjob-flink"))
//                    .isNotNull().extracting("metadata.labels")
//                    .is(HamcrestCondition.matching(Matchers.<Map<String, String>>allOf(
//                            Matchers.hasEntry(ACTIVE_SCHEDULING_LABEL_NAME, TEST_SCHEDULING),
//                            Matchers.hasEntry(ACTIVE_SCHEDULING_LABEL_NAMESPACE, NAMESPACE)
//                    )));
//        });
//
//        updateBatchJobStatus("batchjob-flink", BatchJobState.InQueueState);
//
//        await().atMost(TIMEOUT_DURATION_IN_SECONDS, TimeUnit.SECONDS).untilAsserted(() -> {
//            var scheduling = getScheduling(TEST_SCHEDULING);
//
//            assertThat(scheduling.getStatus().getState()).isEqualTo(SchedulingStatusState.INITIAL);
//
//            assertThat(scheduling.getStatus().getConditions())
//                    .extracting("condition", "name.name", "name.namespace", "value")
//                    .containsExactlyInAnyOrder(
//                            Tuple.tuple(AWAITING_JOB_ENQUEUE, "batchjob-flink2", NAMESPACE, false),
//                            Tuple.tuple(AWAITING_JOB_ENQUEUE, "batchjob-flink", NAMESPACE, true),
//                            Tuple.tuple(AWAITING_NUMBER_OF_SLOTS_AVAILABLE, "batchjob-flink2", NAMESPACE, true),
//                            Tuple.tuple(AWAITING_NUMBER_OF_SLOTS_AVAILABLE, "batchjob-flink", NAMESPACE, true)
//                    );
//
//            assertThat(getJob("batchjob-flink2"))
//                    .isNotNull().extracting("metadata.labels")
//                    .is(HamcrestCondition.matching(Matchers.<Map<String, String>>allOf(
//                            Matchers.hasEntry(ACTIVE_SCHEDULING_LABEL_NAME, TEST_SCHEDULING),
//                            Matchers.hasEntry(ACTIVE_SCHEDULING_LABEL_NAMESPACE, NAMESPACE)
//                    )));
//
//            assertThat(getJob("batchjob-flink"))
//                    .isNotNull().extracting("metadata.labels")
//                    .is(HamcrestCondition.matching(Matchers.<Map<String, String>>allOf(
//                            Matchers.hasEntry(ACTIVE_SCHEDULING_LABEL_NAME, TEST_SCHEDULING),
//                            Matchers.hasEntry(ACTIVE_SCHEDULING_LABEL_NAMESPACE, NAMESPACE)
//                    )));
//        });
//
//        updateBatchJobStatus("batchjob-flink2", BatchJobState.InQueueState);
//
//        await().atMost(TIMEOUT_DURATION_IN_SECONDS, TimeUnit.SECONDS).untilAsserted(() -> {
//            var scheduling = getScheduling(TEST_SCHEDULING);
//
//            assertThat(scheduling.getStatus().getState()).isEqualTo(SchedulingStatusState.RUNNING);
//
//            assertThat(scheduling.getStatus().getConditions())
//                    .extracting("condition", "name.name", "name.namespace", "value")
//                    .containsExactlyInAnyOrder(
//                            Tuple.tuple(AWAITING_JOB_ENQUEUE, "batchjob-flink2", NAMESPACE, true),
//                            Tuple.tuple(AWAITING_JOB_SUBMISSION, "batchjob-flink2", NAMESPACE, false),
//                            Tuple.tuple(AWAITING_JOB_COMPLETION, "batchjob-flink2", NAMESPACE, false),
//                            Tuple.tuple(AWAITING_JOB_START, "batchjob-flink2", NAMESPACE, false),
//                            Tuple.tuple(AWAITING_JOB_ENQUEUE, "batchjob-flink", NAMESPACE, true),
//                            Tuple.tuple(AWAITING_JOB_SUBMISSION, "batchjob-flink", NAMESPACE, false),
//                            Tuple.tuple(AWAITING_JOB_COMPLETION, "batchjob-flink", NAMESPACE, false),
//                            Tuple.tuple(AWAITING_JOB_START, "batchjob-flink", NAMESPACE, false),
//                            Tuple.tuple(AWAITING_PRECEDING_JOB_SUBMISSION, "batchjob-flink", NAMESPACE, false),
//                            Tuple.tuple(AWAITING_PRECEDING_JOB_SUBMISSION, "batchjob-flink2", NAMESPACE, false)
//                    );
//
//            assertThat(getJob("batchjob-flink2"))
//                    .isNotNull().extracting("metadata.labels")
//                    .is(HamcrestCondition.matching(Matchers.<Map<String, String>>allOf(
//                            Matchers.hasEntry(ACTIVE_SCHEDULING_LABEL_NAME, TEST_SCHEDULING),
//                            Matchers.hasEntry(ACTIVE_SCHEDULING_LABEL_NAMESPACE, NAMESPACE),
//                            Matchers.hasEntry(APPLICATION_CREATION_REQUEST_SLOT_IDS, "0"),
//                            Matchers.hasEntry(APPLICATION_CREATION_REQUEST_SLOTS_NAME, TEST_SLOT_NAME_1),
//                            Matchers.hasEntry(APPLICATION_CREATION_REQUEST_SLOTS_NAMESPACE, NAMESPACE),
//                            Matchers.hasEntry(APPLICATION_CREATION_REQUEST_REPLICATION, "1")
//                    )));
//
//            assertThat(getJob("batchjob-flink"))
//                    .isNotNull().extracting("metadata.labels")
//                    .is(HamcrestCondition.matching(Matchers.<Map<String, String>>allOf(
//                            Matchers.hasEntry(ACTIVE_SCHEDULING_LABEL_NAME, TEST_SCHEDULING),
//                            Matchers.hasEntry(ACTIVE_SCHEDULING_LABEL_NAMESPACE, NAMESPACE),
//                            Matchers.hasEntry(APPLICATION_CREATION_REQUEST_SLOT_IDS, "1"),
//                            Matchers.hasEntry(APPLICATION_CREATION_REQUEST_SLOTS_NAME, TEST_SLOT_NAME_1),
//                            Matchers.hasEntry(APPLICATION_CREATION_REQUEST_SLOTS_NAMESPACE, NAMESPACE),
//                            Matchers.hasEntry(APPLICATION_CREATION_REQUEST_REPLICATION, "1")
//                    )));
//        });
//
//        updateSlots(Set.of(), Set.of(0, 1), RUNNING);
//        Thread.sleep(100);
//        updateBatchJobStatus("batchjob-flink2", BatchJobState.SubmittedState);
//
//        await().atMost(TIMEOUT_DURATION_IN_SECONDS, TimeUnit.SECONDS).untilAsserted(() -> {
//            var scheduling = getScheduling(TEST_SCHEDULING);
//
//            assertThat(scheduling.getStatus().getState()).isEqualTo(SchedulingStatusState.RUNNING);
//
//            assertThat(scheduling.getStatus().getConditions())
//                    .extracting("condition", "name.name", "name.namespace", "value")
//                    .containsExactlyInAnyOrder(
//                            Tuple.tuple(AWAITING_JOB_ENQUEUE, "batchjob-flink2", NAMESPACE, true),
//                            Tuple.tuple(AWAITING_JOB_ENQUEUE, "batchjob-flink", NAMESPACE, true),
//                            Tuple.tuple(AWAITING_JOB_SUBMISSION, "batchjob-flink2", NAMESPACE, true),
//                            Tuple.tuple(AWAITING_JOB_START, "batchjob-flink2", NAMESPACE, false),
//                            Tuple.tuple(AWAITING_JOB_COMPLETION, "batchjob-flink2", NAMESPACE, false),
//                            Tuple.tuple(AWAITING_JOB_SUBMISSION, "batchjob-flink", NAMESPACE, false),
//                            Tuple.tuple(AWAITING_JOB_START, "batchjob-flink", NAMESPACE, false),
//                            Tuple.tuple(AWAITING_JOB_COMPLETION, "batchjob-flink", NAMESPACE, false),
//                            Tuple.tuple(AWAITING_PRECEDING_JOB_SUBMISSION, "batchjob-flink", NAMESPACE, false),
//                            Tuple.tuple(AWAITING_PRECEDING_JOB_SUBMISSION, "batchjob-flink2", NAMESPACE, true)
//
//                    );
//
//            assertThat(getJob("batchjob-flink2"))
//                    .isNotNull().extracting("metadata.labels")
//                    .is(HamcrestCondition.matching(Matchers.<Map<String, String>>allOf(
//                            Matchers.hasEntry(ACTIVE_SCHEDULING_LABEL_NAME, TEST_SCHEDULING),
//                            Matchers.hasEntry(ACTIVE_SCHEDULING_LABEL_NAMESPACE, NAMESPACE),
//                            Matchers.hasEntry(APPLICATION_CREATION_REQUEST_SLOT_IDS, "0"),
//                            Matchers.hasEntry(APPLICATION_CREATION_REQUEST_SLOTS_NAME, TEST_SLOT_NAME_1),
//                            Matchers.hasEntry(APPLICATION_CREATION_REQUEST_SLOTS_NAMESPACE, NAMESPACE),
//                            Matchers.hasEntry(APPLICATION_CREATION_REQUEST_REPLICATION, "1")
//                    )));
//
//            assertThat(getJob("batchjob-flink"))
//                    .isNotNull().extracting("metadata.labels")
//                    .is(HamcrestCondition.matching(Matchers.<Map<String, String>>allOf(
//                            Matchers.hasEntry(ACTIVE_SCHEDULING_LABEL_NAME, TEST_SCHEDULING),
//                            Matchers.hasEntry(ACTIVE_SCHEDULING_LABEL_NAMESPACE, NAMESPACE),
//                            Matchers.hasEntry(APPLICATION_CREATION_REQUEST_SLOT_IDS, "1"),
//                            Matchers.hasEntry(APPLICATION_CREATION_REQUEST_SLOTS_NAME, TEST_SLOT_NAME_1),
//                            Matchers.hasEntry(APPLICATION_CREATION_REQUEST_SLOTS_NAMESPACE, NAMESPACE),
//                            Matchers.hasEntry(APPLICATION_CREATION_REQUEST_REPLICATION, "1")
//                    )));
//        });
//
//        updateSlots(Set.of(0), Set.of(), RUNNING);
//        Thread.sleep(200);
//
//        await().atMost(TIMEOUT_DURATION_IN_SECONDS, TimeUnit.SECONDS).untilAsserted(() -> {
//            var scheduling = getScheduling(TEST_SCHEDULING);
//
//            assertThat(scheduling.getStatus().getState()).isEqualTo(SchedulingStatusState.RUNNING);
//
//            assertThat(scheduling.getStatus().getConditions())
//                    .extracting("condition", "name.name", "name.namespace", "value")
//                    .containsExactlyInAnyOrder(
//                            Tuple.tuple(AWAITING_JOB_ENQUEUE, "batchjob-flink2", NAMESPACE, true),
//                            Tuple.tuple(AWAITING_JOB_ENQUEUE, "batchjob-flink", NAMESPACE, true),
//                            Tuple.tuple(AWAITING_JOB_SUBMISSION, "batchjob-flink2", NAMESPACE, true),
//                            Tuple.tuple(AWAITING_JOB_START, "batchjob-flink2", NAMESPACE, false),
//                            Tuple.tuple(AWAITING_JOB_COMPLETION, "batchjob-flink2", NAMESPACE, false),
//                            Tuple.tuple(AWAITING_JOB_SUBMISSION, "batchjob-flink", NAMESPACE, false),
//                            Tuple.tuple(AWAITING_JOB_START, "batchjob-flink", NAMESPACE, false),
//                            Tuple.tuple(AWAITING_JOB_COMPLETION, "batchjob-flink", NAMESPACE, false),
//                            Tuple.tuple(AWAITING_PRECEDING_JOB_SUBMISSION, "batchjob-flink", NAMESPACE, false),
//                            Tuple.tuple(AWAITING_PRECEDING_JOB_SUBMISSION, "batchjob-flink2", NAMESPACE, true)
//
//                    );
//
//            assertThat(getJob("batchjob-flink2"))
//                    .isNotNull().extracting("metadata.labels")
//                    .is(HamcrestCondition.matching(Matchers.<Map<String, String>>allOf(
//                            Matchers.hasEntry(ACTIVE_SCHEDULING_LABEL_NAME, TEST_SCHEDULING),
//                            Matchers.hasEntry(ACTIVE_SCHEDULING_LABEL_NAMESPACE, NAMESPACE),
//                            Matchers.hasEntry(APPLICATION_CREATION_REQUEST_SLOT_IDS, "0"),
//                            Matchers.hasEntry(APPLICATION_CREATION_REQUEST_SLOTS_NAME, TEST_SLOT_NAME_1),
//                            Matchers.hasEntry(APPLICATION_CREATION_REQUEST_SLOTS_NAMESPACE, NAMESPACE),
//                            Matchers.hasEntry(APPLICATION_CREATION_REQUEST_REPLICATION, "1")
//                    )));
//
//            assertThat(getJob("batchjob-flink"))
//                    .isNotNull().extracting("metadata.labels")
//                    .is(HamcrestCondition.matching(Matchers.<Map<String, String>>allOf(
//                            Matchers.hasEntry(ACTIVE_SCHEDULING_LABEL_NAME, TEST_SCHEDULING),
//                            Matchers.hasEntry(ACTIVE_SCHEDULING_LABEL_NAMESPACE, NAMESPACE),
//                            Matchers.hasEntry(APPLICATION_CREATION_REQUEST_SLOT_IDS, "1"),
//                            Matchers.hasEntry(APPLICATION_CREATION_REQUEST_SLOTS_NAME, TEST_SLOT_NAME_1),
//                            Matchers.hasEntry(APPLICATION_CREATION_REQUEST_SLOTS_NAMESPACE, NAMESPACE),
//                            Matchers.hasEntry(APPLICATION_CREATION_REQUEST_REPLICATION, "1")
//                    )));
//        });
//
//        updateSlots(Set.of(0), Set.of(1), RUNNING);
//        Thread.sleep(100);
//        updateBatchJobStatus("batchjob-flink", BatchJobState.SubmittedState);
//
//        await().atMost(TIMEOUT_DURATION_IN_SECONDS, TimeUnit.SECONDS).untilAsserted(() -> {
//            var scheduling = getScheduling(TEST_SCHEDULING);
//
//            assertThat(scheduling.getStatus().getState()).isEqualTo(SchedulingStatusState.RUNNING);
//
//            assertThat(scheduling.getStatus().getConditions())
//                    .extracting("condition", "name.name", "name.namespace", "value")
//                    .containsExactlyInAnyOrder(
//                            Tuple.tuple(AWAITING_JOB_ENQUEUE, "batchjob-flink2", NAMESPACE, true),
//                            Tuple.tuple(AWAITING_JOB_ENQUEUE, "batchjob-flink", NAMESPACE, true),
//                            Tuple.tuple(AWAITING_JOB_SUBMISSION, "batchjob-flink2", NAMESPACE, true),
//                            Tuple.tuple(AWAITING_JOB_START, "batchjob-flink2", NAMESPACE, false),
//                            Tuple.tuple(AWAITING_JOB_COMPLETION, "batchjob-flink2", NAMESPACE, false),
//                            Tuple.tuple(AWAITING_JOB_SUBMISSION, "batchjob-flink", NAMESPACE, true),
//                            Tuple.tuple(AWAITING_JOB_START, "batchjob-flink", NAMESPACE, false),
//                            Tuple.tuple(AWAITING_JOB_COMPLETION, "batchjob-flink", NAMESPACE, false),
//                            Tuple.tuple(AWAITING_PRECEDING_JOB_SUBMISSION, "batchjob-flink", NAMESPACE, true),
//                            Tuple.tuple(AWAITING_PRECEDING_JOB_SUBMISSION, "batchjob-flink2", NAMESPACE, true)
//
//                    );
//        });
//
//
//        updateBatchJobStatus("batchjob-flink2", BatchJobState.CompletedState);
//        Thread.sleep(100);
//        updateBatchJobStatus("batchjob-flink", BatchJobState.RunningState);
//
//        await().atMost(TIMEOUT_DURATION_IN_SECONDS, TimeUnit.SECONDS).untilAsserted(() -> {
//            var scheduling = getScheduling(TEST_SCHEDULING);
//
//            assertThat(scheduling.getStatus().getState()).isEqualTo(SchedulingStatusState.RUNNING);
//
//            assertThat(scheduling.getStatus().getConditions())
//                    .extracting("condition", "name.name", "name.namespace", "value")
//                    .containsExactlyInAnyOrder(
//                            Tuple.tuple(AWAITING_JOB_ENQUEUE, "batchjob-flink2", NAMESPACE, true),
//                            Tuple.tuple(AWAITING_JOB_ENQUEUE, "batchjob-flink", NAMESPACE, true),
//                            Tuple.tuple(AWAITING_JOB_SUBMISSION, "batchjob-flink2", NAMESPACE, true),
//                            Tuple.tuple(AWAITING_JOB_START, "batchjob-flink2", NAMESPACE, true),
//                            Tuple.tuple(AWAITING_JOB_COMPLETION, "batchjob-flink2", NAMESPACE, true),
//                            Tuple.tuple(AWAITING_JOB_SUBMISSION, "batchjob-flink", NAMESPACE, true),
//                            Tuple.tuple(AWAITING_JOB_START, "batchjob-flink", NAMESPACE, true),
//                            Tuple.tuple(AWAITING_JOB_COMPLETION, "batchjob-flink", NAMESPACE, false),
//                            Tuple.tuple(AWAITING_PRECEDING_JOB_SUBMISSION, "batchjob-flink", NAMESPACE, true),
//                            Tuple.tuple(AWAITING_PRECEDING_JOB_SUBMISSION, "batchjob-flink2", NAMESPACE, true)
//
//                    );
//        });
//
//        updateBatchJobStatus("batchjob-flink", BatchJobState.CompletedState);
//
//        await().atMost(TIMEOUT_DURATION_IN_SECONDS, TimeUnit.SECONDS).untilAsserted(() -> {
//            var scheduling = getScheduling(TEST_SCHEDULING);
//
//            assertThat(scheduling.getStatus().getState()).isEqualTo(SchedulingStatusState.COMPLETED);
//
//            assertThat(scheduling.getStatus().getConditions())
//                    .extracting("condition", "name.name", "name.namespace", "value")
//                    .containsExactlyInAnyOrder(
//                            Tuple.tuple(AWAITING_JOB_ENQUEUE, "batchjob-flink2", NAMESPACE, true),
//                            Tuple.tuple(AWAITING_JOB_ENQUEUE, "batchjob-flink", NAMESPACE, true),
//                            Tuple.tuple(AWAITING_JOB_SUBMISSION, "batchjob-flink2", NAMESPACE, true),
//                            Tuple.tuple(AWAITING_JOB_START, "batchjob-flink2", NAMESPACE, true),
//                            Tuple.tuple(AWAITING_JOB_COMPLETION, "batchjob-flink2", NAMESPACE, true),
//                            Tuple.tuple(AWAITING_JOB_SUBMISSION, "batchjob-flink", NAMESPACE, true),
//                            Tuple.tuple(AWAITING_JOB_START, "batchjob-flink", NAMESPACE, true),
//                            Tuple.tuple(AWAITING_JOB_COMPLETION, "batchjob-flink", NAMESPACE, true),
//                            Tuple.tuple(AWAITING_PRECEDING_JOB_SUBMISSION, "batchjob-flink", NAMESPACE, true),
//                            Tuple.tuple(AWAITING_PRECEDING_JOB_SUBMISSION, "batchjob-flink2", NAMESPACE, true)
//                    );
//        });
//
//    }
//
//    private BatchJob getJob(String name) {
//        return client.resources(BatchJob.class).inNamespace(NAMESPACE).withName(name).get();
//    }

}
