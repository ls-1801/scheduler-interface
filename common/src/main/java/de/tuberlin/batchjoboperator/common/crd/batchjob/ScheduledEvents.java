package de.tuberlin.batchjoboperator.common.crd.batchjob;

import de.tuberlin.batchjoboperator.common.NamespacedName;
import lombok.Data;

import javax.annotation.Nullable;
import java.time.LocalDateTime;

@Data
public class ScheduledEvents {
    private String start;
    private String stop;
    private Boolean successful;
    @Nullable
    private NamespacedName scheduling;


    public static ScheduledEvents startEvent(@Nullable NamespacedName scheduling) {
        ScheduledEvents scheduledEvents = new ScheduledEvents();
        scheduledEvents.setStart(LocalDateTime.now().toString());
        scheduledEvents.setScheduling(scheduling);
        return scheduledEvents;
    }


    public ScheduledEvents stopEvent(boolean successful) {
        this.setStop(LocalDateTime.now().toString());
        this.setSuccessful(successful);
        return this;
    }
}
