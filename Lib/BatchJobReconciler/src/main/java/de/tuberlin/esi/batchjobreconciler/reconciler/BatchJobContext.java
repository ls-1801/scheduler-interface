package de.tuberlin.esi.batchjobreconciler.reconciler;

import de.tuberlin.esi.batchjobreconciler.reconciler.flink.FlinkApplicationProvider;
import de.tuberlin.esi.batchjobreconciler.reconciler.spark.SparkApplicationProvider;
import de.tuberlin.esi.common.crd.NamespacedName;
import de.tuberlin.esi.common.crd.batchjob.BatchJob;
import de.tuberlin.esi.common.crd.batchjob.CreationRequest;
import de.tuberlin.esi.common.crd.scheduling.Scheduling;
import de.tuberlin.esi.common.crd.testbed.Testbed;
import de.tuberlin.esi.common.crd.testbed.TestbedState;
import de.tuberlin.esi.common.statemachine.StateMachineContext;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.client.KubernetesClient;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class BatchJobContext implements StateMachineContext {

    @Getter
    private final BatchJob resource;
    @Getter
    private final KubernetesClient client;

    @Getter
    private final ApplicationSpecific application;
    private final Map<NamespacedName, Scheduling> schedulingCache = new HashMap<>();
    private final Map<NamespacedName, Testbed> slotsCache = new HashMap<>();


    public BatchJobContext(BatchJob resource, KubernetesClient client) {
        this.resource = resource;
        this.client = client;
        if (resource.isFlink()) {
            this.application = new FlinkApplicationProvider(client, NamespacedName.of(resource));
        }
        else if (resource.isSpark()) {
            this.application = new SparkApplicationProvider(client, NamespacedName.of(resource));
        }
        else {
            throw new RuntimeException("Neither Spark nor Flink");
        }

    }

    public void removeApplication() {
        getApplication().delete();
    }

    public void createApplication(CreationRequest request) {
        var slots = getSlots(request.getTestbedName());

        if (slots == null || slots.getStatus().getState() == TestbedState.ERROR)
            throw new RuntimeException("PROBLEM: This should have been checked by the condition");

        if (application.isExisting()) {
            log.warn("Application already exists, skipping creation");
            return;
        }

        ApplicationBuilder.forJob(resource, slots)
                          .inNamespace(resource.getMetadata().getNamespace())
                          .inSlots(request.getSlotIds())
                          .create(client);

    }

    @Nullable
    private <T extends CustomResource> T getCR(@Nonnull NamespacedName namespacedName, @Nonnull Class<T> clazz) {
        return client.resources(clazz).inNamespace(namespacedName.getNamespace())
                     .withName(namespacedName.getName())
                     .get();
    }

    @Nullable
    public Scheduling getScheduling(@Nonnull NamespacedName namespacedName) {
        return schedulingCache.computeIfAbsent(namespacedName, (nn) -> getCR(nn, Scheduling.class));
    }

    @Nullable
    public Testbed getSlots(@Nonnull NamespacedName namespacedName) {
        return slotsCache.computeIfAbsent(namespacedName, (nn) -> getCR(nn, Testbed.class));
    }

}
