package de.tuberlin.batchjoboperator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import de.tuberlin.batchjoboperator.batchjobreconciler.reconciler.BatchJobReconciler;
import de.tuberlin.batchjoboperator.common.NamespacedName;
import de.tuberlin.batchjoboperator.common.crd.batchjob.BatchJob;
import de.tuberlin.batchjoboperator.common.crd.batchjob.BatchJobState;
import io.fabric8.kubernetes.client.server.mock.EnableKubernetesMockClient;
import io.javaoperatorsdk.operator.api.reconciler.Reconciler;
import k8s.flinkoperator.FlinkCluster;
import k8s.flinkoperator.V1beta1FlinkClusterStatus;
import k8s.flinkoperator.V1beta1FlinkClusterStatusComponents;
import k8s.flinkoperator.V1beta1FlinkClusterStatusComponentsConfigMap;
import k8s.sparkoperator.SparkApplication;
import k8s.sparkoperator.SparkApplicationStatusState;
import k8s.sparkoperator.V1beta2SparkApplicationStatus;
import k8s.sparkoperator.V1beta2SparkApplicationStatusApplicationState;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.Test;

import javax.annotation.Nonnull;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static de.tuberlin.batchjoboperator.batchjobreconciler.reconciler.conditions.BatchJobCondition.AWAIT_COMPLETION_CONDITION;
import static de.tuberlin.batchjoboperator.batchjobreconciler.reconciler.conditions.BatchJobCondition.AWAIT_CREATION_REQUEST_CONDITION;
import static de.tuberlin.batchjoboperator.batchjobreconciler.reconciler.conditions.BatchJobCondition.AWAIT_DELETION_CONDITION;
import static de.tuberlin.batchjoboperator.batchjobreconciler.reconciler.conditions.BatchJobCondition.AWAIT_ENQUEUE_REQUEST_CONDITION;
import static de.tuberlin.batchjoboperator.batchjobreconciler.reconciler.conditions.BatchJobCondition.AWAIT_POD_SCHEDULED_CONDITION;
import static de.tuberlin.batchjoboperator.batchjobreconciler.reconciler.conditions.BatchJobCondition.AWAIT_RELEASE_CONDITION;
import static de.tuberlin.batchjoboperator.batchjobreconciler.reconciler.conditions.BatchJobCondition.AWAIT_RUNNING_CONDITION;
import static de.tuberlin.batchjoboperator.common.constants.SchedulingConstants.ACTIVE_SCHEDULING_LABEL_NAME;
import static de.tuberlin.batchjoboperator.common.constants.SchedulingConstants.ACTIVE_SCHEDULING_LABEL_NAMESPACE;
import static de.tuberlin.batchjoboperator.common.constants.SchedulingConstants.APPLICATION_CREATION_REQUEST_REPLICATION;
import static de.tuberlin.batchjoboperator.common.constants.SchedulingConstants.APPLICATION_CREATION_REQUEST_SLOTS_NAME;
import static de.tuberlin.batchjoboperator.common.constants.SchedulingConstants.APPLICATION_CREATION_REQUEST_SLOTS_NAMESPACE;
import static de.tuberlin.batchjoboperator.common.constants.SchedulingConstants.APPLICATION_CREATION_REQUEST_SLOT_IDS;
import static de.tuberlin.batchjoboperator.common.constants.SlotsConstants.SLOT_IDS_NAME;
import static de.tuberlin.batchjoboperator.common.constants.SlotsConstants.SLOT_POD_IS_GHOSTPOD_NAME;
import static de.tuberlin.batchjoboperator.common.constants.SlotsConstants.SLOT_POD_LABEL_NAME;
import static de.tuberlin.batchjoboperator.common.constants.SlotsConstants.SLOT_POD_SLOT_ID_NAME;
import static de.tuberlin.batchjoboperator.common.constants.SlotsConstants.SLOT_POD_TARGET_NODE_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@Slf4j
@EnableKubernetesMockClient(https = false, crud = true)
class BatchJobReconcilerTest extends BaseReconcilerTest {


    private List<FlinkCluster> getFlinkApplications() {
        var flinks = client.resources(FlinkCluster.class).inNamespace(NAMESPACE).list();
        return flinks.getItems();
    }

    private List<SparkApplication> getSparkApplications() {
        var sparks = client.resources(SparkApplication.class).inNamespace(NAMESPACE).list();
        return sparks.getItems();
    }


