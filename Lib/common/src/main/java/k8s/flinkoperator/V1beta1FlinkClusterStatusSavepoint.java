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
 * V1beta1FlinkClusterStatusSavepoint
 */
@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaClientCodegen", date = "2022-02-19T16:59" +
        ":54.980Z[Etc/UTC]")
public class V1beta1FlinkClusterStatusSavepoint {
    @JsonProperty("jobID")
    private String jobID;

    @JsonProperty("message")
    private String message;

    @JsonProperty("requestTime")
    private String requestTime;

    @JsonProperty("state")
    private String state;

    @JsonProperty("triggerID")
    private String triggerID;

    @JsonProperty("triggerReason")
    private String triggerReason;

    @JsonProperty("triggerTime")
    private String triggerTime;


    public V1beta1FlinkClusterStatusSavepoint jobID(String jobID) {

        this.jobID = jobID;
        return this;
    }

    /**
     * Get jobID
     *
     * @return jobID
     **/
    @javax.annotation.Nullable
    @ApiModelProperty(value = "")

    public String getJobID() {
        return jobID;
    }


    public void setJobID(String jobID) {
        this.jobID = jobID;
    }


    public V1beta1FlinkClusterStatusSavepoint message(String message) {

        this.message = message;
        return this;
    }

    /**
     * Get message
     *
     * @return message
     **/
    @javax.annotation.Nullable
    @ApiModelProperty(value = "")

    public String getMessage() {
        return message;
    }


    public void setMessage(String message) {
        this.message = message;
    }


    public V1beta1FlinkClusterStatusSavepoint requestTime(String requestTime) {

        this.requestTime = requestTime;
        return this;
    }

    /**
     * Get requestTime
     *
     * @return requestTime
     **/
    @javax.annotation.Nullable
    @ApiModelProperty(value = "")

    public String getRequestTime() {
        return requestTime;
    }


    public void setRequestTime(String requestTime) {
        this.requestTime = requestTime;
    }


    public V1beta1FlinkClusterStatusSavepoint state(String state) {

        this.state = state;
        return this;
    }

    /**
     * Get state
     *
     * @return state
     **/
    @ApiModelProperty(required = true, value = "")

    public String getState() {
        return state;
    }


    public void setState(String state) {
        this.state = state;
    }


    public V1beta1FlinkClusterStatusSavepoint triggerID(String triggerID) {

        this.triggerID = triggerID;
        return this;
    }

    /**
     * Get triggerID
     *
     * @return triggerID
     **/
    @javax.annotation.Nullable
    @ApiModelProperty(value = "")

    public String getTriggerID() {
        return triggerID;
    }


    public void setTriggerID(String triggerID) {
        this.triggerID = triggerID;
    }


    public V1beta1FlinkClusterStatusSavepoint triggerReason(String triggerReason) {

        this.triggerReason = triggerReason;
        return this;
    }

    /**
     * Get triggerReason
     *
     * @return triggerReason
     **/
    @javax.annotation.Nullable
    @ApiModelProperty(value = "")

    public String getTriggerReason() {
        return triggerReason;
    }


    public void setTriggerReason(String triggerReason) {
        this.triggerReason = triggerReason;
    }


    public V1beta1FlinkClusterStatusSavepoint triggerTime(String triggerTime) {

        this.triggerTime = triggerTime;
        return this;
    }

    /**
     * Get triggerTime
     *
     * @return triggerTime
     **/
    @javax.annotation.Nullable
    @ApiModelProperty(value = "")

    public String getTriggerTime() {
        return triggerTime;
    }


    public void setTriggerTime(String triggerTime) {
        this.triggerTime = triggerTime;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        V1beta1FlinkClusterStatusSavepoint v1beta1FlinkClusterStatusSavepoint = (V1beta1FlinkClusterStatusSavepoint) o;
        return Objects.equals(this.jobID, v1beta1FlinkClusterStatusSavepoint.jobID) &&
                Objects.equals(this.message, v1beta1FlinkClusterStatusSavepoint.message) &&
                Objects.equals(this.requestTime, v1beta1FlinkClusterStatusSavepoint.requestTime) &&
                Objects.equals(this.state, v1beta1FlinkClusterStatusSavepoint.state) &&
                Objects.equals(this.triggerID, v1beta1FlinkClusterStatusSavepoint.triggerID) &&
                Objects.equals(this.triggerReason, v1beta1FlinkClusterStatusSavepoint.triggerReason) &&
                Objects.equals(this.triggerTime, v1beta1FlinkClusterStatusSavepoint.triggerTime);
    }

    @Override
    public int hashCode() {
        return Objects.hash(jobID, message, requestTime, state, triggerID, triggerReason, triggerTime);
    }


    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class V1beta1FlinkClusterStatusSavepoint {\n");
        sb.append("    jobID: ").append(toIndentedString(jobID)).append("\n");
        sb.append("    message: ").append(toIndentedString(message)).append("\n");
        sb.append("    requestTime: ").append(toIndentedString(requestTime)).append("\n");
        sb.append("    state: ").append(toIndentedString(state)).append("\n");
        sb.append("    triggerID: ").append(toIndentedString(triggerID)).append("\n");
        sb.append("    triggerReason: ").append(toIndentedString(triggerReason)).append("\n");
        sb.append("    triggerTime: ").append(toIndentedString(triggerTime)).append("\n");
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

