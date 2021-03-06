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
 * V1beta2SparkApplicationSpecAzureDisk
 */
@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaClientCodegen", date = "2022-01-30T13:32" +
        ":37.998Z[Etc/UTC]")
public class V1beta2SparkApplicationSpecAzureDisk {

    private String cachingMode;


    private String diskName;


    private String diskURI;


    private String fsType;


    private String kind;


    private Boolean readOnly;


    public V1beta2SparkApplicationSpecAzureDisk cachingMode(String cachingMode) {

        this.cachingMode = cachingMode;
        return this;
    }

    /**
     * Get cachingMode
     *
     * @return cachingMode
     **/
    @javax.annotation.Nullable
    @ApiModelProperty(value = "")

    public String getCachingMode() {
        return cachingMode;
    }


    public void setCachingMode(String cachingMode) {
        this.cachingMode = cachingMode;
    }


    public V1beta2SparkApplicationSpecAzureDisk diskName(String diskName) {

        this.diskName = diskName;
        return this;
    }

    /**
     * Get diskName
     *
     * @return diskName
     **/
    @ApiModelProperty(required = true, value = "")

    public String getDiskName() {
        return diskName;
    }


    public void setDiskName(String diskName) {
        this.diskName = diskName;
    }


    public V1beta2SparkApplicationSpecAzureDisk diskURI(String diskURI) {

        this.diskURI = diskURI;
        return this;
    }

    /**
     * Get diskURI
     *
     * @return diskURI
     **/
    @ApiModelProperty(required = true, value = "")

    public String getDiskURI() {
        return diskURI;
    }


    public void setDiskURI(String diskURI) {
        this.diskURI = diskURI;
    }


    public V1beta2SparkApplicationSpecAzureDisk fsType(String fsType) {

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


    public V1beta2SparkApplicationSpecAzureDisk kind(String kind) {

        this.kind = kind;
        return this;
    }

    /**
     * Get kind
     *
     * @return kind
     **/
    @javax.annotation.Nullable
    @ApiModelProperty(value = "")

    public String getKind() {
        return kind;
    }


    public void setKind(String kind) {
        this.kind = kind;
    }


    public V1beta2SparkApplicationSpecAzureDisk readOnly(Boolean readOnly) {

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


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        V1beta2SparkApplicationSpecAzureDisk v1beta2SparkApplicationSpecAzureDisk =
                (V1beta2SparkApplicationSpecAzureDisk) o;
        return Objects.equals(this.cachingMode, v1beta2SparkApplicationSpecAzureDisk.cachingMode) &&
                Objects.equals(this.diskName, v1beta2SparkApplicationSpecAzureDisk.diskName) &&
                Objects.equals(this.diskURI, v1beta2SparkApplicationSpecAzureDisk.diskURI) &&
                Objects.equals(this.fsType, v1beta2SparkApplicationSpecAzureDisk.fsType) &&
                Objects.equals(this.kind, v1beta2SparkApplicationSpecAzureDisk.kind) &&
                Objects.equals(this.readOnly, v1beta2SparkApplicationSpecAzureDisk.readOnly);
    }

    @Override
    public int hashCode() {
        return Objects.hash(cachingMode, diskName, diskURI, fsType, kind, readOnly);
    }


    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class V1beta2SparkApplicationSpecAzureDisk {\n");
        sb.append("    cachingMode: ").append(toIndentedString(cachingMode)).append("\n");
        sb.append("    diskName: ").append(toIndentedString(diskName)).append("\n");
        sb.append("    diskURI: ").append(toIndentedString(diskURI)).append("\n");
        sb.append("    fsType: ").append(toIndentedString(fsType)).append("\n");
        sb.append("    kind: ").append(toIndentedString(kind)).append("\n");
        sb.append("    readOnly: ").append(toIndentedString(readOnly)).append("\n");
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

