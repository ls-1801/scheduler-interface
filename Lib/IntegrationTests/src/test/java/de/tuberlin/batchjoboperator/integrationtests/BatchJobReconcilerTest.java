package de.tuberlin.batchjoboperator.integrationtests;

import de.tuberlin.batchjoboperator.batchjobreconciler.reconciler.BatchJobReconciler;
import de.tuberlin.batchjoboperator.common.crd.NamespacedName;
import de.tuberlin.batchjoboperator.common.crd.batchjob.BatchJob;
import de.tuberlin.batchjoboperator.common.crd.batchjob.BatchJobState;
import de.tuberlin.batchjoboperator.common.crd.batchjob.CreationRequest;
import de.tuberlin.batchjoboperator.common.crd.slots.SlotIDsAnnotationString;
import io.fabric8.kubernetes.client.KubernetesClient;
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
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static de.tuberlin.batchjoboperator.batchjobreconciler.reconciler.conditions.BatchJobCondition.AWAIT_COMPLETION_CONDITION;
import static de.tuberlin.batchjoboperator.batchjobreconciler.reconciler.conditions.BatchJobCondition.AWAIT_CREATION_REQUEST_CONDITION;
import static de.tuberlin.batchjoboperator.batchjobreconciler.reconciler.conditions.BatchJobCondition.AWAIT_DELETION_CONDITION;
import static de.tuberlin.batchjoboperator.batchjobreconciler.reconciler.conditions.BatchJobCondition.AWAIT_ENQUEUE_REQUEST_CONDITION;
import static de.tuberlin.batchjoboperator.batchjobreconciler.reconciler.conditions.BatchJobCondition.AWAIT_POD_SCHEDULED_CONDITION;
import static de.tuberlin.batchjoboperator.batchjobreconciler.reconciler.conditions.BatchJobCondition.AWAIT_RELEASE_CONDITION;
import static de.tuberlin.batchjoboperator.batchjobreconciler.reconciler.conditions.BatchJobCondition.AWAIT_RUNNING_CONDITION;
import static de.tuberlin.batchjoboperator.common.constants.SlotsConstants.SLOT_IDS_NAME;
import static de.tuberlin.batchjoboperator.common.constants.SlotsConstants.SLOT_POD_IS_GHOSTPOD_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@Slf4j
@EnableKubernetesMockClient(https = false, crud = true)
class BatchJobReconcilerTest extends BaseReconcilerTest {

    private void requestEnqueue(String jobName) {
        client.resources(BatchJob.class).inNamespace(NAMESPACE).withName(jobName).edit((job) -> {
            if (job.getMetadata().getLabels() == null)
                job.getMetadata().setLabels(new HashMap<>());

            job.getSpec().setActiveScheduling(new NamespacedName(TEST_SCHEDULING, NAMESPACE));
            return job;
        });
    }

    private void releaseJob(String jobName) {
        client.resources(BatchJob.class).inNamespace(NAMESPACE).withName(jobName).edit((job) -> {
            job.getSpec().setActiveScheduling(null);
            job.getSpec().setCreationRequest(null);
            return job;
        });
    }


    private void requestCreation(String jobName, String slotIdString, int replication) {
        var ids = SlotIDsAnnotationString.parse(slotIdString).getSlotIds();
        client.resources(BatchJob.class).inNamespace(NAMESPACE).withName(jobName).edit((job) -> {
            job.getSpec().setCreationRequest(
                    new CreationRequest(ids, new NamespacedName(TEST_SLOT_NAME_1, NAMESPACE), ids.size()));
            return job;
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

        client.resources(SparkApplication.class).inNamespace(NAMESPACE).withName(jobName).editStatus((spark) -> {
            spark.setStatus(
                    new V1beta2SparkApplicationStatus()
                            .applicationState(new V1beta2SparkApplicationStatusApplicationState()
                                    .state(SparkApplicationStatusState.RunningState))
            );
            return spark;
        });

        // Spark Executors are not yet created so the batchjob should not be scheduled
        assertJobStatus(jobName, BatchJobState.SubmittedState);

        mockSparkPods(jobName, 4);
        client.resources(SparkApplication.class).inNamespace(NAMESPACE).withName(jobName).editStatus((spark) -> {
            spark.setStatus(
                    new V1beta2SparkApplicationStatus()
                            .applicationState(new V1beta2SparkApplicationStatusApplicationState()
                                    .state(SparkApplicationStatusState.RunningState))
                            .executorState(Map.of(
                                    "exec1", "Pending",
                                    "exec2", "Pending",
                                    "exec3", "Pending",
                                    "exec4", "Pending"
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
            spark.setStatus(
                    new V1beta2SparkApplicationStatus()
                            .applicationState(new V1beta2SparkApplicationStatusApplicationState()
                                    .state(SparkApplicationStatusState.RunningState))
                            .executorState(Map.of(
                                    "exec1", "RUNNING",
                                    "exec2", "RUNNING",
                                    "exec3", "RUNNING",
                                    "exec4", "RUNNING"
                            )));
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
    protected List<Reconciler> createReconcilers(Supplier<KubernetesClient> clientSupplier) {
        return Collections.singletonList(new BatchJobReconciler(clientSupplier.get(), NAMESPACE));
    }

    @Override
    protected void registerCRDs() {
        createCRDFromResource("batchjobs.batchjob.gcr.io-v1.yml");
        createCRDFromResource("flinkclusters.flinkoperator.k8s.io-v1.yml");
        createCRDFromResource("sparkapplications.sparkoperator.k8s.io-v1.yml");
    }


}
