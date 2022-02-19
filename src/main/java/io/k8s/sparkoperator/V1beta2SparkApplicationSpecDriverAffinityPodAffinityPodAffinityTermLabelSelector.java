package io.k8s.sparkoperator;/*
 * Kubernetes
 * No description provided (generated by Openapi Generator https://github.com/openapitools/openapi-generator)
 *
 * The version of the OpenAPI document: v1.21.1
 *
 *
 * NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech).
 * https://openapi-generator.tech
 * Do not edit the class manually.
 */


import io.swagger.annotations.ApiModelProperty;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * V1beta2SparkApplicationSpecDriverAffinityPodAffinityPodAffinityTermLabelSelector
 */
@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaClientCodegen", date = "2022-01-30T13:32" +
        ":37.998Z[Etc/UTC]")
public class V1beta2SparkApplicationSpecDriverAffinityPodAffinityPodAffinityTermLabelSelector {

    private List<V1beta2SparkApplicationSpecDriverAffinityNodeAffinityPreferenceMatchExpressions> matchExpressions =
            null;


    private Map<String, String> matchLabels = null;


    public V1beta2SparkApplicationSpecDriverAffinityPodAffinityPodAffinityTermLabelSelector matchExpressions(List<V1beta2SparkApplicationSpecDriverAffinityNodeAffinityPreferenceMatchExpressions> matchExpressions) {

        this.matchExpressions = matchExpressions;
        return this;
    }

    public V1beta2SparkApplicationSpecDriverAffinityPodAffinityPodAffinityTermLabelSelector addMatchExpressionsItem(V1beta2SparkApplicationSpecDriverAffinityNodeAffinityPreferenceMatchExpressions matchExpressionsItem) {
        if (this.matchExpressions == null) {
            this.matchExpressions = new ArrayList<>();
        }
        this.matchExpressions.add(matchExpressionsItem);
        return this;
    }

    /**
     * Get matchExpressions
     *
     * @return matchExpressions
     **/
    @javax.annotation.Nullable
    @ApiModelProperty(value = "")

    public List<V1beta2SparkApplicationSpecDriverAffinityNodeAffinityPreferenceMatchExpressions> getMatchExpressions() {
        return matchExpressions;
    }


    public void setMatchExpressions(List<V1beta2SparkApplicationSpecDriverAffinityNodeAffinityPreferenceMatchExpressions> matchExpressions) {
        this.matchExpressions = matchExpressions;
    }


    public V1beta2SparkApplicationSpecDriverAffinityPodAffinityPodAffinityTermLabelSelector matchLabels(Map<String,
            String> matchLabels) {

        this.matchLabels = matchLabels;
        return this;
    }

    public V1beta2SparkApplicationSpecDriverAffinityPodAffinityPodAffinityTermLabelSelector putMatchLabelsItem(String key, String matchLabelsItem) {
        if (this.matchLabels == null) {
            this.matchLabels = new HashMap<>();
        }
        this.matchLabels.put(key, matchLabelsItem);
        return this;
    }

    /**
     * Get matchLabels
     *
     * @return matchLabels
     **/
    @javax.annotation.Nullable
    @ApiModelProperty(value = "")

    public Map<String, String> getMatchLabels() {
        return matchLabels;
    }


    public void setMatchLabels(Map<String, String> matchLabels) {
        this.matchLabels = matchLabels;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        V1beta2SparkApplicationSpecDriverAffinityPodAffinityPodAffinityTermLabelSelector v1beta2SparkApplicationSpecDriverAffinityPodAffinityPodAffinityTermLabelSelector = (V1beta2SparkApplicationSpecDriverAffinityPodAffinityPodAffinityTermLabelSelector) o;
        return Objects.equals(this.matchExpressions,
                v1beta2SparkApplicationSpecDriverAffinityPodAffinityPodAffinityTermLabelSelector.matchExpressions) &&
                Objects.equals(this.matchLabels,
                        v1beta2SparkApplicationSpecDriverAffinityPodAffinityPodAffinityTermLabelSelector.matchLabels);
    }

    @Override
    public int hashCode() {
        return Objects.hash(matchExpressions, matchLabels);
    }


    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class V1beta2SparkApplicationSpecDriverAffinityPodAffinityPodAffinityTermLabelSelector {\n");
        sb.append("    matchExpressions: ").append(toIndentedString(matchExpressions)).append("\n");
        sb.append("    matchLabels: ").append(toIndentedString(matchLabels)).append("\n");
        sb.append("}");
        return sb.toString();
    }

    /**
     * Convert the given object to string with each line indented by 4 spaces
     * (except the first line).
     */
    private String toIndentedString(Object o) {
        if (o == null) {
            return "null";
        }
        return o.toString().replace("\n", "\n    ");
    }

}

