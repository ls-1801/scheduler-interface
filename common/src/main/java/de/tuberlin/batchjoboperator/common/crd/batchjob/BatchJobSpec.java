package de.tuberlin.batchjoboperator.common.crd.batchjob;

import k8s.flinkoperator.V1beta1FlinkClusterSpecJob;
import k8s.sparkoperator.V1beta2SparkApplicationSpec;
import lombok.Data;

import javax.annotation.Nullable;

@Data
public class BatchJobSpec {

    private String foo;
    private boolean requeue;
    @Nullable
    private V1beta2SparkApplicationSpec sparkSpec;
    @Nullable
    private V1beta1FlinkClusterSpecJob flinkSpec;

}