package de.tuberlin.esi.common.crd.batchjob;

import de.tuberlin.esi.common.crd.NamespacedName;
import lombok.Data;

import javax.annotation.Nullable;
import java.time.Instant;

@Data
public class ScheduledEvents {
    private String start;
    private String stop;
    private Boolean successful;
    @Nullable
    private NamespacedName scheduling;


    public static ScheduledEvents startEvent(@Nullable NamespacedName scheduling) {
        ScheduledEvents scheduledEvents = new ScheduledEvents();
        scheduledEvents.setStart(Instant.now().toString());
        scheduledEvents.setScheduling(scheduling);
        return scheduledEvents;
    }


    public ScheduledEvents stopEvent(boolean successful) {
        this.setStop(Instant.now().toString());
        this.setSuccessful(successful);
        return this;
    }
}
