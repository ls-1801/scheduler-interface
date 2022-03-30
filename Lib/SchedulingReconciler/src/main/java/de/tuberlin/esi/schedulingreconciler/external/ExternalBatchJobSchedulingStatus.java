package de.tuberlin.esi.schedulingreconciler.external;

import de.tuberlin.esi.common.crd.scheduling.SchedulingJobState;
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
