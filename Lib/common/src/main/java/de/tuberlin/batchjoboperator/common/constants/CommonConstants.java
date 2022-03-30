package de.tuberlin.batchjoboperator.common.constants;

public class CommonConstants {
    public static final String PREFIX = "batchjob.tuberlin.de/";
    public static final String MANAGED_BY_LABEL_NAME = "batchjob/managed-by";
    public static final String MANAGED_BY_LABEL_VALUE = "BatchJob-Operator";
    public static final String SPARK_POD_LABEL = "sparkoperator.k8s.io/app-name";
    public static final String FLINK_POD_LABEL = "cluster";
}
