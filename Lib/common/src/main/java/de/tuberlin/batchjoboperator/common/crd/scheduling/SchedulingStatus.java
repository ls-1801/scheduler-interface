package de.tuberlin.batchjoboperator.common.crd.scheduling;

import io.fabric8.kubernetes.model.annotation.PrinterColumn;
import lombok.Data;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Data
public class SchedulingStatus {
    @PrinterColumn(name = "state")
    private SchedulingState state = null;
    private Set<AbstractSchedulingJobCondition> conditions = new HashSet<>();
    private Set<SchedulingJobState> jobStates = new HashSet<>();

    @Nullable
    private List<String> problems;
}

