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
 * V1beta2SparkApplicationSpecDriverValueFromFieldRef
 */
@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaClientCodegen", date = "2022-01-30T13:32" +
        ":37.998Z[Etc/UTC]")
public class V1beta2SparkApplicationSpecDriverValueFromFieldRef {

    private String apiVersion;


    private String fieldPath;


    public V1beta2SparkApplicationSpecDriverValueFromFieldRef apiVersion(String apiVersion) {

        this.apiVersion = apiVersion;
        return this;
    }

    /**
     * Get apiVersion
     *
     * @return apiVersion
     **/
    @javax.annotation.Nullable
    @ApiModelProperty(value = "")

    public String getApiVersion() {
        return apiVersion;
    }


    public void setApiVersion(String apiVersion) {
        this.apiVersion = apiVersion;
    }


    public V1beta2SparkApplicationSpecDriverValueFromFieldRef fieldPath(String fieldPath) {

        this.fieldPath = fieldPath;
        return this;
    }

    /**
     * Get fieldPath
     *
     * @return fieldPath
     **/
    @ApiModelProperty(required = true, value = "")

    public String getFieldPath() {
        return fieldPath;
    }


    public void setFieldPath(String fieldPath) {
        this.fieldPath = fieldPath;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        V1beta2SparkApplicationSpecDriverValueFromFieldRef v1beta2SparkApplicationSpecDriverValueFromFieldRef =
                (V1beta2SparkApplicationSpecDriverValueFromFieldRef) o;
        return Objects.equals(this.apiVersion, v1beta2SparkApplicationSpecDriverValueFromFieldRef.apiVersion) &&
                Objects.equals(this.fieldPath, v1beta2SparkApplicationSpecDriverValueFromFieldRef.fieldPath);
    }

    @Override
    public int hashCode() {
        return Objects.hash(apiVersion, fieldPath);
    }


    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class V1beta2SparkApplicationSpecDriverValueFromFieldRef {\n");
        sb.append("    apiVersion: ").append(toIndentedString(apiVersion)).append("\n");
        sb.append("    fieldPath: ").append(toIndentedString(fieldPath)).append("\n");
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

