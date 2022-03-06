package de.tuberlin.batchjoboperator.common.crd.scheduling;

import de.tuberlin.batchjoboperator.common.NamespacedName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Objects;


@NoArgsConstructor
@Data
@AllArgsConstructor
public class SchedulingJobState {
    private NamespacedName name;
    private SchedulingJobStateEnum state;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SchedulingJobState that = (SchedulingJobState) o;
        return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    public enum SchedulingJobStateEnum {
        Submitted,
        Scheduled,// Currently unused as there is currently no reliable way to set the ScheduledEvent for all Jobs
        Running,// Currently unused as there is currently no reliable way to set the RunningEvent for all Jobs
        Completed
    }
}
