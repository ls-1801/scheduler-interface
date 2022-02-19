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


package io.k8s.flinkoperator;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;

import java.util.Objects;

/**
 * V1beta1FlinkClusterSpecJobManagerPorts
 */
@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaClientCodegen", date = "2022-02-19T16:59" +
        ":54.980Z[Etc/UTC]")
public class V1beta1FlinkClusterSpecJobManagerPorts {
    @JsonProperty("blob")
    private Integer blob;

    @JsonProperty("query")
    private Integer query;

    @JsonProperty("rpc")
    private Integer rpc;

    @JsonProperty("ui")
    private Integer ui;


    public V1beta1FlinkClusterSpecJobManagerPorts blob(Integer blob) {

        this.blob = blob;
        return this;
    }

    /**
     * Get blob
     *
     * @return blob
     **/
    @javax.annotation.Nullable
    @ApiModelProperty(value = "")

    public Integer getBlob() {
        return blob;
    }


    public void setBlob(Integer blob) {
        this.blob = blob;
    }


    public V1beta1FlinkClusterSpecJobManagerPorts query(Integer query) {

        this.query = query;
        return this;
    }

    /**
     * Get query
     *
     * @return query
     **/
    @javax.annotation.Nullable
    @ApiModelProperty(value = "")

    public Integer getQuery() {
        return query;
    }


    public void setQuery(Integer query) {
        this.query = query;
    }


    public V1beta1FlinkClusterSpecJobManagerPorts rpc(Integer rpc) {

        this.rpc = rpc;
        return this;
    }

    /**
     * Get rpc
     *
     * @return rpc
     **/
    @javax.annotation.Nullable
    @ApiModelProperty(value = "")

    public Integer getRpc() {
        return rpc;
    }


    public void setRpc(Integer rpc) {
        this.rpc = rpc;
    }


    public V1beta1FlinkClusterSpecJobManagerPorts ui(Integer ui) {

        this.ui = ui;
        return this;
    }

    /**
     * Get ui
     *
     * @return ui
     **/
    @javax.annotation.Nullable
    @ApiModelProperty(value = "")

    public Integer getUi() {
        return ui;
    }


    public void setUi(Integer ui) {
        this.ui = ui;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        V1beta1FlinkClusterSpecJobManagerPorts v1beta1FlinkClusterSpecJobManagerPorts =
                (V1beta1FlinkClusterSpecJobManagerPorts) o;
        return Objects.equals(this.blob, v1beta1FlinkClusterSpecJobManagerPorts.blob) &&
                Objects.equals(this.query, v1beta1FlinkClusterSpecJobManagerPorts.query) &&
                Objects.equals(this.rpc, v1beta1FlinkClusterSpecJobManagerPorts.rpc) &&
                Objects.equals(this.ui, v1beta1FlinkClusterSpecJobManagerPorts.ui);
    }

    @Override
    public int hashCode() {
        return Objects.hash(blob, query, rpc, ui);
    }


    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class V1beta1FlinkClusterSpecJobManagerPorts {\n");
        sb.append("    blob: ").append(toIndentedString(blob)).append("\n");
        sb.append("    query: ").append(toIndentedString(query)).append("\n");
        sb.append("    rpc: ").append(toIndentedString(rpc)).append("\n");
        sb.append("    ui: ").append(toIndentedString(ui)).append("\n");
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