    @SneakyThrows
    private void createJob(String filename) {
        var job = new ObjectMapper(new YAMLFactory())
                .readValue(new FileInputStream("src/main/resources/jobs/" + filename), BatchJob.class);

        job.getMetadata().setNamespace(NAMESPACE);

        job = client.resources(BatchJob.class).inNamespace(NAMESPACE).create(job);
        assertThat(job).isNotNull();
    }


    private void requestEnqueue(String jobName) {
        client.resources(BatchJob.class).inNamespace(NAMESPACE).withName(jobName).edit((job) -> {
            if (job.getMetadata().getLabels() == null)
                job.getMetadata().setLabels(new HashMap<>());


            job.getMetadata().getLabels().put(ACTIVE_SCHEDULING_LABEL_NAME, TEST_SCHEDULING);
            job.getMetadata().getLabels().put(ACTIVE_SCHEDULING_LABEL_NAMESPACE, NAMESPACE);
            return job;
        });
    }

    private void releaseJob(String jobName) {
        client.resources(BatchJob.class).inNamespace(NAMESPACE).withName(jobName).edit((job) -> {
            if (job.getMetadata().getLabels() == null)
                job.getMetadata().setLabels(new HashMap<>());

            job.getMetadata().getLabels().remove(ACTIVE_SCHEDULING_LABEL_NAME);
            job.getMetadata().getLabels().remove(ACTIVE_SCHEDULING_LABEL_NAMESPACE);
            job.getMetadata().getLabels().remove(APPLICATION_CREATION_REQUEST_SLOT_IDS);
            job.getMetadata().getLabels().remove(APPLICATION_CREATION_REQUEST_SLOTS_NAMESPACE);
            job.getMetadata().getLabels().remove(APPLICATION_CREATION_REQUEST_SLOTS_NAME);
            job.getMetadata().getLabels().remove(APPLICATION_CREATION_REQUEST_REPLICATION);
            return job;
        });
    }


    private void requestCreation(String jobName, String slotIdString, int replication) {
        client.resources(BatchJob.class).inNamespace(NAMESPACE).withName(jobName).edit((job) -> {
            if (job.getMetadata().getLabels() == null)
                job.getMetadata().setLabels(new HashMap<>());

            job.getMetadata().getLabels().put(APPLICATION_CREATION_REQUEST_SLOT_IDS, slotIdString);
            job.getMetadata().getLabels().put(APPLICATION_CREATION_REQUEST_SLOTS_NAMESPACE, NAMESPACE);
            job.getMetadata().getLabels().put(APPLICATION_CREATION_REQUEST_SLOTS_NAME, TEST_SLOT_NAME_1);
            job.getMetadata().getLabels().put(APPLICATION_CREATION_REQUEST_REPLICATION, replication + "");
            return job;
        });
    }


