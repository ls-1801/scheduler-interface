package de.tuberlin.esi.common.crd.scheduling;

import de.tuberlin.esi.common.crd.NamespacedName;
import lombok.Data;

import javax.annotation.Nullable;
import java.util.Set;

/**
 * Both the AbstractSchedulingJobCondition and the AbstractBatchJobCondition are used for the CRD generation.
 * The Scheduling State machine does extend AbstractSchedulingJobCondition and also implements a custom deserializer
 * that deserializes into a concrete class. However, these classes can not actually be abstract, because the other
 * applications need to deserialize it, they don't need to know about concrete classes, but they need to know all
 * possible fields.
 */
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
    protected NamespacedName testbedName;
    @Nullable
    protected String error;

    @Nullable
    private Set<JobConditionValue> jobs;
}

