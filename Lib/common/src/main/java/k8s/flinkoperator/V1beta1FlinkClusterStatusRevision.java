/*
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


package k8s.flinkoperator;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;

import java.util.Objects;

/**
 * V1beta1FlinkClusterStatusRevision
 */
@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaClientCodegen", date = "2022-02-19T16:59" +
        ":54.980Z[Etc/UTC]")
public class V1beta1FlinkClusterStatusRevision {
    @JsonProperty("collisionCount")
    private Integer collisionCount;

    @JsonProperty("currentRevision")
    private String currentRevision;

    @JsonProperty("nextRevision")
    private String nextRevision;


    public V1beta1FlinkClusterStatusRevision collisionCount(Integer collisionCount) {

        this.collisionCount = collisionCount;
        return this;
    }

    /**
     * Get collisionCount
     *
     * @return collisionCount
     **/
    @javax.annotation.Nullable
    @ApiModelProperty(value = "")

    public Integer getCollisionCount() {
        return collisionCount;
    }


    public void setCollisionCount(Integer collisionCount) {
        this.collisionCount = collisionCount;
    }


    public V1beta1FlinkClusterStatusRevision currentRevision(String currentRevision) {

        this.currentRevision = currentRevision;
        return this;
    }

    /**
     * Get currentRevision
     *
     * @return currentRevision
     **/
    @javax.annotation.Nullable
    @ApiModelProperty(value = "")

    public String getCurrentRevision() {
        return currentRevision;
    }


    public void setCurrentRevision(String currentRevision) {
        this.currentRevision = currentRevision;
    }


    public V1beta1FlinkClusterStatusRevision nextRevision(String nextRevision) {

        this.nextRevision = nextRevision;
        return this;
    }

    /**
     * Get nextRevision
     *
     * @return nextRevision
     **/
    @javax.annotation.Nullable
    @ApiModelProperty(value = "")

    public String getNextRevision() {
        return nextRevision;
    }


    public void setNextRevision(String nextRevision) {
        this.nextRevision = nextRevision;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        V1beta1FlinkClusterStatusRevision v1beta1FlinkClusterStatusRevision = (V1beta1FlinkClusterStatusRevision) o;
        return Objects.equals(this.collisionCount, v1beta1FlinkClusterStatusRevision.collisionCount) &&
                Objects.equals(this.currentRevision, v1beta1FlinkClusterStatusRevision.currentRevision) &&
                Objects.equals(this.nextRevision, v1beta1FlinkClusterStatusRevision.nextRevision);
    }

    @Override
    public int hashCode() {
        return Objects.hash(collisionCount, currentRevision, nextRevision);
    }


    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class V1beta1FlinkClusterStatusRevision {\n");
        sb.append("    collisionCount: ").append(toIndentedString(collisionCount)).append("\n");
        sb.append("    currentRevision: ").append(toIndentedString(currentRevision)).append("\n");
        sb.append("    nextRevision: ").append(toIndentedString(nextRevision)).append("\n");
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

