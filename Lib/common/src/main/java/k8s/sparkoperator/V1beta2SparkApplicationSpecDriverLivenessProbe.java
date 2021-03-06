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

import java.util.Objects;

/**
 * V1beta2SparkApplicationSpecDriverLivenessProbe
 */
@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaClientCodegen", date = "2022-01-30T13:32" +
        ":37.998Z[Etc/UTC]")
public class V1beta2SparkApplicationSpecDriverLivenessProbe {

    private V1beta2SparkApplicationSpecDriverLifecyclePostStartExec exec;


    private Integer failureThreshold;


    private V1beta2SparkApplicationSpecDriverLifecyclePostStartHttpGet httpGet;


    private Integer initialDelaySeconds;


    private Integer periodSeconds;


    private Integer successThreshold;


    private V1beta2SparkApplicationSpecDriverLifecyclePostStartTcpSocket tcpSocket;


    private Integer timeoutSeconds;


    public V1beta2SparkApplicationSpecDriverLivenessProbe exec(V1beta2SparkApplicationSpecDriverLifecyclePostStartExec exec) {

        this.exec = exec;
        return this;
    }

    /**
     * Get exec
     *
     * @return exec
     **/
    @javax.annotation.Nullable
    @ApiModelProperty(value = "")

    public V1beta2SparkApplicationSpecDriverLifecyclePostStartExec getExec() {
        return exec;
    }


    public void setExec(V1beta2SparkApplicationSpecDriverLifecyclePostStartExec exec) {
        this.exec = exec;
    }


    public V1beta2SparkApplicationSpecDriverLivenessProbe failureThreshold(Integer failureThreshold) {

        this.failureThreshold = failureThreshold;
        return this;
    }

    /**
     * Get failureThreshold
     *
     * @return failureThreshold
     **/
    @javax.annotation.Nullable
    @ApiModelProperty(value = "")

    public Integer getFailureThreshold() {
        return failureThreshold;
    }


    public void setFailureThreshold(Integer failureThreshold) {
        this.failureThreshold = failureThreshold;
    }


    public V1beta2SparkApplicationSpecDriverLivenessProbe httpGet(V1beta2SparkApplicationSpecDriverLifecyclePostStartHttpGet httpGet) {

        this.httpGet = httpGet;
        return this;
    }

    /**
     * Get httpGet
     *
     * @return httpGet
     **/
    @javax.annotation.Nullable
    @ApiModelProperty(value = "")

    public V1beta2SparkApplicationSpecDriverLifecyclePostStartHttpGet getHttpGet() {
        return httpGet;
    }


    public void setHttpGet(V1beta2SparkApplicationSpecDriverLifecyclePostStartHttpGet httpGet) {
        this.httpGet = httpGet;
    }


    public V1beta2SparkApplicationSpecDriverLivenessProbe initialDelaySeconds(Integer initialDelaySeconds) {

        this.initialDelaySeconds = initialDelaySeconds;
        return this;
    }

    /**
     * Get initialDelaySeconds
     *
     * @return initialDelaySeconds
     **/
    @javax.annotation.Nullable
    @ApiModelProperty(value = "")

    public Integer getInitialDelaySeconds() {
        return initialDelaySeconds;
    }


    public void setInitialDelaySeconds(Integer initialDelaySeconds) {
        this.initialDelaySeconds = initialDelaySeconds;
    }


    public V1beta2SparkApplicationSpecDriverLivenessProbe periodSeconds(Integer periodSeconds) {

        this.periodSeconds = periodSeconds;
        return this;
    }

    /**
     * Get periodSeconds
     *
     * @return periodSeconds
     **/
    @javax.annotation.Nullable
    @ApiModelProperty(value = "")

    public Integer getPeriodSeconds() {
        return periodSeconds;
    }


    public void setPeriodSeconds(Integer periodSeconds) {
        this.periodSeconds = periodSeconds;
    }


    public V1beta2SparkApplicationSpecDriverLivenessProbe successThreshold(Integer successThreshold) {

        this.successThreshold = successThreshold;
        return this;
    }

