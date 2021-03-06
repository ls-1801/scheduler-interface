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
 * V1beta2SparkApplicationSpecPhotonPersistentDisk
 */
@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaClientCodegen", date = "2022-01-30T13:32" +
        ":37.998Z[Etc/UTC]")
public class V1beta2SparkApplicationSpecPhotonPersistentDisk {

    private String fsType;


    private String pdID;


    public V1beta2SparkApplicationSpecPhotonPersistentDisk fsType(String fsType) {

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


    public V1beta2SparkApplicationSpecPhotonPersistentDisk pdID(String pdID) {

        this.pdID = pdID;
        return this;
    }

    /**
     * Get pdID
     *
     * @return pdID
     **/
    @ApiModelProperty(required = true, value = "")

    public String getPdID() {
        return pdID;
    }


    public void setPdID(String pdID) {
        this.pdID = pdID;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        V1beta2SparkApplicationSpecPhotonPersistentDisk v1beta2SparkApplicationSpecPhotonPersistentDisk =
                (V1beta2SparkApplicationSpecPhotonPersistentDisk) o;
        return Objects.equals(this.fsType, v1beta2SparkApplicationSpecPhotonPersistentDisk.fsType) &&
                Objects.equals(this.pdID, v1beta2SparkApplicationSpecPhotonPersistentDisk.pdID);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fsType, pdID);
    }


    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class V1beta2SparkApplicationSpecPhotonPersistentDisk {\n");
        sb.append("    fsType: ").append(toIndentedString(fsType)).append("\n");
        sb.append("    pdID: ").append(toIndentedString(pdID)).append("\n");
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

