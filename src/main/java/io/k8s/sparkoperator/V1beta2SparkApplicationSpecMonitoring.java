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

import java.util.Objects;

/**
 * V1beta2SparkApplicationSpecMonitoring
 */
@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaClientCodegen", date = "2022-01-30T13:32" +
        ":37.998Z[Etc/UTC]")
public class V1beta2SparkApplicationSpecMonitoring {

    private Boolean exposeDriverMetrics;


    private Boolean exposeExecutorMetrics;


    private String metricsProperties;


    private String metricsPropertiesFile;


    private V1beta2SparkApplicationSpecMonitoringPrometheus prometheus;


    public V1beta2SparkApplicationSpecMonitoring exposeDriverMetrics(Boolean exposeDriverMetrics) {

        this.exposeDriverMetrics = exposeDriverMetrics;
        return this;
    }

    /**
     * Get exposeDriverMetrics
     *
     * @return exposeDriverMetrics
     **/
    @ApiModelProperty(required = true, value = "")

    public Boolean getExposeDriverMetrics() {
        return exposeDriverMetrics;
    }


    public void setExposeDriverMetrics(Boolean exposeDriverMetrics) {
        this.exposeDriverMetrics = exposeDriverMetrics;
    }


    public V1beta2SparkApplicationSpecMonitoring exposeExecutorMetrics(Boolean exposeExecutorMetrics) {

        this.exposeExecutorMetrics = exposeExecutorMetrics;
        return this;
    }

    /**
     * Get exposeExecutorMetrics
     *
     * @return exposeExecutorMetrics
     **/
    @ApiModelProperty(required = true, value = "")

    public Boolean getExposeExecutorMetrics() {
        return exposeExecutorMetrics;
    }


    public void setExposeExecutorMetrics(Boolean exposeExecutorMetrics) {
        this.exposeExecutorMetrics = exposeExecutorMetrics;
    }


    public V1beta2SparkApplicationSpecMonitoring metricsProperties(String metricsProperties) {

        this.metricsProperties = metricsProperties;
        return this;
    }

    /**
     * Get metricsProperties
     *
     * @return metricsProperties
     **/
    @javax.annotation.Nullable
    @ApiModelProperty(value = "")

    public String getMetricsProperties() {
        return metricsProperties;
    }


    public void setMetricsProperties(String metricsProperties) {
        this.metricsProperties = metricsProperties;
    }


    public V1beta2SparkApplicationSpecMonitoring metricsPropertiesFile(String metricsPropertiesFile) {

        this.metricsPropertiesFile = metricsPropertiesFile;
        return this;
    }

    /**
     * Get metricsPropertiesFile
     *
     * @return metricsPropertiesFile
     **/
    @javax.annotation.Nullable
    @ApiModelProperty(value = "")

    public String getMetricsPropertiesFile() {
        return metricsPropertiesFile;
    }


    public void setMetricsPropertiesFile(String metricsPropertiesFile) {
        this.metricsPropertiesFile = metricsPropertiesFile;
    }


    public V1beta2SparkApplicationSpecMonitoring prometheus(V1beta2SparkApplicationSpecMonitoringPrometheus prometheus) {

        this.prometheus = prometheus;
        return this;
    }

    /**
     * Get prometheus
     *
     * @return prometheus
     **/
    @javax.annotation.Nullable
    @ApiModelProperty(value = "")

    public V1beta2SparkApplicationSpecMonitoringPrometheus getPrometheus() {
        return prometheus;
    }


    public void setPrometheus(V1beta2SparkApplicationSpecMonitoringPrometheus prometheus) {
        this.prometheus = prometheus;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        V1beta2SparkApplicationSpecMonitoring v1beta2SparkApplicationSpecMonitoring =
                (V1beta2SparkApplicationSpecMonitoring) o;
        return Objects.equals(this.exposeDriverMetrics, v1beta2SparkApplicationSpecMonitoring.exposeDriverMetrics) &&
                Objects.equals(this.exposeExecutorMetrics,
                        v1beta2SparkApplicationSpecMonitoring.exposeExecutorMetrics) &&
                Objects.equals(this.metricsProperties, v1beta2SparkApplicationSpecMonitoring.metricsProperties) &&
                Objects.equals(this.metricsPropertiesFile,
                        v1beta2SparkApplicationSpecMonitoring.metricsPropertiesFile) &&
                Objects.equals(this.prometheus, v1beta2SparkApplicationSpecMonitoring.prometheus);
    }

    @Override
    public int hashCode() {
        return Objects.hash(exposeDriverMetrics, exposeExecutorMetrics, metricsProperties, metricsPropertiesFile,
                prometheus);
    }


    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class V1beta2SparkApplicationSpecMonitoring {\n");
        sb.append("    exposeDriverMetrics: ").append(toIndentedString(exposeDriverMetrics)).append("\n");
        sb.append("    exposeExecutorMetrics: ").append(toIndentedString(exposeExecutorMetrics)).append("\n");
        sb.append("    metricsProperties: ").append(toIndentedString(metricsProperties)).append("\n");
        sb.append("    metricsPropertiesFile: ").append(toIndentedString(metricsPropertiesFile)).append("\n");
        sb.append("    prometheus: ").append(toIndentedString(prometheus)).append("\n");
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

