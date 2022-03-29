package de.tuberlin.batchjoboperator.common.crd.batchjob;

import de.tuberlin.batchjoboperator.common.crd.NamespacedName;
import k8s.flinkoperator.V1beta1FlinkClusterSpecJob;
import k8s.sparkoperator.V1beta2SparkApplicationSpec;
import lombok.Data;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

@Data
public class BatchJobSpec {

    @Nullable
    private V1beta2SparkApplicationSpec sparkSpec;
    @Nullable
    private V1beta1FlinkClusterSpecJob flinkSpec;

    @Nullable
    private CreationRequest creationRequest;

    @Nullable
    private NamespacedName activeScheduling;

    @Nullable
    private Map<String, List<Map<String, String>>> externalScheduler;
}