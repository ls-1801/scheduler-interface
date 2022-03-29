package de.tuberlin.batchjoboperator.common.crd.batchjob;

import de.tuberlin.batchjoboperator.common.crd.NamespacedName;
import lombok.Data;

import javax.annotation.Nullable;

@Data
public abstract class AbstractBatchJobCondition {

    protected String condition;
    protected Boolean value = false;
    protected NamespacedName name;

    @Nullable
    protected String error;
    @Nullable
    protected String lastUpdateTimestamp;
}