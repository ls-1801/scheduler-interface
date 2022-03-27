package de.tuberlin.batchjoboperator.schedulingreconciler.external;

import de.tuberlin.batchjoboperator.common.crd.batchjob.BatchJobState;
import de.tuberlin.batchjoboperator.common.crd.batchjob.ScheduledEvents;
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
