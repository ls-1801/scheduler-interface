package de.tuberlin.batchjoboperator.common.crd.scheduling;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SchedulingStatusCondition {
    private String jobName;
    private String namespace;
    private SchedulingCondition state;
    private Long replication;
}
