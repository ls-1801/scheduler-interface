package de.tuberlin.batchjoboperator.integrationtests;

import de.tuberlin.batchjoboperator.batchjobreconciler.reconciler.BatchJobReconciler;
import de.tuberlin.batchjoboperator.common.crd.scheduling.SchedulingState;
import de.tuberlin.batchjoboperator.common.crd.slots.SlotIDsAnnotationString;
import de.tuberlin.batchjoboperator.common.crd.slots.SlotsStatusState;
import de.tuberlin.batchjoboperator.extender.ExtenderController;
import de.tuberlin.batchjoboperator.schedulingreconciler.SchedulingReconciler;
import de.tuberlin.batchjoboperator.testbedreconciler.TestbedReconciler;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.javaoperatorsdk.operator.api.reconciler.Reconciler;
import k8s.flinkoperator.FlinkCluster;
import k8s.flinkoperator.V1beta1FlinkClusterStatus;
import k8s.flinkoperator.V1beta1FlinkClusterStatusComponents;
import k8s.flinkoperator.V1beta1FlinkClusterStatusComponentsConfigMap;
import k8s.sparkoperator.SparkApplication;
import k8s.sparkoperator.SparkApplicationStatusState;
import k8s.sparkoperator.V1beta2SparkApplicationStatus;
import k8s.sparkoperator.V1beta2SparkApplicationStatusApplicationState;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
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

