package de.tuberlin.esi.common.crd.batchjob;

import de.tuberlin.esi.common.crd.NamespacedName;
import lombok.Data;

import javax.annotation.Nullable;

@Data
public class AbstractBatchJobCondition {

    protected String condition;
    protected Boolean value = false;
    protected NamespacedName name;

    @Nullable
    protected String error;
    @Nullable
    protected String lastUpdateTimestamp;
}