    private void assertJobConditions(String jobName, Set<String> falseCondition, Set<String> trueConditions) {
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


    @Test
    void testNormalFlinkExecution() {
        createJob("flink2.yaml");
        final var jobName = "batchjob-flink2";

        assertJobStatus(jobName, BatchJobState.ReadyState);
        assertJobConditions(jobName, Set.of(AWAIT_ENQUEUE_REQUEST_CONDITION), Set.of());

        createSlot();
        createScheduling(List.of(jobName, jobName, jobName));
        claimSlot(defaultScheduling());

        requestEnqueue(jobName);

        assertJobStatus(jobName, BatchJobState.InQueueState);
        assertJobConditions(jobName, Set.of(AWAIT_RELEASE_CONDITION, AWAIT_CREATION_REQUEST_CONDITION), Set.of());


        requestCreation(jobName, "0_1_2_3", 4);

        // Submitted State
        assertJobStatus(jobName, BatchJobState.SubmittedState);
        await().atMost(TIMEOUT_DURATION_IN_SECONDS, TimeUnit.SECONDS).untilAsserted(() -> {
            var flink = getFlinkApplications();
            assertThat(flink).hasSize(1);
            assertThat(flink.get(0).getSpec().getTaskManager().getPodLabels())
                    .is(keysWithValues(
                            SLOT_POD_IS_GHOSTPOD_NAME, "false",
                            SLOT_IDS_NAME, "0_1_2_3"
                    ));
        });
        assertJobConditions(jobName, Set.of(
                AWAIT_RELEASE_CONDITION,
                AWAIT_DELETION_CONDITION,
                AWAIT_COMPLETION_CONDITION,
                AWAIT_POD_SCHEDULED_CONDITION
        ), Set.of());


        mockFlinkPods(jobName, 4);
        client.resources(FlinkCluster.class).inNamespace(NAMESPACE).withName(jobName).editStatus((flink) -> {
            flink.setStatus(new V1beta1FlinkClusterStatus()
                    .components(new V1beta1FlinkClusterStatusComponents()
                            .taskManagerStatefulSet(new V1beta1FlinkClusterStatusComponentsConfigMap()
                                    .name("pod").state("ready")))
            );
            return flink;
        });

        // Scheduled State
        assertJobStatus(jobName, BatchJobState.ScheduledState);
        assertJobConditions(jobName, Set.of(
                AWAIT_RELEASE_CONDITION,
                AWAIT_DELETION_CONDITION,
                AWAIT_COMPLETION_CONDITION,
                AWAIT_RUNNING_CONDITION
        ), Set.of());


        client.resources(FlinkCluster.class).inNamespace(NAMESPACE).withName(jobName).editStatus((flink) -> {
            flink.setStatus(new V1beta1FlinkClusterStatus().state("Running"));
            return flink;
        });

        //Running State
        assertJobStatus(jobName, BatchJobState.RunningState);
        assertJobConditions(jobName, Set.of(
                AWAIT_RELEASE_CONDITION,
                AWAIT_DELETION_CONDITION,
                AWAIT_COMPLETION_CONDITION
        ), Set.of());


        client.resources(FlinkCluster.class).inNamespace(NAMESPACE).withName(jobName).editStatus((flink) -> {
            flink.setStatus(new V1beta1FlinkClusterStatus().state("Stopped"));
            return flink;
        });


        // Completed State
        assertJobStatus(jobName, BatchJobState.CompletedState);
        assertJobConditions(jobName, Set.of(
                AWAIT_RELEASE_CONDITION,
                AWAIT_DELETION_CONDITION
        ), Set.of());


        releaseJob(jobName);

        assertJobStatus(jobName, BatchJobState.ReadyState);
        assertJobConditions(jobName, Set.of(
                AWAIT_ENQUEUE_REQUEST_CONDITION
        ), Set.of());

        await().atMost(TIMEOUT_DURATION_IN_SECONDS, TimeUnit.SECONDS).untilAsserted(() -> {
            var flink = getFlinkApplications();
            assertThat(flink).isEmpty();
        });


    }

    @Test
    void testNormalSparkExecution() {
        createJob("spark.yaml");
        final var jobName = "batchjob-spark";

        assertJobStatus(jobName, BatchJobState.ReadyState);
        assertJobConditions(jobName, Set.of(AWAIT_ENQUEUE_REQUEST_CONDITION), Set.of());

        createSlot();
        createScheduling(List.of(jobName, jobName, jobName));
        claimSlot(defaultScheduling());

        requestEnqueue(jobName);

        assertJobStatus(jobName, BatchJobState.InQueueState);
        assertJobConditions(jobName, Set.of(AWAIT_RELEASE_CONDITION, AWAIT_CREATION_REQUEST_CONDITION), Set.of());

        requestCreation(jobName, "0_1_2_3", 4);

        assertJobStatus(jobName, BatchJobState.SubmittedState);
        await().atMost(TIMEOUT_DURATION_IN_SECONDS, TimeUnit.SECONDS).untilAsserted(() -> {
            var spark = getSparkApplications();
            assertThat(spark).hasSize(1);
            assertThat(spark.get(0).getSpec().getExecutor().getLabels())
                    .is(keysWithValues(
                            SLOT_POD_IS_GHOSTPOD_NAME, "false",
                            SLOT_IDS_NAME, "0_1_2_3"
                    ));
        });
        assertJobConditions(jobName, Set.of(
                AWAIT_RELEASE_CONDITION,
                AWAIT_DELETION_CONDITION,
                AWAIT_COMPLETION_CONDITION,
                AWAIT_POD_SCHEDULED_CONDITION
        ), Set.of());

        mockSparkPods(jobName, 4);
        client.resources(SparkApplication.class).inNamespace(NAMESPACE).withName(jobName).editStatus((spark) -> {
            spark.setStatus(
                    new V1beta2SparkApplicationStatus()
                            .executorState(Map.of(
                                    "exec1", "scheduled",
                                    "exec2", "scheduled",
                                    "exec3", "scheduled",
                                    "exec4", "scheduled"
                            )));
            return spark;
        });


        assertJobStatus(jobName, BatchJobState.ScheduledState);
        assertJobConditions(jobName, Set.of(
                AWAIT_RELEASE_CONDITION,
                AWAIT_DELETION_CONDITION,
                AWAIT_COMPLETION_CONDITION,
                AWAIT_RUNNING_CONDITION
        ), Set.of());

        client.resources(SparkApplication.class).inNamespace(NAMESPACE).withName(jobName).editStatus((spark) -> {
            spark.setStatus(new V1beta2SparkApplicationStatus()
                    .applicationState(new V1beta2SparkApplicationStatusApplicationState()
                            .state(SparkApplicationStatusState.RunningState)));
            return spark;
        });


        assertJobStatus(jobName, BatchJobState.RunningState);
        assertJobConditions(jobName, Set.of(
                AWAIT_RELEASE_CONDITION,
                AWAIT_DELETION_CONDITION,
                AWAIT_COMPLETION_CONDITION
        ), Set.of());

        client.resources(SparkApplication.class).inNamespace(NAMESPACE).withName(jobName).editStatus((spark) -> {
            spark.setStatus(new V1beta2SparkApplicationStatus()
                    .applicationState(new V1beta2SparkApplicationStatusApplicationState()
                            .state(SparkApplicationStatusState.CompletedState)));
            return spark;
        });

        assertJobStatus(jobName, BatchJobState.CompletedState);
        assertJobConditions(jobName, Set.of(
                AWAIT_RELEASE_CONDITION,
                AWAIT_DELETION_CONDITION
        ), Set.of());

        releaseJob(jobName);

        assertJobStatus(jobName, BatchJobState.ReadyState);
        assertJobConditions(jobName, Set.of(
                AWAIT_ENQUEUE_REQUEST_CONDITION
        ), Set.of());

        await().atMost(TIMEOUT_DURATION_IN_SECONDS, TimeUnit.SECONDS).untilAsserted(() -> {
            var spark = getSparkApplications();
            assertThat(spark).isEmpty();
        });

    }

    @Nonnull
    @Override
    protected List<Reconciler> createReconcilers() {
        return Collections.singletonList(new BatchJobReconciler(client));
    }

    @Override
    protected void registerCRDs() {
        createCRDFromResource("batchjobs.batchjob.gcr.io-v1.yml");
        createCRDFromResource("flinkclusters.flinkoperator.k8s.io-v1.yml");
        createCRDFromResource("sparkapplications.sparkoperator.k8s.io-v1.yml");
    }

    private void mockFlinkPods(String jobName, int replication) {
        for (int i = 0; i < replication; i++) {
            createPod(new NamespacedName("flink-TM-pod" + i, NAMESPACE), Map.of(
                    "cluster", jobName, "component", "taskmanager", // Flink
                    //Slot
                    SLOT_POD_SLOT_ID_NAME, "" + i,
                    SLOT_POD_LABEL_NAME, TEST_SLOT_NAME_1,
                    SLOT_POD_IS_GHOSTPOD_NAME, "false",
                    SLOT_POD_TARGET_NODE_NAME, TEST_NODE_NAMES[i % TEST_NODE_NAMES.length]
            ), TEST_NODE_NAMES[i % TEST_NODE_NAMES.length]);
        }
    }

    private void mockSparkPods(String jobName, int replication) {
        for (int i = 0; i < replication; i++) {
            createPod(new NamespacedName("spark-executor-pod" + i, NAMESPACE), Map.of(
                    "sparkoperator.k8s.io/app-name", jobName, "spark-role", "executor", // Spark
                    //Slot
                    SLOT_POD_SLOT_ID_NAME, "" + i,
                    SLOT_POD_LABEL_NAME, TEST_SLOT_NAME_1,
                    SLOT_POD_IS_GHOSTPOD_NAME, "false",
                    SLOT_POD_TARGET_NODE_NAME, TEST_NODE_NAMES[i % TEST_NODE_NAMES.length]
            ), TEST_NODE_NAMES[i % TEST_NODE_NAMES.length]);
        }
    }
}
