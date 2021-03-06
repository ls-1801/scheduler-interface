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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * V1beta2SparkApplicationSpecSparkUIOptions
 */
@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaClientCodegen", date = "2022-01-30T13:32" +
        ":37.998Z[Etc/UTC]")
public class V1beta2SparkApplicationSpecSparkUIOptions {

    private Map<String, String> ingressAnnotations = null;


    private List<V1beta2SparkApplicationSpecSparkUIOptionsIngressTLS> ingressTLS = null;


    private Map<String, String> serviceAnnotations = null;


    private Integer servicePort;


    private String servicePortName;


    private String serviceType;


    public V1beta2SparkApplicationSpecSparkUIOptions ingressAnnotations(Map<String, String> ingressAnnotations) {

        this.ingressAnnotations = ingressAnnotations;
        return this;
    }

    public V1beta2SparkApplicationSpecSparkUIOptions putIngressAnnotationsItem(String key,
                                                                               String ingressAnnotationsItem) {
        if (this.ingressAnnotations == null) {
            this.ingressAnnotations = new HashMap<>();
        }
        this.ingressAnnotations.put(key, ingressAnnotationsItem);
        return this;
    }

    /**
     * Get ingressAnnotations
     *
     * @return ingressAnnotations
     **/
    @javax.annotation.Nullable
    @ApiModelProperty(value = "")

    public Map<String, String> getIngressAnnotations() {
        return ingressAnnotations;
    }


    public void setIngressAnnotations(Map<String, String> ingressAnnotations) {
        this.ingressAnnotations = ingressAnnotations;
    }


    public V1beta2SparkApplicationSpecSparkUIOptions ingressTLS(List<V1beta2SparkApplicationSpecSparkUIOptionsIngressTLS> ingressTLS) {

        this.ingressTLS = ingressTLS;
        return this;
    }

    public V1beta2SparkApplicationSpecSparkUIOptions addIngressTLSItem(V1beta2SparkApplicationSpecSparkUIOptionsIngressTLS ingressTLSItem) {
        if (this.ingressTLS == null) {
            this.ingressTLS = new ArrayList<>();
        }
        this.ingressTLS.add(ingressTLSItem);
        return this;
    }

    /**
     * Get ingressTLS
     *
     * @return ingressTLS
     **/
    @javax.annotation.Nullable
    @ApiModelProperty(value = "")

    public List<V1beta2SparkApplicationSpecSparkUIOptionsIngressTLS> getIngressTLS() {
        return ingressTLS;
    }


    public void setIngressTLS(List<V1beta2SparkApplicationSpecSparkUIOptionsIngressTLS> ingressTLS) {
        this.ingressTLS = ingressTLS;
    }


    public V1beta2SparkApplicationSpecSparkUIOptions serviceAnnotations(Map<String, String> serviceAnnotations) {

        this.serviceAnnotations = serviceAnnotations;
        return this;
    }

    public V1beta2SparkApplicationSpecSparkUIOptions putServiceAnnotationsItem(String key,
                                                                               String serviceAnnotationsItem) {
        if (this.serviceAnnotations == null) {
            this.serviceAnnotations = new HashMap<>();
        }
        this.serviceAnnotations.put(key, serviceAnnotationsItem);
        return this;
    }

    /**
     * Get serviceAnnotations
     *
     * @return serviceAnnotations
     **/
    @javax.annotation.Nullable
    @ApiModelProperty(value = "")

    public Map<String, String> getServiceAnnotations() {
        return serviceAnnotations;
    }


    public void setServiceAnnotations(Map<String, String> serviceAnnotations) {
        this.serviceAnnotations = serviceAnnotations;
    }


    public V1beta2SparkApplicationSpecSparkUIOptions servicePort(Integer servicePort) {

        this.servicePort = servicePort;
        return this;
    }

    /**
     * Get servicePort
     *
     * @return servicePort
     **/
    @javax.annotation.Nullable
    @ApiModelProperty(value = "")

    public Integer getServicePort() {
        return servicePort;
    }


    public void setServicePort(Integer servicePort) {
        this.servicePort = servicePort;
    }


    public V1beta2SparkApplicationSpecSparkUIOptions servicePortName(String servicePortName) {

        this.servicePortName = servicePortName;
        return this;
    }

    /**
     * Get servicePortName
     *
     * @return servicePortName
     **/
    @javax.annotation.Nullable
    @ApiModelProperty(value = "")

    public String getServicePortName() {
        return servicePortName;
    }


    public void setServicePortName(String servicePortName) {
        this.servicePortName = servicePortName;
    }


    public V1beta2SparkApplicationSpecSparkUIOptions serviceType(String serviceType) {

        this.serviceType = serviceType;
        return this;
    }

    /**
     * Get serviceType
     *
     * @return serviceType
     **/
    @javax.annotation.Nullable
    @ApiModelProperty(value = "")

    public String getServiceType() {
        return serviceType;
    }


    public void setServiceType(String serviceType) {
        this.serviceType = serviceType;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        V1beta2SparkApplicationSpecSparkUIOptions v1beta2SparkApplicationSpecSparkUIOptions =
                (V1beta2SparkApplicationSpecSparkUIOptions) o;
        return Objects.equals(this.ingressAnnotations, v1beta2SparkApplicationSpecSparkUIOptions.ingressAnnotations) &&
                Objects.equals(this.ingressTLS, v1beta2SparkApplicationSpecSparkUIOptions.ingressTLS) &&
                Objects.equals(this.serviceAnnotations, v1beta2SparkApplicationSpecSparkUIOptions.serviceAnnotations) &&
                Objects.equals(this.servicePort, v1beta2SparkApplicationSpecSparkUIOptions.servicePort) &&
                Objects.equals(this.servicePortName, v1beta2SparkApplicationSpecSparkUIOptions.servicePortName) &&
                Objects.equals(this.serviceType, v1beta2SparkApplicationSpecSparkUIOptions.serviceType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ingressAnnotations, ingressTLS, serviceAnnotations, servicePort, servicePortName,
                serviceType);
    }


    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class V1beta2SparkApplicationSpecSparkUIOptions {\n");
        sb.append("    ingressAnnotations: ").append(toIndentedString(ingressAnnotations)).append("\n");
        sb.append("    ingressTLS: ").append(toIndentedString(ingressTLS)).append("\n");
        sb.append("    serviceAnnotations: ").append(toIndentedString(serviceAnnotations)).append("\n");
        sb.append("    servicePort: ").append(toIndentedString(servicePort)).append("\n");
        sb.append("    servicePortName: ").append(toIndentedString(servicePortName)).append("\n");
        sb.append("    serviceType: ").append(toIndentedString(serviceType)).append("\n");
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

