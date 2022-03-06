package de.tuberlin.batchjoboperator.common.crd.scheduling;

import de.tuberlin.batchjoboperator.common.NamespacedName;
import lombok.Data;

import javax.annotation.Nullable;
import java.util.Set;

@Data
public class AbstractSchedulingJobCondition {
    protected String condition;
    protected Boolean value = false;

    @Nullable
    protected NamespacedName jobName;
    @Nullable
    protected Set<Integer> slotIds;
    @Nullable
    protected Integer numberOfSlotsRequired;

    @Nullable
    protected String lastUpdateTimestamp;
    @Nullable
    protected NamespacedName slotsName;
    @Nullable
    protected String error;

    @Nullable
    private Set<JobConditionValue> jobs;
}

