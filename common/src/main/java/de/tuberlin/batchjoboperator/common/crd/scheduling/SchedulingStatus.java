package de.tuberlin.batchjoboperator.common.crd.scheduling;

import lombok.Data;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Data
public class SchedulingStatus {
    private SchedulingState state = SchedulingState.AcquireState;
    private Set<AbstractSchedulingJobCondition> conditions = new HashSet<>();
    private Set<SchedulingJobState> jobStates = new HashSet<>();

    @Nullable
    private List<String> problems;
}

