package de.tuberlin.batchjoboperator.common.crd.scheduling;

import lombok.Data;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Data
public class SchedulingStatus {
    private SchedulingStatusState state = SchedulingStatusState.INITIAL;
    private Set<AbstractSchedulingJobCondition> conditions = new HashSet<>();
    @Nullable
    private List<String> problems;
}

