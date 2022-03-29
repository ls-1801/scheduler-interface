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
 * V1beta2SparkApplicationSpecDriverDnsConfigOptions
 */
@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaClientCodegen", date = "2022-01-30T13:32" +
        ":37.998Z[Etc/UTC]")
public class V1beta2SparkApplicationSpecDriverDnsConfigOptions {

    private String name;


    private String value;


    public V1beta2SparkApplicationSpecDriverDnsConfigOptions name(String name) {

        this.name = name;
        return this;
    }

    /**
     * Get name
     *
     * @return name
     **/
    @javax.annotation.Nullable
    @ApiModelProperty(value = "")

    public String getName() {
        return name;
    }


    public void setName(String name) {
        this.name = name;
    }


    public V1beta2SparkApplicationSpecDriverDnsConfigOptions value(String value) {

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


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        V1beta2SparkApplicationSpecDriverDnsConfigOptions v1beta2SparkApplicationSpecDriverDnsConfigOptions =
                (V1beta2SparkApplicationSpecDriverDnsConfigOptions) o;
        return Objects.equals(this.name, v1beta2SparkApplicationSpecDriverDnsConfigOptions.name) &&
                Objects.equals(this.value, v1beta2SparkApplicationSpecDriverDnsConfigOptions.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, value);
    }


    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class V1beta2SparkApplicationSpecDriverDnsConfigOptions {\n");
        sb.append("    name: ").append(toIndentedString(name)).append("\n");
        sb.append("    value: ").append(toIndentedString(value)).append("\n");
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
