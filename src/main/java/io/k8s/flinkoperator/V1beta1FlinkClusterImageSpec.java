package io.k8s.flinkoperator;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.fabric8.kubernetes.api.model.LocalObjectReference;

import javax.annotation.Nullable;
import java.util.ArrayList;

// ImageSpec defines Flink image of JobManager and TaskManager containers.
public class V1beta1FlinkClusterImageSpec {
    // Flink image name.
    @JsonProperty("name")
    private String name;

    // Image pull policy. One of Always, Never, IfNotPresent. Defaults to Always
    // if :latest tag is specified, or IfNotPresent otherwise.
    @JsonProperty("pullPolicy")
    @Nullable
    private String pullPolicy;

    // Secrets for image pull.
    @JsonProperty("pullSecrets")
    @Nullable
    private ArrayList<LocalObjectReference> pullSecrets;


    public V1beta1FlinkClusterImageSpec name(String name) {
        this.name = name;
        return this;
    }
}
