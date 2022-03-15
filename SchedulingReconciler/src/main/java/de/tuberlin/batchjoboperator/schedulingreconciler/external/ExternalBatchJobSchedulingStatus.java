package de.tuberlin.batchjoboperator.schedulingreconciler.external;

import de.tuberlin.batchjoboperator.common.crd.scheduling.SchedulingJobState;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Jacksonized
@Builder
public class ExternalBatchJobSchedulingStatus {

    String name;
    SchedulingJobState.SchedulingJobStateEnum state;
}