    /**
     * Get successThreshold
     *
     * @return successThreshold
     **/
    @javax.annotation.Nullable
    @ApiModelProperty(value = "")

    public Integer getSuccessThreshold() {
        return successThreshold;
    }


    public void setSuccessThreshold(Integer successThreshold) {
        this.successThreshold = successThreshold;
    }


    public V1beta2SparkApplicationSpecDriverLivenessProbe tcpSocket(V1beta2SparkApplicationSpecDriverLifecyclePostStartTcpSocket tcpSocket) {

        this.tcpSocket = tcpSocket;
        return this;
    }

    /**
     * Get tcpSocket
     *
     * @return tcpSocket
     **/
    @javax.annotation.Nullable
    @ApiModelProperty(value = "")

    public V1beta2SparkApplicationSpecDriverLifecyclePostStartTcpSocket getTcpSocket() {
        return tcpSocket;
    }


    public void setTcpSocket(V1beta2SparkApplicationSpecDriverLifecyclePostStartTcpSocket tcpSocket) {
        this.tcpSocket = tcpSocket;
    }


    public V1beta2SparkApplicationSpecDriverLivenessProbe timeoutSeconds(Integer timeoutSeconds) {

        this.timeoutSeconds = timeoutSeconds;
        return this;
    }

    /**
     * Get timeoutSeconds
     *
     * @return timeoutSeconds
     **/
    @javax.annotation.Nullable
    @ApiModelProperty(value = "")

    public Integer getTimeoutSeconds() {
        return timeoutSeconds;
    }


    public void setTimeoutSeconds(Integer timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        V1beta2SparkApplicationSpecDriverLivenessProbe v1beta2SparkApplicationSpecDriverLivenessProbe =
                (V1beta2SparkApplicationSpecDriverLivenessProbe) o;
        return Objects.equals(this.exec, v1beta2SparkApplicationSpecDriverLivenessProbe.exec) &&
                Objects.equals(this.failureThreshold,
                        v1beta2SparkApplicationSpecDriverLivenessProbe.failureThreshold) &&
                Objects.equals(this.httpGet, v1beta2SparkApplicationSpecDriverLivenessProbe.httpGet) &&
                Objects.equals(this.initialDelaySeconds,
                        v1beta2SparkApplicationSpecDriverLivenessProbe.initialDelaySeconds) &&
                Objects.equals(this.periodSeconds, v1beta2SparkApplicationSpecDriverLivenessProbe.periodSeconds) &&
                Objects.equals(this.successThreshold,
                        v1beta2SparkApplicationSpecDriverLivenessProbe.successThreshold) &&
                Objects.equals(this.tcpSocket, v1beta2SparkApplicationSpecDriverLivenessProbe.tcpSocket) &&
                Objects.equals(this.timeoutSeconds, v1beta2SparkApplicationSpecDriverLivenessProbe.timeoutSeconds);
    }

    @Override
    public int hashCode() {
        return Objects.hash(exec, failureThreshold, httpGet, initialDelaySeconds, periodSeconds, successThreshold,
                tcpSocket, timeoutSeconds);
    }


    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class V1beta2SparkApplicationSpecDriverLivenessProbe {\n");
        sb.append("    exec: ").append(toIndentedString(exec)).append("\n");
        sb.append("    failureThreshold: ").append(toIndentedString(failureThreshold)).append("\n");
        sb.append("    httpGet: ").append(toIndentedString(httpGet)).append("\n");
        sb.append("    initialDelaySeconds: ").append(toIndentedString(initialDelaySeconds)).append("\n");
        sb.append("    periodSeconds: ").append(toIndentedString(periodSeconds)).append("\n");
        sb.append("    successThreshold: ").append(toIndentedString(successThreshold)).append("\n");
        sb.append("    tcpSocket: ").append(toIndentedString(tcpSocket)).append("\n");
        sb.append("    timeoutSeconds: ").append(toIndentedString(timeoutSeconds)).append("\n");
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

