package de.tuberlin.batchjoboperator.batchjobreconciler.reconciler;

import com.google.common.collect.ImmutableList;
import de.tuberlin.batchjoboperator.batchjobreconciler.reconciler.flink.FlinkClusterBuilder;
import de.tuberlin.batchjoboperator.batchjobreconciler.reconciler.spark.SparkApplicationBuilder;
import de.tuberlin.batchjoboperator.common.crd.batchjob.BatchJob;
import de.tuberlin.batchjoboperator.common.crd.slots.Slot;
import de.tuberlin.batchjoboperator.common.crd.slots.SlotIDsAnnotationString;
import de.tuberlin.batchjoboperator.common.crd.slots.SlotOccupationStatus;
import de.tuberlin.batchjoboperator.common.crd.slots.SlotState;
import io.fabric8.kubernetes.api.model.Affinity;
import io.fabric8.kubernetes.api.model.AffinityBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static de.tuberlin.batchjoboperator.common.constants.SlotsConstants.SLOT_IDS_NAME;
import static de.tuberlin.batchjoboperator.common.constants.SlotsConstants.SLOT_POD_IS_GHOSTPOD_NAME;
import static de.tuberlin.batchjoboperator.common.constants.SlotsConstants.SLOT_POD_LABEL_NAME;

public abstract class ApplicationBuilder {

    protected final BatchJob job;
    protected final Slot slots;
    protected final List<SlotOccupationStatus> freeSlots;
    protected String namespace;


    protected ApplicationBuilder(BatchJob job, Slot slots) {
        this.slots = slots;
        this.freeSlots = slots.getStatus().getSlots().stream()
                              .filter(occ -> occ.getState() == SlotState.FREE).collect(Collectors.toList());
        this.job = job;
    }

    public static ApplicationBuilder forJob(BatchJob job, Slot slots) {
        if (job.isSpark()) {
            return new SparkApplicationBuilder(job, slots);
        }
        else if (job.isFlink()) {
            return new FlinkClusterBuilder(job, slots);
        }

        throw new RuntimeException("Invalid Job Spec. Neither Spark nor Flink");
    }

    public ApplicationBuilder inNamespace(String namespace) {
        this.namespace = namespace;
        return this;
    }

    public ApplicationBuilder inSlots(Set<Integer> slots) {
        this.freeSlots.removeIf(occ -> !slots.contains(occ.getPosition()));
        return this;
    }

    protected Map<String, String> createLabels() {
        return Map.of(
                SLOT_POD_LABEL_NAME, slots.getMetadata().getName(),
                SLOT_POD_IS_GHOSTPOD_NAME, "false",
                SLOT_IDS_NAME, SlotIDsAnnotationString.of(freeSlots).toString()
        );
    }

    protected Affinity createAffinity() {


        return new AffinityBuilder()
                .withNewNodeAffinity().withNewRequiredDuringSchedulingIgnoredDuringExecution()
                .addNewNodeSelectorTerm().addNewMatchExpression()
                .withKey(slots.getSpec().getNodeLabel())
                .withValues(ImmutableList.copyOf(freeSlots.stream()
                                                          .map(freeSlot -> String.valueOf(freeSlot.getNodeId()))
                                                          .collect(Collectors.toSet())))
                .withOperator("In")
                .endMatchExpression().endNodeSelectorTerm().endRequiredDuringSchedulingIgnoredDuringExecution()
                .endNodeAffinity()
                .build();
    }

    public abstract void create(KubernetesClient client);
}
