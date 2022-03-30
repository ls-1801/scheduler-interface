package de.tuberlin.esi.schedulingreconciler.external;

import de.tuberlin.esi.common.crd.batchjob.BatchJobState;
import de.tuberlin.esi.common.crd.batchjob.ScheduledEvents;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

import java.util.List;
import java.util.Map;

@Data
@Builder
@Jacksonized
public class ExternalBatchJob {
    String name;
    BatchJobState state;
    Map<String, List<Map<String, String>>> externalScheduler;
    List<ScheduledEvents> scheduledEvents;
}
