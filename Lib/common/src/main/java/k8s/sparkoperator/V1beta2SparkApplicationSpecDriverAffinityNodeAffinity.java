package k8s.sparkoperator;/*
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
import java.util.List;
import java.util.Objects;

/**
 * V1beta2SparkApplicationSpecDriverAffinityNodeAffinity
 */
@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaClientCodegen", date = "2022-01-30T13:32" +
        ":37.998Z[Etc/UTC]")
public class V1beta2SparkApplicationSpecDriverAffinityNodeAffinity {

    private List<V1beta2SparkApplicationSpecDriverAffinityNodeAffinityPreferredDuringSchedulingIgnoredDuringExecution> preferredDuringSchedulingIgnoredDuringExecution = null;


    private V1beta2SparkApplicationSpecDriverAffinityNodeAffinityRequiredDuringSchedulingIgnoredDuringExecution requiredDuringSchedulingIgnoredDuringExecution;


    public V1beta2SparkApplicationSpecDriverAffinityNodeAffinity preferredDuringSchedulingIgnoredDuringExecution(List<V1beta2SparkApplicationSpecDriverAffinityNodeAffinityPreferredDuringSchedulingIgnoredDuringExecution> preferredDuringSchedulingIgnoredDuringExecution) {

        this.preferredDuringSchedulingIgnoredDuringExecution = preferredDuringSchedulingIgnoredDuringExecution;
        return this;
    }

    public V1beta2SparkApplicationSpecDriverAffinityNodeAffinity addPreferredDuringSchedulingIgnoredDuringExecutionItem(V1beta2SparkApplicationSpecDriverAffinityNodeAffinityPreferredDuringSchedulingIgnoredDuringExecution preferredDuringSchedulingIgnoredDuringExecutionItem) {
        if (this.preferredDuringSchedulingIgnoredDuringExecution == null) {
            this.preferredDuringSchedulingIgnoredDuringExecution = new ArrayList<>();
        }
        this.preferredDuringSchedulingIgnoredDuringExecution.add(preferredDuringSchedulingIgnoredDuringExecutionItem);
        return this;
    }

    /**
     * Get preferredDuringSchedulingIgnoredDuringExecution
     *
     * @return preferredDuringSchedulingIgnoredDuringExecution
     **/
    @javax.annotation.Nullable
    @ApiModelProperty(value = "")

    public List<V1beta2SparkApplicationSpecDriverAffinityNodeAffinityPreferredDuringSchedulingIgnoredDuringExecution> getPreferredDuringSchedulingIgnoredDuringExecution() {
        return preferredDuringSchedulingIgnoredDuringExecution;
    }


    public void setPreferredDuringSchedulingIgnoredDuringExecution(List<V1beta2SparkApplicationSpecDriverAffinityNodeAffinityPreferredDuringSchedulingIgnoredDuringExecution> preferredDuringSchedulingIgnoredDuringExecution) {
        this.preferredDuringSchedulingIgnoredDuringExecution = preferredDuringSchedulingIgnoredDuringExecution;
    }


    public V1beta2SparkApplicationSpecDriverAffinityNodeAffinity requiredDuringSchedulingIgnoredDuringExecution(V1beta2SparkApplicationSpecDriverAffinityNodeAffinityRequiredDuringSchedulingIgnoredDuringExecution requiredDuringSchedulingIgnoredDuringExecution) {

        this.requiredDuringSchedulingIgnoredDuringExecution = requiredDuringSchedulingIgnoredDuringExecution;
        return this;
    }

    /**
     * Get requiredDuringSchedulingIgnoredDuringExecution
     *
     * @return requiredDuringSchedulingIgnoredDuringExecution
     **/
    @javax.annotation.Nullable
    @ApiModelProperty(value = "")

    public V1beta2SparkApplicationSpecDriverAffinityNodeAffinityRequiredDuringSchedulingIgnoredDuringExecution getRequiredDuringSchedulingIgnoredDuringExecution() {
        return requiredDuringSchedulingIgnoredDuringExecution;
    }


    public void setRequiredDuringSchedulingIgnoredDuringExecution(V1beta2SparkApplicationSpecDriverAffinityNodeAffinityRequiredDuringSchedulingIgnoredDuringExecution requiredDuringSchedulingIgnoredDuringExecution) {
        this.requiredDuringSchedulingIgnoredDuringExecution = requiredDuringSchedulingIgnoredDuringExecution;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        V1beta2SparkApplicationSpecDriverAffinityNodeAffinity v1beta2SparkApplicationSpecDriverAffinityNodeAffinity =
                (V1beta2SparkApplicationSpecDriverAffinityNodeAffinity) o;
        return Objects.equals(this.preferredDuringSchedulingIgnoredDuringExecution,
                v1beta2SparkApplicationSpecDriverAffinityNodeAffinity.preferredDuringSchedulingIgnoredDuringExecution) &&
                Objects.equals(this.requiredDuringSchedulingIgnoredDuringExecution,
                        v1beta2SparkApplicationSpecDriverAffinityNodeAffinity.requiredDuringSchedulingIgnoredDuringExecution);
    }

    @Override
    public int hashCode() {
        return Objects.hash(preferredDuringSchedulingIgnoredDuringExecution,
                requiredDuringSchedulingIgnoredDuringExecution);
    }


    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class V1beta2SparkApplicationSpecDriverAffinityNodeAffinity {\n");
        sb.append("    preferredDuringSchedulingIgnoredDuringExecution: ")
          .append(toIndentedString(preferredDuringSchedulingIgnoredDuringExecution)).append("\n");
        sb.append("    requiredDuringSchedulingIgnoredDuringExecution: ")
          .append(toIndentedString(requiredDuringSchedulingIgnoredDuringExecution)).append("\n");
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

