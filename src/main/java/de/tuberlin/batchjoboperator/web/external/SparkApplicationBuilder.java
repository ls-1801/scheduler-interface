package de.tuberlin.batchjoboperator.web.external;

import de.tuberlin.batchjoboperator.crd.batchjob.BatchJob;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.k8s.sparkoperator.SparkApplication;
import io.k8s.sparkoperator.V1beta2SparkApplicationSpecBatchSchedulerOptions;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.math.NumberUtils;

import java.util.Map;

import static de.tuberlin.batchjoboperator.config.Constants.MANAGED_BY_LABEL_NAME;
import static de.tuberlin.batchjoboperator.config.Constants.MANAGED_BY_LABEL_VALUE;

@Slf4j
public class SparkApplicationBuilder extends ApplicationBuilder {

    protected SparkApplicationBuilder(VerifiedReleaseFromQueueRequest request, BatchJob job) {
        super(request, job);
    }

    public String quantityToSparkMemory(Quantity memoryQuantity) {
        long amount = (long) (NumberUtils.toLong(memoryQuantity.getAmount(), 0) / 1.75);

        switch (memoryQuantity.getFormat()) {
            case "Mi":
                return amount + "m";
            case "Gi":
                return amount + "g";
            default:
                throw new RuntimeException("Spark Memory conversion not implemented for " + memoryQuantity);
        }
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
                .coreRequest(request.getSlot().getSpec().getResourcesPerSlot().get("cpu").toString())
                .labels(createLabels())
                .affinity(createAffinity())
                .memory(quantityToSparkMemory(request.getSlot().getSpec().getResourcesPerSlot().get("memory")))
                .schedulerName("my-scheduler")
                .instances(freeSlots.size())
                .cores(1);

        client.resources(SparkApplication.class).inNamespace(this.namespace).create(sparkApp);
    }
}
