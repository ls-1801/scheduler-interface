package de.tuberlin.esi.batchjobreconciler.reconciler.spark;

import de.tuberlin.esi.batchjobreconciler.reconciler.ApplicationBuilder;
import de.tuberlin.esi.common.crd.batchjob.BatchJob;
import de.tuberlin.esi.common.crd.slots.Slot;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.client.KubernetesClient;
import k8s.sparkoperator.SparkApplication;
import k8s.sparkoperator.V1beta2SparkApplicationSpecBatchSchedulerOptions;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

import static de.tuberlin.esi.common.constants.CommonConstants.MANAGED_BY_LABEL_NAME;
import static de.tuberlin.esi.common.constants.CommonConstants.MANAGED_BY_LABEL_VALUE;

@Slf4j
public class SparkApplicationBuilder extends ApplicationBuilder {

    public SparkApplicationBuilder(BatchJob job, Slot slots) {
        super(job, slots);
    }

    // The MemoryOverhead of the Executors is overwritten to 0m, however it seems that the spark spec uses
    // different unit descriptions. Spark Operator seems to only accept integers. The amount is always converted to
    // mibis.
    public String quantityToSparkMemory(Quantity memoryQuantity) {
        var bytes = Quantity.getAmountInBytes(memoryQuantity);
        var mibi = bytes.divide(BigDecimal.valueOf(2).pow(20));
        var amountInMibi = mibi.setScale(0, RoundingMode.UP).intValue();
        return amountInMibi + "m";
    }

    public void create(KubernetesClient client) {
        var sparkApp = new SparkApplication();
        sparkApp.setSpec(job.getSpec().getSparkSpec());
        sparkApp.getMetadata().setName(job.getMetadata().getName());
        sparkApp.addOwnerReference(job);

        sparkApp.getMetadata().setLabels(Map.of(MANAGED_BY_LABEL_NAME, MANAGED_BY_LABEL_VALUE));

        var options = new V1beta2SparkApplicationSpecBatchSchedulerOptions();
        options.setPriorityClassName("high-priority");
        sparkApp.getSpec().setBatchSchedulerOptions(options);


        sparkApp.getSpec().getExecutor()
                .cores(1)//TODO: Check if the cpu counts need to be adjusted for slot sizes greater than 1000m CPU
                .coreRequest(slots.getSpec().getResourcesPerSlot().get("cpu").toString())
                .labels(createLabels())
                .affinity(createAffinity())
                .memoryOverhead("0m")
                .memory(quantityToSparkMemory(slots.getSpec().getResourcesPerSlot().get("memory")))
                .schedulerName("external-scheduler")
                .instances(freeSlots.size())
                .cores(1);

        client.resources(SparkApplication.class).inNamespace(this.namespace).create(sparkApp);
    }
}