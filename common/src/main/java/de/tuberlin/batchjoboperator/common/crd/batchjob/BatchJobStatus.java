package de.tuberlin.batchjoboperator.common.crd.batchjob;

import de.tuberlin.batchjoboperator.common.NamespacedName;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import lombok.Data;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static de.tuberlin.batchjoboperator.common.constants.SchedulingConstants.ACTIVE_SCHEDULING_LABEL_NAME;
import static de.tuberlin.batchjoboperator.common.constants.SchedulingConstants.ACTIVE_SCHEDULING_LABEL_NAMESPACE;

@Data
public class BatchJobStatus {
    private BatchJobState state;
    @Nullable
    private Long latestResourceVersion;
    private List<ScheduledEvents> scheduledEvents;

    @Nullable
    private NamespacedName slots;
    @Nullable
    private NamespacedName activeScheduling;
    @Nullable
    private Set<Integer> slotIds;
    @Nullable
    private Integer replication;

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

    public void startSchedulingEvent(@Nullable ObjectMeta metadata) {
        var optLabels = Optional.ofNullable(metadata)
                                .map(ObjectMeta::getLabels);
        var name = optLabels.map(map -> map.get(ACTIVE_SCHEDULING_LABEL_NAME)).orElse(null);
        var namespace = optLabels.map(map -> map.get(ACTIVE_SCHEDULING_LABEL_NAMESPACE)).orElse(null);
        if (name == null || namespace == null) {
            scheduledEvents.add(ScheduledEvents.startEvent(new NamespacedName(name, namespace)));
        }
        else {
            scheduledEvents.add(ScheduledEvents.startEvent(null));
        }
    }
}
