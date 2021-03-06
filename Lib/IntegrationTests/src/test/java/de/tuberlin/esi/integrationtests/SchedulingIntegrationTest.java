package de.tuberlin.esi.integrationtests;

import de.tuberlin.esi.common.crd.batchjob.BatchJobState;
import de.tuberlin.esi.common.crd.scheduling.Scheduling;
import de.tuberlin.esi.common.crd.scheduling.SchedulingState;
import de.tuberlin.esi.common.crd.testbed.TestbedState;
import de.tuberlin.esi.schedulingreconciler.SchedulingReconciler;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.javaoperatorsdk.operator.api.reconciler.Reconciler;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

public class SchedulingIntegrationTest extends BaseReconcilerTest {

    @Override
    protected List<Reconciler> createReconcilers(Supplier<KubernetesClient> clientSupplier) {
        return List.of(new SchedulingReconciler(clientSupplier.get(), NAMESPACE, true));
    }

    @Override
    protected void registerCRDs() {
        createCRDFromResource("batchjobs.esi.tu-berlin.de-v1.yml");
        createCRDFromResource("slots.esi.tu-berlin.de-v1.yml");
        createCRDFromResource("schedulings.esi.tu-berlin.de-v1.yml");
        createCRDFromResource("flinkclusters.flinkoperator.k8s.io-v1.yml");
    }


    @Test
    public void testMultipleJobsSubmittedAtOnce() {
        createSlot(SlotConfiguration.builder()
                                    .slotsPerNode(2)
                                    .mock(true)
                                    .nodeNames(List.of(TEST_NODE_NAMES[0], TEST_NODE_NAMES[1]))
                                    .build());

        var job1 = createJob("sample2.yaml");
        var job2 = createJob("sample3.yaml");

        assertJobEnqueueWasNotRequested(job1);
        assertJobEnqueueWasNotRequested(job2);

        createScheduling(TEST_SCHEDULING,
                List.of(job2, job1, job2, job1)
        );

        assertJobEnqueueWasRequested(job1);
        assertJobEnqueueWasRequested(job2);

        assertApplicationCreationWasNotRequested(job1);
        assertApplicationCreationWasNotRequested(job2);

        changeBatchJobState(job1, BatchJobState.InQueueState);

        assertApplicationCreationWasNotRequested(job1);
        assertApplicationCreationWasNotRequested(job2);

        changeBatchJobState(job2, BatchJobState.InQueueState);

        assertApplicationCreationWasRequested(job1, "1_3", 2);
        assertApplicationCreationWasRequested(job2, "0_2", 2);

        assertSchedulingState(TEST_SCHEDULING, SchedulingState.AwaitingCompletionState);


        changeBatchJobState(job1, BatchJobState.SubmittedState);
        changeBatchJobState(job1, BatchJobState.ScheduledState);
        updateSlots(Set.of(), Set.of(1, 3), TestbedState.RUNNING);
        changeBatchJobState(job2, BatchJobState.SubmittedState);
        updateSlots(Set.of(1, 3), Set.of(0, 2), TestbedState.RUNNING);
        changeBatchJobState(job2, BatchJobState.ScheduledState);
        updateSlots(Set.of(0, 1, 2, 3), Set.of(), TestbedState.RUNNING);

        changeBatchJobState(job1, BatchJobState.CompletedState);
        updateSlots(Set.of(0, 2), Set.of(), TestbedState.RUNNING);

        changeBatchJobState(job2, BatchJobState.CompletedState);
        updateSlots(Set.of(), Set.of(), TestbedState.SUCCESS);

        assertSchedulingState(TEST_SCHEDULING, SchedulingState.CompletedState);

// TODO: Finalizers do not run in a Test
//        deleteScheduling(TEST_SCHEDULING);
//
//        assertJobEnqueueWasNotRequested(job1);
//        assertJobEnqueueWasNotRequested(job2);
//        assertApplicationCreationWasNotRequested(job1);
//        assertApplicationCreationWasNotRequested(job2);

    }

    private void deleteScheduling(String testScheduling) {
        client.resources(Scheduling.class).inNamespace(NAMESPACE).withName(testScheduling).delete();
    }


    @Test
    public void testMultipleJobsQueue() {
        createSlot(SlotConfiguration.builder()
                                    .slotsPerNode(2)
                                    .mock(true)
                                    .nodeNames(List.of(TEST_NODE_NAMES[0], TEST_NODE_NAMES[1]))
                                    .build());

        var job1 = createJob("sample2.yaml");
        var job2 = createJob("sample3.yaml");

        assertJobEnqueueWasNotRequested(job1);
        assertJobEnqueueWasNotRequested(job2);

        createScheduling(TEST_SCHEDULING,
                List.of(job1, job1, job1, job2, job2, job2)
        );

        assertJobEnqueueWasRequested(job1);
        assertJobEnqueueWasRequested(job2);

        assertApplicationCreationWasNotRequested(job1);
        assertApplicationCreationWasNotRequested(job2);

        changeBatchJobState(job1, BatchJobState.InQueueState);

        assertApplicationCreationWasNotRequested(job1);
        assertApplicationCreationWasNotRequested(job2);

        changeBatchJobState(job2, BatchJobState.InQueueState);


        // Only Job1 can be submitted
        assertApplicationCreationWasRequested(job1, "0_1_2", 3);
        assertApplicationCreationWasNotRequested(job2);

        // Wait for confirmation that the scheduling was succesful
        assertSchedulingState(TEST_SCHEDULING, SchedulingState.ConfirmationState);


        changeBatchJobState(job1, BatchJobState.SubmittedState);
        updateSlots(Set.of(), Set.of(0, 1, 2), TestbedState.RUNNING);
        updateSlots(Set.of(0, 1, 2), Set.of(), TestbedState.RUNNING);
        changeBatchJobState(job1, BatchJobState.ScheduledState);

        // Wait for slots to become available again
        assertSchedulingState(TEST_SCHEDULING, SchedulingState.SubmissionState);

        changeBatchJobState(job1, BatchJobState.RunningState);
        changeBatchJobState(job1, BatchJobState.CompletedState);

        // Completion of previous job is not enough! Wait for slots to become available again
        assertSchedulingState(TEST_SCHEDULING, SchedulingState.SubmissionState);
        assertApplicationCreationWasNotRequested(job2);

        updateSlots(Set.of(), Set.of(), TestbedState.SUCCESS);

        // Job 2 is created
        assertApplicationCreationWasRequested(job2, "0_1_2", 3);

        // All Jobs were created
        assertSchedulingState(TEST_SCHEDULING, SchedulingState.AwaitingCompletionState);

        changeBatchJobState(job2, BatchJobState.SubmittedState);
        updateSlots(Set.of(), Set.of(0, 1, 2), TestbedState.RUNNING);
        updateSlots(Set.of(0, 1, 2), Set.of(), TestbedState.RUNNING);
        changeBatchJobState(job2, BatchJobState.ScheduledState);
        changeBatchJobState(job2, BatchJobState.RunningState);
        changeBatchJobState(job2, BatchJobState.CompletedState);

        updateSlots(Set.of(), Set.of(), TestbedState.SUCCESS);

        // All Jobs were completed
        assertSchedulingState(TEST_SCHEDULING, SchedulingState.CompletedState);
    }
}
