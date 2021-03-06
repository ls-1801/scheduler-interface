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
 * V1beta2SparkApplicationSpecDriverAffinityNodeAffinityRequiredDuringSchedulingIgnoredDuringExecution
 */
@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaClientCodegen", date = "2022-01-30T13:32" +
        ":37.998Z[Etc/UTC]")
public class V1beta2SparkApplicationSpecDriverAffinityNodeAffinityRequiredDuringSchedulingIgnoredDuringExecution {

    private List<V1beta2SparkApplicationSpecDriverAffinityNodeAffinityPreference> nodeSelectorTerms = new ArrayList<>();


    public V1beta2SparkApplicationSpecDriverAffinityNodeAffinityRequiredDuringSchedulingIgnoredDuringExecution nodeSelectorTerms(List<V1beta2SparkApplicationSpecDriverAffinityNodeAffinityPreference> nodeSelectorTerms) {

        this.nodeSelectorTerms = nodeSelectorTerms;
        return this;
    }

    public V1beta2SparkApplicationSpecDriverAffinityNodeAffinityRequiredDuringSchedulingIgnoredDuringExecution addNodeSelectorTermsItem(V1beta2SparkApplicationSpecDriverAffinityNodeAffinityPreference nodeSelectorTermsItem) {
        this.nodeSelectorTerms.add(nodeSelectorTermsItem);
        return this;
    }

    /**
     * Get nodeSelectorTerms
     *
     * @return nodeSelectorTerms
     **/
    @ApiModelProperty(required = true, value = "")

    public List<V1beta2SparkApplicationSpecDriverAffinityNodeAffinityPreference> getNodeSelectorTerms() {
        return nodeSelectorTerms;
    }


    public void setNodeSelectorTerms(List<V1beta2SparkApplicationSpecDriverAffinityNodeAffinityPreference> nodeSelectorTerms) {
        this.nodeSelectorTerms = nodeSelectorTerms;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        V1beta2SparkApplicationSpecDriverAffinityNodeAffinityRequiredDuringSchedulingIgnoredDuringExecution v1beta2SparkApplicationSpecDriverAffinityNodeAffinityRequiredDuringSchedulingIgnoredDuringExecution = (V1beta2SparkApplicationSpecDriverAffinityNodeAffinityRequiredDuringSchedulingIgnoredDuringExecution) o;
        return Objects.equals(this.nodeSelectorTerms,
                v1beta2SparkApplicationSpecDriverAffinityNodeAffinityRequiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nodeSelectorTerms);
    }


    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class " +
                "V1beta2SparkApplicationSpecDriverAffinityNodeAffinityRequiredDuringSchedulingIgnoredDuringExecution " +
                "{\n");
        sb.append("    nodeSelectorTerms: ").append(toIndentedString(nodeSelectorTerms)).append("\n");
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

