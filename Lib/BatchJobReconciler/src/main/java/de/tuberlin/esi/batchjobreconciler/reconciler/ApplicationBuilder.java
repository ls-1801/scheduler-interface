package de.tuberlin.esi.batchjobreconciler.reconciler;

import com.google.common.collect.ImmutableList;
import de.tuberlin.esi.batchjobreconciler.reconciler.flink.FlinkClusterBuilder;
import de.tuberlin.esi.batchjobreconciler.reconciler.spark.SparkApplicationBuilder;
import de.tuberlin.esi.common.crd.batchjob.BatchJob;
import de.tuberlin.esi.common.crd.testbed.SlotIDsAnnotationString;
import de.tuberlin.esi.common.crd.testbed.SlotOccupationStatus;
import de.tuberlin.esi.common.crd.testbed.SlotState;
import de.tuberlin.esi.common.crd.testbed.Testbed;
import io.fabric8.kubernetes.api.model.Affinity;
import io.fabric8.kubernetes.api.model.AffinityBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static de.tuberlin.esi.common.constants.SlotsConstants.SLOT_IDS_NAME;
import static de.tuberlin.esi.common.constants.SlotsConstants.SLOT_POD_IS_GHOSTPOD_NAME;
import static de.tuberlin.esi.common.constants.SlotsConstants.SLOT_POD_LABEL_NAME;

public abstract class ApplicationBuilder {

    protected final BatchJob job;
    protected final Testbed testbed;
    protected final List<SlotOccupationStatus> freeSlots;
    protected String namespace;


    protected ApplicationBuilder(BatchJob job, Testbed testbed) {
        this.testbed = testbed;
        this.freeSlots = testbed.getStatus().getSlots().stream()
                                .filter(occ -> occ.getState() == SlotState.FREE).collect(Collectors.toList());
        this.job = job;
    }

    public static ApplicationBuilder forJob(BatchJob job, Testbed slots) {
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
                SLOT_POD_LABEL_NAME, testbed.getMetadata().getName(),
                SLOT_POD_IS_GHOSTPOD_NAME, "false",
                SLOT_IDS_NAME, SlotIDsAnnotationString.of(freeSlots).toString()
        );
    }

    protected Affinity createAffinity() {


        return new AffinityBuilder()
                .withNewNodeAffinity().withNewRequiredDuringSchedulingIgnoredDuringExecution()
                .addNewNodeSelectorTerm().addNewMatchExpression()
                .withKey(testbed.getSpec().getNodeLabel())
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
