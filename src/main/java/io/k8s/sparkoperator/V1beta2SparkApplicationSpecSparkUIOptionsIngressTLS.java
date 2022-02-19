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
import java.util.List;
import java.util.Objects;

/**
 * V1beta2SparkApplicationSpecSparkUIOptionsIngressTLS
 */
@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaClientCodegen", date = "2022-01-30T13:32" +
        ":37.998Z[Etc/UTC]")
public class V1beta2SparkApplicationSpecSparkUIOptionsIngressTLS {

    private List<String> hosts = null;


    private String secretName;


    public V1beta2SparkApplicationSpecSparkUIOptionsIngressTLS hosts(List<String> hosts) {

        this.hosts = hosts;
        return this;
    }

    public V1beta2SparkApplicationSpecSparkUIOptionsIngressTLS addHostsItem(String hostsItem) {
        if (this.hosts == null) {
            this.hosts = new ArrayList<>();
        }
        this.hosts.add(hostsItem);
        return this;
    }

    /**
     * Get hosts
     *
     * @return hosts
     **/
    @javax.annotation.Nullable
    @ApiModelProperty(value = "")

    public List<String> getHosts() {
        return hosts;
    }


    public void setHosts(List<String> hosts) {
        this.hosts = hosts;
    }


    public V1beta2SparkApplicationSpecSparkUIOptionsIngressTLS secretName(String secretName) {

        this.secretName = secretName;
        return this;
    }

    /**
     * Get secretName
     *
     * @return secretName
     **/
    @javax.annotation.Nullable
    @ApiModelProperty(value = "")

    public String getSecretName() {
        return secretName;
    }


    public void setSecretName(String secretName) {
        this.secretName = secretName;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        V1beta2SparkApplicationSpecSparkUIOptionsIngressTLS v1beta2SparkApplicationSpecSparkUIOptionsIngressTLS =
                (V1beta2SparkApplicationSpecSparkUIOptionsIngressTLS) o;
        return Objects.equals(this.hosts, v1beta2SparkApplicationSpecSparkUIOptionsIngressTLS.hosts) &&
                Objects.equals(this.secretName, v1beta2SparkApplicationSpecSparkUIOptionsIngressTLS.secretName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(hosts, secretName);
    }


    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class V1beta2SparkApplicationSpecSparkUIOptionsIngressTLS {\n");
        sb.append("    hosts: ").append(toIndentedString(hosts)).append("\n");
        sb.append("    secretName: ").append(toIndentedString(secretName)).append("\n");
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

