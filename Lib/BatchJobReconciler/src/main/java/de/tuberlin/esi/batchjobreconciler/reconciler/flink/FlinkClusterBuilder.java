package de.tuberlin.esi.batchjobreconciler.reconciler.flink;

import de.tuberlin.esi.batchjobreconciler.reconciler.ApplicationBuilder;
import de.tuberlin.esi.common.crd.batchjob.BatchJob;
import de.tuberlin.esi.common.crd.testbed.SlotOccupationStatus;
import de.tuberlin.esi.common.crd.testbed.Testbed;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.api.model.ResourceRequirementsBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import k8s.flinkoperator.FlinkCluster;
import k8s.flinkoperator.V1beta1FlinkClusterImageSpec;
import k8s.flinkoperator.V1beta1FlinkClusterSpec;
import k8s.flinkoperator.V1beta1FlinkClusterSpecBatchScheduler;
import k8s.flinkoperator.V1beta1FlinkClusterSpecJobManager;
import k8s.flinkoperator.V1beta1FlinkClusterSpecJobManagerPorts;
import k8s.flinkoperator.V1beta1FlinkClusterSpecTaskManager;

import java.util.List;
import java.util.Map;

import static de.tuberlin.esi.common.constants.CommonConstants.MANAGED_BY_LABEL_NAME;
import static de.tuberlin.esi.common.constants.CommonConstants.MANAGED_BY_LABEL_VALUE;

public class FlinkClusterBuilder extends ApplicationBuilder {
    public FlinkClusterBuilder(BatchJob job, Testbed slots) {
        super(job, slots);
    }

    private V1beta1FlinkClusterSpecJobManager createJobManagerSpec() {
        return new V1beta1FlinkClusterSpecJobManager()
                .resources(new ResourceRequirementsBuilder()
                        .withLimits(Map.of("memory", Quantity.parse("1024Mi"), "cpu", Quantity.parse("200m")))
                        .build())
                .ports(new V1beta1FlinkClusterSpecJobManagerPorts().ui(8081));
    }

    private V1beta1FlinkClusterSpecTaskManager createTaskManagerSpec(Testbed testbed,
                                                                     List<SlotOccupationStatus> freeSlots) {


        // PriorityClassName + SchedulerName are set by the Flink Operator using a custom Batch Scheduler.
        return new V1beta1FlinkClusterSpecTaskManager()
                .resources(new ResourceRequirementsBuilder()
                        .withRequests(testbed.getSpec().getResourcesPerSlot())
                        .withLimits(testbed.getSpec().getResourcesPerSlot())
                        .build())
                .podLabels(createLabels())
                .affinity(createAffinity())
                .memoryOffHeapMin(testbed.getSpec().getResourcesPerSlot().get("memory").toString())
                .replicas(freeSlots.size());
    }


    @Override
    public void create(KubernetesClient client) {
        var flink = new FlinkCluster();
        flink.getMetadata().setName(job.getMetadata().getName());
        flink.getMetadata().setNamespace(namespace);
        flink.addOwnerReference(job);

        flink.getMetadata().setLabels(Map.of(MANAGED_BY_LABEL_NAME, MANAGED_BY_LABEL_VALUE));

        var clusterSpec = new V1beta1FlinkClusterSpec()
                .batchScheduler(new V1beta1FlinkClusterSpecBatchScheduler()
                        .name("external"))
                .image(new V1beta1FlinkClusterImageSpec().name("flink:1.8.2"))
                .jobManager(createJobManagerSpec())
                .taskManager(createTaskManagerSpec(testbed, freeSlots))
                .job(job.getSpec().getFlinkSpec())
                .flinkProperties(Map.of("taskmanager.numberOfTaskSlots", "1"));

        flink.setSpec(clusterSpec);
        client.resources(FlinkCluster.class).inNamespace(namespace).create(flink);
    }
}
