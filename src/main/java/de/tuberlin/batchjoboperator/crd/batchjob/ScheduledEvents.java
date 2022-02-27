package de.tuberlin.batchjoboperator.crd.batchjob;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ScheduledEvents {
    private String start;
    private String stop;
    private Boolean successful;


    public static ScheduledEvents startEvent() {
        ScheduledEvents scheduledEvents = new ScheduledEvents();
        scheduledEvents.setStart(LocalDateTime.now().toString());
        return scheduledEvents;
    }


    public ScheduledEvents stopEvent(boolean successful) {
        this.setStop(LocalDateTime.now().toString());
        this.setSuccessful(successful);
        return this;
    }
}
