package de.tuberlin.batchjoboperator.crd.batchjob;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ScheduledEvents {
    private LocalDateTime start;
    private LocalDateTime stop;
    private Boolean successful;
    private String scheduledNode;


    public static ScheduledEvents startEvent(String scheduledNode) {
        ScheduledEvents scheduledEvents = new ScheduledEvents();
        scheduledEvents.setStart(LocalDateTime.now());
        scheduledEvents.setScheduledNode(scheduledNode);
        return scheduledEvents;
    }


    public ScheduledEvents stopEvent(boolean successful) {
        this.setStop(LocalDateTime.now());
        this.setSuccessful(successful);
        return this;
    }
}
