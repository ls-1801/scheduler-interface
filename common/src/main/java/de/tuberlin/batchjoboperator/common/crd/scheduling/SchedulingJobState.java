package de.tuberlin.batchjoboperator.common.crd.scheduling;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.tuberlin.batchjoboperator.common.crd.NamespacedName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@NoArgsConstructor
@Data
@AllArgsConstructor
public class SchedulingJobState {
    private NamespacedName name;
    private SchedulingJobStateEnum state;

    // Order is Important, because only the most advanced state is saved. So the first enum item will be override by
    // the second
    public enum SchedulingJobStateEnum {
        @JsonProperty("Acquiring")
        ACQUIRING,
        @JsonProperty("InQueue")
        IN_QUEUE,
        @JsonProperty("Submitted")
        SUBMITTED,
        @JsonProperty("Scheduled")
        SCHEDULED,
        @JsonProperty("Completed")
        COMPLETED
    }
}
