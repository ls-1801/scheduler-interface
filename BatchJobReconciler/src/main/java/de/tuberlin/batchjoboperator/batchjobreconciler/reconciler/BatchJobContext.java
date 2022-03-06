package de.tuberlin.batchjoboperator.batchjobreconciler.reconciler;

import de.tuberlin.batchjoboperator.batchjobreconciler.reconciler.flink.FlinkApplicationProvider;
import de.tuberlin.batchjoboperator.batchjobreconciler.reconciler.spark.SparkApplicationProvider;
import de.tuberlin.batchjoboperator.common.NamespacedName;
import de.tuberlin.batchjoboperator.common.StateMachineContext;
import de.tuberlin.batchjoboperator.common.crd.batchjob.BatchJob;
import de.tuberlin.batchjoboperator.common.crd.scheduling.Scheduling;
import de.tuberlin.batchjoboperator.common.crd.slots.Slot;
import de.tuberlin.batchjoboperator.common.crd.slots.SlotsStatusState;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.client.KubernetesClient;
import k8s.flinkoperator.FlinkCluster;
import k8s.sparkoperator.SparkApplication;
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
    private final ApplicationProvider application;
    private final Map<NamespacedName, Scheduling> schedulingCache = new HashMap<>();
    private final Map<NamespacedName, Slot> slotsCache = new HashMap<>();


    public BatchJobContext(BatchJob resource, KubernetesClient client) {
        this.resource = resource;
        this.client = client;
        if (resource.isFlink()) {
            this.application = new FlinkApplicationProvider(client, NamespacedName.of(resource));
        }
        else if (resource.isSpark()) {
            this.application = new SparkApplicationProvider(client);
        }
        else {
            throw new RuntimeException("Neither Spark nor Flink");
        }

    }

    public void removeApplication(NamespacedName name) {
        if (resource.isSpark()) {
            client.resources(SparkApplication.class)
                  .inNamespace(resource.getMetadata().getNamespace())
                  .withName(resource.getMetadata().getName()).delete();
        }
        else if (resource.isFlink()) {
            client.resources(FlinkCluster.class)
                  .inNamespace(resource.getMetadata().getNamespace())
                  .withName(resource.getMetadata().getName()).delete();
        }
    }

    public void createApplication(CreationRequest request) {
        var slots = getSlots(request.getSlotsName());

        if (slots == null || slots.getStatus().getState() == SlotsStatusState.ERROR)
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
    public Slot getSlots(@Nonnull NamespacedName namespacedName) {
        return slotsCache.computeIfAbsent(namespacedName, (nn) -> getCR(nn, Slot.class));
    }

}
