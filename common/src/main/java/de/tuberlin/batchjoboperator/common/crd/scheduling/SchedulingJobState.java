package de.tuberlin.batchjoboperator.common.crd.scheduling;

import de.tuberlin.batchjoboperator.common.NamespacedName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@NoArgsConstructor
@Data
@AllArgsConstructor
public class SchedulingJobState {
    private NamespacedName name;
    private SchedulingJobStateEnum state;

    public enum SchedulingJobStateEnum {
        InQueue,
        Submitted,
        Scheduled,
        Completed
    }
}
