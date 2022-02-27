package de.tuberlin.batchjoboperator.web.external;

import com.google.common.collect.ImmutableList;
import de.tuberlin.batchjoboperator.crd.batchjob.BatchJob;
import de.tuberlin.batchjoboperator.crd.slots.SlotOccupationStatus;
import io.fabric8.kubernetes.api.model.Affinity;
import io.fabric8.kubernetes.api.model.AffinityBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static de.tuberlin.batchjoboperator.reconciler.slots.SlotReconciler.SLOT_IDS_NAME;
import static de.tuberlin.batchjoboperator.reconciler.slots.SlotReconciler.SLOT_POD_IS_GHOSTPOD_NAME;
import static de.tuberlin.batchjoboperator.reconciler.slots.SlotReconciler.SLOT_POD_LABEL_NAME;

public abstract class ApplicationBuilder {

    protected final VerifiedReleaseFromQueueRequest request;
    protected final BatchJob job;
    protected final List<SlotOccupationStatus> freeSlots = new ArrayList<>();
    protected String namespace;


    protected ApplicationBuilder(VerifiedReleaseFromQueueRequest request, BatchJob job) {
        this.request = request;
        this.job = job;
    }

    static ApplicationBuilder forJob(BatchJob job, VerifiedReleaseFromQueueRequest request) {
        if (job.getSpec().getSparkSpec() != null) {
            return new SparkApplicationBuilder(request, job);
        }
        else if (job.getSpec().getFlinkSpec() != null) {
            return new FlinkClusterBuilder(request, job);
        }

        throw new RuntimeException("Invalid Job Spec. Neither Spark nor Flink");
    }

    public ApplicationBuilder inNamespace(String namespace) {
        this.namespace = namespace;
        return this;
    }

    public ApplicationBuilder inSlots(List<SlotOccupationStatus> slots) {
        this.freeSlots.addAll(slots);
        return this;
    }

    protected Map<String, String> createLabels() {
        return Map.of(
                SLOT_POD_LABEL_NAME, request.getSlot().getMetadata().getName(),
                SLOT_POD_IS_GHOSTPOD_NAME, "false",
                SLOT_IDS_NAME, SlotIDsAnnotationString.of(freeSlots).toString()
        );
    }

    protected Affinity createAffinity() {
        return new AffinityBuilder()
                .withNewNodeAffinity().withNewRequiredDuringSchedulingIgnoredDuringExecution()
                .addNewNodeSelectorTerm().addNewMatchExpression()
                .withKey(request.getSlot().getSpec().getNodeLabel())
                .withValues(ImmutableList.copyOf(freeSlots.stream()
                                                          .map(freeSlot -> request.getNodesSlotIds()
                                                                                  .get(freeSlot.getNodeName()))
                                                          .collect(Collectors.toSet())))
                .withOperator("In")
                .endMatchExpression().endNodeSelectorTerm().endRequiredDuringSchedulingIgnoredDuringExecution()
                .endNodeAffinity()
                .build();
    }

    public abstract void create(KubernetesClient client);
}
