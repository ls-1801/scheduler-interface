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
 * V1beta2SparkApplicationSpecPortworxVolume
 */
@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaClientCodegen", date = "2022-01-30T13:32" +
        ":37.998Z[Etc/UTC]")
public class V1beta2SparkApplicationSpecPortworxVolume {

    private String fsType;


    private Boolean readOnly;


    private String volumeID;


    public V1beta2SparkApplicationSpecPortworxVolume fsType(String fsType) {

        this.fsType = fsType;
        return this;
    }

    /**
     * Get fsType
     *
     * @return fsType
     **/
    @javax.annotation.Nullable
    @ApiModelProperty(value = "")

    public String getFsType() {
        return fsType;
    }


    public void setFsType(String fsType) {
        this.fsType = fsType;
    }


    public V1beta2SparkApplicationSpecPortworxVolume readOnly(Boolean readOnly) {

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


    public V1beta2SparkApplicationSpecPortworxVolume volumeID(String volumeID) {

        this.volumeID = volumeID;
        return this;
    }

    /**
     * Get volumeID
     *
     * @return volumeID
     **/
    @ApiModelProperty(required = true, value = "")

    public String getVolumeID() {
        return volumeID;
    }


    public void setVolumeID(String volumeID) {
        this.volumeID = volumeID;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        V1beta2SparkApplicationSpecPortworxVolume v1beta2SparkApplicationSpecPortworxVolume =
                (V1beta2SparkApplicationSpecPortworxVolume) o;
        return Objects.equals(this.fsType, v1beta2SparkApplicationSpecPortworxVolume.fsType) &&
                Objects.equals(this.readOnly, v1beta2SparkApplicationSpecPortworxVolume.readOnly) &&
                Objects.equals(this.volumeID, v1beta2SparkApplicationSpecPortworxVolume.volumeID);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fsType, readOnly, volumeID);
    }


    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class V1beta2SparkApplicationSpecPortworxVolume {\n");
        sb.append("    fsType: ").append(toIndentedString(fsType)).append("\n");
        sb.append("    readOnly: ").append(toIndentedString(readOnly)).append("\n");
        sb.append("    volumeID: ").append(toIndentedString(volumeID)).append("\n");
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