/*
 * This test is more complex as it is including all reconcilers and the Extender. Some of these Testcases are flaky,
 * because they may crash the MockKubernetesServer, which im the current version suffers from concurrent
 * modification exceptions. Version 6.0.0 fixes this issue.
 * */
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

    @After
    public void tearDown() {
        scheduler.stop();
        super.tearDown();
    }

    @Nonnull
    @Override
    protected List<Reconciler> createReconcilers(Supplier<KubernetesClient> clientSupplier) {
        return List.of(
                new BatchJobReconciler(clientSupplier.get(), NAMESPACE, true),
                new TestbedReconciler(clientSupplier.get(), NAMESPACE, true),
                new SchedulingReconciler(clientSupplier.get(), NAMESPACE, true));
    }

    @Override
    protected void registerCRDs() {
        createCRDFromResource("batchjobs.batchjob.gcr.io-v1.yml");
        createCRDFromResource("slots.batchjob.gcr.io-v1.yml");
        createCRDFromResource("schedulings.batchjob.gcr.io-v1.yml");
        createCRDFromResource("flinkclusters.flinkoperator.k8s.io-v1.yml");
        createCRDFromResource("sparkapplications.sparkoperator.k8s.io-v1.yml");

    }

    @SneakyThrows
    void doInOrderButRandom(
            List<Runnable>... tasks
    ) {
        var used = IntStream.range(0, tasks.length).map(i -> tasks[i].size()).boxed()
                            .collect(Collectors.toList());
        var randomActionsInOrder = new ArrayList<Runnable>();
        Random random = new Random();
        while (!used.stream().allMatch(size -> size == 0)) {
            var taskIndex = random.nextInt(tasks.length);
            if (used.get(taskIndex) == 0) {
                continue;
            }

            randomActionsInOrder.add(tasks[taskIndex].get(tasks[taskIndex].size() - used.get(taskIndex)));
            used.set(taskIndex, used.get(taskIndex) - 1);

        }

        for (Runnable action : randomActionsInOrder) {
            Thread.sleep((long) (Math.random() * 1000));
            action.run();
        }

    }

    @SneakyThrows
    @Test
    public void testAcquireState() {
        createSlot(SlotConfiguration.builder()
                                    .slotsPerNode(2)
                                    .mock(false)
                                    .nodeNames(List.of(TEST_NODE_NAMES[0], TEST_NODE_NAMES[1]))
                                    .build());

        var job1 = createJob("sample2.yaml");
        var job2 = createJob("sample3.yaml");
        var job3 = createJob("flink.yaml");
        var job4 = createJob("flink2.yaml");

        var scheduling1 = TEST_SCHEDULING;
        var scheduling2 = TEST_SCHEDULING + "1";

        createScheduling(scheduling1,
                List.of(job2, job1, job2, job3)
        );

        Thread.sleep(1000);

        createScheduling(scheduling2,
                List.of(job2, job1, job2, job4)
        );

        assertSchedulingState(scheduling1, SchedulingState.AwaitingCompletionState);
        assertSchedulingState(scheduling2, SchedulingState.AcquireState);
        assertJobEnqueueWasNotRequested(job4);

        var app1 = new FlinkApplicationMock(job1, Set.of(1)).initialize();
        var app2 = new SparkApplicationMock(job2, Set.of(0, 2)).initialize();
        var app3 = new FlinkApplicationMock(job3, Set.of(3)).initialize();

        doInOrderButRandom(
                app1.getLifecycle(),
                app2.getLifecycle(),
                app3.getLifecycle()
        );

        assertJobEnqueueWasNotRequested(job4);
        assertSchedulingState(scheduling2, SchedulingState.AcquireState);
        assertSchedulingState(scheduling1, SchedulingState.CompletedState);

    }

    /**
     * This tests revealed a bug, where the QueueBased Strategy only waits for a specific number of slots available,
     * however it the test case cannot stop all pods at once and the Testbed may reconcile in between thus marking
     * only a single slot as free. This causes job 4 to sometimes use 0_3 because the first pod of job3 gets
     * terminated and now 2 slots are already available for the scheduling if job 4
     */
    @Test
    public void testLongQueue() {
        createSlot(SlotConfiguration.builder()
                                    .slotsPerNode(2)
                                    .mock(false)
                                    .nodeNames(List.of(TEST_NODE_NAMES[0], TEST_NODE_NAMES[1]))
                                    .build());

        var job1 = createJob("sample2.yaml");
        var job2 = createJob("sample3.yaml");
        var job3 = createJob("flink.yaml");
        var job4 = createJob("flink2.yaml");

        // Expected
        // 2, 1, 2, 1
        // _, 1, _, 1
        // 3, 3, 3, _
        // 4, 4, _, _
        createScheduling(TEST_SCHEDULING,
                List.of(job2, job1, job2, job1, job3, job3, job3, job4, job4)
        );

        List.of(job1, job2, job3, job4).forEach(this::assertJobEnqueueWasRequested);

        assertApplicationCreationWasRequested(job2, "0_2", 2);
        assertApplicationCreationWasRequested(job1, "1_3", 2);

        assertApplicationCreationWasNotRequested(job3);
        assertApplicationCreationWasNotRequested(job4);

        assertSchedulingState(TEST_SCHEDULING, SchedulingState.ConfirmationState);

        var sparkApplication2 = new SparkApplicationMock(job2, Set.of(0, 2))
                .initialize()
                .createsPods();

        assertSchedulingState(TEST_SCHEDULING, SchedulingState.ConfirmationState);

        sparkApplication2.becomesScheduled().startsRunning();

        assertSchedulingState(TEST_SCHEDULING, SchedulingState.ConfirmationState);

        var flinkApplication1 = new FlinkApplicationMock(job1, Set.of(1, 3))
                .initialize()
                .createsPods();

        assertSchedulingState(TEST_SCHEDULING, SchedulingState.ConfirmationState);

        flinkApplication1.becomesScheduled().startsRunning();

        assertSchedulingState(TEST_SCHEDULING, SchedulingState.SubmissionState);

        sparkApplication2.completes();

        assertSchedulingState(TEST_SCHEDULING, SchedulingState.SubmissionState);

        assertApplicationCreationWasNotRequested(job3);
        assertApplicationCreationWasNotRequested(job4);

        flinkApplication1.completes();

        assertApplicationCreationWasRequested(job3, "0_1_2", 3);
        assertApplicationCreationWasNotRequested(job4);

        var flinkApplication3 = new FlinkApplicationMock(job3, Set.of(0, 1, 2))
                .initialize();

        assertSchedulingState(TEST_SCHEDULING, SchedulingState.ConfirmationState);

        flinkApplication3
                .createsPods()
                .becomesScheduled();

        assertSchedulingState(TEST_SCHEDULING, SchedulingState.SubmissionState);


        flinkApplication3.startsRunning().completes();

        assertSchedulingState(TEST_SCHEDULING, SchedulingState.AwaitingCompletionState);

        assertApplicationCreationWasRequested(job4, "0_1", 2);

        var flinkApplication4 = new FlinkApplicationMock(job4, Set.of(0, 1))
                .initialize();

        assertSchedulingState(TEST_SCHEDULING, SchedulingState.AwaitingCompletionState);

        flinkApplication4.createsPods().becomesScheduled().startsRunning().completes();

        assertSchedulingState(TEST_SCHEDULING, SchedulingState.CompletedState);
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

        client.resources(SparkApplication.class).inNamespace(NAMESPACE).withName(job2).editStatus((spark) -> {
            spark.setStatus(
                    new V1beta2SparkApplicationStatus()
                            .executorState(Map.of(
                                    "exec1", "scheduled",
                                    "exec2", "scheduled"
                            )));
            return spark;
        });


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

    @RequiredArgsConstructor
    class FlinkApplicationMock {
        private final String name;
        private final Set<Integer> slotIds;
        private final String namespace;
        private final String podPrefix;

        public FlinkApplicationMock(String name, Set<Integer> slotIds) {
            this(name, slotIds, NAMESPACE, name + "-Flink-Executor-");
        }

        public List<Runnable> getLifecycle() {
            return List.of(
                    this::createsPods,
                    this::becomesScheduled,
                    this::startsRunning,
                    this::completes
            );
        }

        public FlinkApplicationMock createsPods() {
            createFlinkPods(name, PodInSlotsConfiguration.builder()
                                                         .prefix(podPrefix)
                                                         .cpu("900m")
                                                         .slotIds(slotIds)
                                                         .build());

            await().atMost(TIMEOUT_DURATION_IN_SECONDS, TimeUnit.SECONDS).untilAsserted(() -> {
                for (int i = 0; i < slotIds.size(); i++) {
                    var pod = client.pods().inNamespace(namespace).withName(podPrefix + i).get();
                    assertThat(pod).isNotNull();
                    assertThat(pod.getSpec().getNodeName()).isNotNull();
                }
            });
            return this;
        }

        public FlinkApplicationMock initialize() {
            await().atMost(TIMEOUT_DURATION_IN_SECONDS, TimeUnit.SECONDS).untilAsserted(() -> {


                var spark = client.resources(FlinkCluster.class).inNamespace(NAMESPACE).withName(name).get();
                assertThat(spark).isNotNull();
                assertThat(spark.getSpec().getTaskManager().getResources().getRequests().get("cpu"))
                        .isEqualTo(Quantity.parse("900m"));
                assertThat(spark.getSpec().getTaskManager().getPodLabels())
                        .is(keysWithValues(
                                SLOT_POD_IS_GHOSTPOD_NAME, "false",
                                SLOT_IDS_NAME, SlotIDsAnnotationString.ofIds(slotIds).toString()
                        ));
            });

            return this;
        }

        public FlinkApplicationMock startsRunning() {
            client.resources(FlinkCluster.class).inNamespace(namespace).withName(name).editStatus((flinkCluster) -> {
                flinkCluster.setStatus(
                        new V1beta1FlinkClusterStatus().state("Running"));
                return flinkCluster;
            });

            return this;
        }

        @SneakyThrows
        public void completes() {
            IntStream.range(0, slotIds.size())
                     .forEach(i -> client.pods().inNamespace(namespace).withName(podPrefix + i).editStatus(pod -> {
                         pod.getStatus().setPhase("Succeeded");
                         return pod;
                     }));

            Thread.sleep(300);

            client.resources(FlinkCluster.class).inNamespace(namespace).withName(name).editStatus((flinkCluster) -> {
                flinkCluster.setStatus(
                        new V1beta1FlinkClusterStatus().state("Stopped")
                                                       .components(new V1beta1FlinkClusterStatusComponents()
                                                               .taskManagerStatefulSet(new V1beta1FlinkClusterStatusComponentsConfigMap().state("Deleted"))
                                                               .jobManagerStatefulSet(new V1beta1FlinkClusterStatusComponentsConfigMap().state("Deleted")))
                );
                return flinkCluster;
            });


        }


        public FlinkApplicationMock becomesScheduled() {


            client.resources(FlinkCluster.class).inNamespace(namespace).withName(name).editStatus((flinkCluster) -> {
                flinkCluster.setStatus(
                        new V1beta1FlinkClusterStatus()
                                .state("Running")
                                .components(new V1beta1FlinkClusterStatusComponents().taskManagerStatefulSet(
                                                new V1beta1FlinkClusterStatusComponentsConfigMap()
                                                        .name("TaskManager")
                                                        .state("Scheduled")
                                        )
                                ));

                return flinkCluster;
            });

            return this;
        }


    }

    @RequiredArgsConstructor
    class SparkApplicationMock {
        private final String name;
        private final Set<Integer> slotIds;
        private final String namespace;
        private final String podPrefix;

        public SparkApplicationMock(String name, Set<Integer> slotIds) {
            this(name, slotIds, NAMESPACE, name + "-Spark-Executor-");
        }

        public List<Runnable> getLifecycle() {
            return List.of(
                    this::createsPods,
                    this::becomesScheduled,
                    this::startsRunning,
                    this::completes
            );
        }

        public SparkApplicationMock initialize() {
            await().atMost(TIMEOUT_DURATION_IN_SECONDS, TimeUnit.SECONDS).untilAsserted(() -> {

                var spark = client.resources(SparkApplication.class).inNamespace(NAMESPACE).withName(name).get();
                assertThat(spark).isNotNull();
                assertThat(spark.getSpec().getExecutor().getCoreRequest())
                        .isEqualTo("900m");
                assertThat(spark.getSpec().getExecutor().getLabels())
                        .is(keysWithValues(
                                SLOT_POD_IS_GHOSTPOD_NAME, "false",
                                SLOT_IDS_NAME, SlotIDsAnnotationString.ofIds(slotIds).toString()
                        ));
            });

            return this;
        }

        public SparkApplicationMock startsRunning() {
            client.resources(SparkApplication.class).inNamespace(namespace).withName(name).editStatus((spark) -> {
                spark.getStatus().getApplicationState().setState(SparkApplicationStatusState.RunningState);
                return spark;
            });
            return this;
        }

        @SneakyThrows
        public void completes() {
            client.resources(SparkApplication.class).inNamespace(namespace).withName(name).editStatus((spark) -> {
                spark.getStatus().getApplicationState().setState(SparkApplicationStatusState.CompletedState);
                return spark;
            });

            Thread.sleep(300);

            IntStream.range(0, slotIds.size())
                     .forEach(i -> client.pods().inNamespace(namespace).withName(podPrefix + i).delete());
        }


        public SparkApplicationMock createsPods() {
            createSparkPods(name, PodInSlotsConfiguration.builder()
                                                         .prefix(podPrefix)
                                                         .cpu("900m")
                                                         .slotIds(slotIds)
                                                         .build());

            await().atMost(TIMEOUT_DURATION_IN_SECONDS, TimeUnit.SECONDS).untilAsserted(() -> {
                for (int i = 0; i < slotIds.size(); i++) {
                    var pod = client.pods().inNamespace(NAMESPACE).withName(podPrefix + i).get();
                    assertThat(pod).isNotNull();
                    assertThat(pod.getSpec().getNodeName()).isNotNull();
                }
            });
            return this;
        }

        public SparkApplicationMock becomesScheduled() {
            client.resources(SparkApplication.class).inNamespace(namespace).withName(name).editStatus((spark) -> {
                spark.setStatus(
                        new V1beta2SparkApplicationStatus()
                                .applicationState(new V1beta2SparkApplicationStatusApplicationState()
                                        .state(SparkApplicationStatusState.SubmittedState))
                                .executorState(Map.of(
                                        "exec1", "scheduled",
                                        "exec2", "scheduled"
                                )));
                return spark;
            });

            return this;
        }


    }

}
