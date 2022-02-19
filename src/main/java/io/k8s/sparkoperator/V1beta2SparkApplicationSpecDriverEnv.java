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
 * V1beta2SparkApplicationSpecDriverEnv
 */
@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaClientCodegen", date = "2022-01-30T13:32" +
        ":37.998Z[Etc/UTC]")
public class V1beta2SparkApplicationSpecDriverEnv {

    private String name;


    private String value;


    private V1beta2SparkApplicationSpecDriverValueFrom valueFrom;


    public V1beta2SparkApplicationSpecDriverEnv name(String name) {

        this.name = name;
        return this;
    }

    /**
     * Get name
     *
     * @return name
     **/
    @ApiModelProperty(required = true, value = "")

    public String getName() {
        return name;
    }


    public void setName(String name) {
        this.name = name;
    }


    public V1beta2SparkApplicationSpecDriverEnv value(String value) {

        this.value = value;
        return this;
    }

    /**
     * Get value
     *
     * @return value
     **/
    @javax.annotation.Nullable
    @ApiModelProperty(value = "")

    public String getValue() {
        return value;
    }


    public void setValue(String value) {
        this.value = value;
    }


    public V1beta2SparkApplicationSpecDriverEnv valueFrom(V1beta2SparkApplicationSpecDriverValueFrom valueFrom) {

        this.valueFrom = valueFrom;
        return this;
    }

    /**
     * Get valueFrom
     *
     * @return valueFrom
     **/
    @javax.annotation.Nullable
    @ApiModelProperty(value = "")

    public V1beta2SparkApplicationSpecDriverValueFrom getValueFrom() {
        return valueFrom;
    }


    public void setValueFrom(V1beta2SparkApplicationSpecDriverValueFrom valueFrom) {
        this.valueFrom = valueFrom;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        V1beta2SparkApplicationSpecDriverEnv v1beta2SparkApplicationSpecDriverEnv =
                (V1beta2SparkApplicationSpecDriverEnv) o;
        return Objects.equals(this.name, v1beta2SparkApplicationSpecDriverEnv.name) &&
                Objects.equals(this.value, v1beta2SparkApplicationSpecDriverEnv.value) &&
                Objects.equals(this.valueFrom, v1beta2SparkApplicationSpecDriverEnv.valueFrom);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, value, valueFrom);
    }


    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class V1beta2SparkApplicationSpecDriverEnv {\n");
        sb.append("    name: ").append(toIndentedString(name)).append("\n");
        sb.append("    value: ").append(toIndentedString(value)).append("\n");
        sb.append("    valueFrom: ").append(toIndentedString(valueFrom)).append("\n");
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

