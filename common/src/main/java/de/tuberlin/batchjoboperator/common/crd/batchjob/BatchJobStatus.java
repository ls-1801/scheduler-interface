package de.tuberlin.batchjoboperator.common.crd.batchjob;

import de.tuberlin.batchjoboperator.common.crd.NamespacedName;
import io.fabric8.kubernetes.model.annotation.PrinterColumn;
import io.javaoperatorsdk.operator.api.ObservedGenerationAwareStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@EqualsAndHashCode(callSuper = true)
@Data
public class BatchJobStatus extends ObservedGenerationAwareStatus {

    @PrinterColumn(name = "state")
    private BatchJobState state;
    private List<ScheduledEvents> scheduledEvents;

    @Nullable
    private NamespacedName slots;

    @Nullable
    private NamespacedName activeScheduling;

    @Nullable
    private Set<Integer> slotIds;

    @Nullable
    private Integer replication;

    private List<String> problems;

    private Set<AbstractBatchJobCondition> conditions = new HashSet<>();

    public BatchJobStatus() {
        this.state = BatchJobState.InitState;
        this.scheduledEvents = new ArrayList<>();
    }

    public void stopLatestSchedulingEvent(boolean succesful) {
        scheduledEvents
                .stream()
                .filter(event -> event.getStart() != null && event.getStop() == null && event.getSuccessful() == null)
                .sorted(Comparator.comparing(ScheduledEvents::getStart).reversed())
                .findFirst()
                .ifPresent(event -> event.stopEvent(succesful));
    }

    public void startSchedulingEvent(BatchJob job) {
        if (job.getSpec().getActiveScheduling() != null) {
            scheduledEvents.add(ScheduledEvents.startEvent(job.getSpec().getActiveScheduling()));
        }
        else {
            scheduledEvents.add(ScheduledEvents.startEvent(null));
        }
    }
}
