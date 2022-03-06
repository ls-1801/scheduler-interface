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
 * V1beta2SparkApplicationSpecDriverSecurityContextCapabilities
 */
@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaClientCodegen", date = "2022-01-30T13:32" +
        ":37.998Z[Etc/UTC]")
public class V1beta2SparkApplicationSpecDriverSecurityContextCapabilities {

    private List<String> add = null;


    private List<String> drop = null;


    public V1beta2SparkApplicationSpecDriverSecurityContextCapabilities add(List<String> add) {

        this.add = add;
        return this;
    }

    public V1beta2SparkApplicationSpecDriverSecurityContextCapabilities addAddItem(String addItem) {
        if (this.add == null) {
            this.add = new ArrayList<>();
        }
        this.add.add(addItem);
        return this;
    }

    /**
     * Get add
     *
     * @return add
     **/
    @javax.annotation.Nullable
    @ApiModelProperty(value = "")

    public List<String> getAdd() {
        return add;
    }


    public void setAdd(List<String> add) {
        this.add = add;
    }


    public V1beta2SparkApplicationSpecDriverSecurityContextCapabilities drop(List<String> drop) {

        this.drop = drop;
        return this;
    }

    public V1beta2SparkApplicationSpecDriverSecurityContextCapabilities addDropItem(String dropItem) {
        if (this.drop == null) {
            this.drop = new ArrayList<>();
        }
        this.drop.add(dropItem);
        return this;
    }

    /**
     * Get drop
     *
     * @return drop
     **/
    @javax.annotation.Nullable
    @ApiModelProperty(value = "")

    public List<String> getDrop() {
        return drop;
    }


    public void setDrop(List<String> drop) {
        this.drop = drop;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        V1beta2SparkApplicationSpecDriverSecurityContextCapabilities v1beta2SparkApplicationSpecDriverSecurityContextCapabilities = (V1beta2SparkApplicationSpecDriverSecurityContextCapabilities) o;
        return Objects.equals(this.add, v1beta2SparkApplicationSpecDriverSecurityContextCapabilities.add) &&
                Objects.equals(this.drop, v1beta2SparkApplicationSpecDriverSecurityContextCapabilities.drop);
    }

    @Override
    public int hashCode() {
        return Objects.hash(add, drop);
    }


    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class V1beta2SparkApplicationSpecDriverSecurityContextCapabilities {\n");
        sb.append("    add: ").append(toIndentedString(add)).append("\n");
        sb.append("    drop: ").append(toIndentedString(drop)).append("\n");
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
