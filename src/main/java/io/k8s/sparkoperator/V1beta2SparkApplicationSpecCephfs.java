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
 * V1beta2SparkApplicationSpecCephfs
 */
@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaClientCodegen", date = "2022-01-30T13:32" +
        ":37.998Z[Etc/UTC]")
public class V1beta2SparkApplicationSpecCephfs {

    private List<String> monitors = new ArrayList<>();


    private String path;


    private Boolean readOnly;


    private String secretFile;


    private V1beta2SparkApplicationSpecCephfsSecretRef secretRef;


    private String user;


    public V1beta2SparkApplicationSpecCephfs monitors(List<String> monitors) {

        this.monitors = monitors;
        return this;
    }

    public V1beta2SparkApplicationSpecCephfs addMonitorsItem(String monitorsItem) {
        this.monitors.add(monitorsItem);
        return this;
    }

    /**
     * Get monitors
     *
     * @return monitors
     **/
    @ApiModelProperty(required = true, value = "")

    public List<String> getMonitors() {
        return monitors;
    }


    public void setMonitors(List<String> monitors) {
        this.monitors = monitors;
    }


    public V1beta2SparkApplicationSpecCephfs path(String path) {

        this.path = path;
        return this;
    }

    /**
     * Get path
     *
     * @return path
     **/
    @javax.annotation.Nullable
    @ApiModelProperty(value = "")

    public String getPath() {
        return path;
    }


    public void setPath(String path) {
        this.path = path;
    }


    public V1beta2SparkApplicationSpecCephfs readOnly(Boolean readOnly) {

        this.readOnly = readOnly;
        return this;
    }

    /**
     * Get readOnly
     *
     * @return readOnly
     **/
    @javax.annotation.Nullable
    @ApiModelProperty(value = "")

    public Boolean getReadOnly() {
        return readOnly;
    }


    public void setReadOnly(Boolean readOnly) {
        this.readOnly = readOnly;
    }


    public V1beta2SparkApplicationSpecCephfs secretFile(String secretFile) {

        this.secretFile = secretFile;
        return this;
    }

    /**
     * Get secretFile
     *
     * @return secretFile
     **/
    @javax.annotation.Nullable
    @ApiModelProperty(value = "")

    public String getSecretFile() {
        return secretFile;
    }


    public void setSecretFile(String secretFile) {
        this.secretFile = secretFile;
    }


    public V1beta2SparkApplicationSpecCephfs secretRef(V1beta2SparkApplicationSpecCephfsSecretRef secretRef) {

        this.secretRef = secretRef;
        return this;
    }

    /**
     * Get secretRef
     *
     * @return secretRef
     **/
    @javax.annotation.Nullable
    @ApiModelProperty(value = "")

    public V1beta2SparkApplicationSpecCephfsSecretRef getSecretRef() {
        return secretRef;
    }


    public void setSecretRef(V1beta2SparkApplicationSpecCephfsSecretRef secretRef) {
        this.secretRef = secretRef;
    }


    public V1beta2SparkApplicationSpecCephfs user(String user) {

        this.user = user;
        return this;
    }

    /**
     * Get user
     *
     * @return user
     **/
    @javax.annotation.Nullable
    @ApiModelProperty(value = "")

    public String getUser() {
        return user;
    }


    public void setUser(String user) {
        this.user = user;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        V1beta2SparkApplicationSpecCephfs v1beta2SparkApplicationSpecCephfs = (V1beta2SparkApplicationSpecCephfs) o;
        return Objects.equals(this.monitors, v1beta2SparkApplicationSpecCephfs.monitors) &&
                Objects.equals(this.path, v1beta2SparkApplicationSpecCephfs.path) &&
                Objects.equals(this.readOnly, v1beta2SparkApplicationSpecCephfs.readOnly) &&
                Objects.equals(this.secretFile, v1beta2SparkApplicationSpecCephfs.secretFile) &&
                Objects.equals(this.secretRef, v1beta2SparkApplicationSpecCephfs.secretRef) &&
                Objects.equals(this.user, v1beta2SparkApplicationSpecCephfs.user);
    }

    @Override
    public int hashCode() {
        return Objects.hash(monitors, path, readOnly, secretFile, secretRef, user);
    }


    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class V1beta2SparkApplicationSpecCephfs {\n");
        sb.append("    monitors: ").append(toIndentedString(monitors)).append("\n");
        sb.append("    path: ").append(toIndentedString(path)).append("\n");
        sb.append("    readOnly: ").append(toIndentedString(readOnly)).append("\n");
        sb.append("    secretFile: ").append(toIndentedString(secretFile)).append("\n");
        sb.append("    secretRef: ").append(toIndentedString(secretRef)).append("\n");
        sb.append("    user: ").append(toIndentedString(user)).append("\n");
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

