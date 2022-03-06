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


package k8s.flinkoperator;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;

import java.util.Objects;

/**
 * V1beta1FlinkClusterSpecHadoopConfig
 */
@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaClientCodegen", date = "2022-02-19T16:59" +
        ":54.980Z[Etc/UTC]")
public class V1beta1FlinkClusterSpecHadoopConfig {
    @JsonProperty("configMapName")
    private String configMapName;

    @JsonProperty("mountPath")
    private String mountPath;


    public V1beta1FlinkClusterSpecHadoopConfig configMapName(String configMapName) {

        this.configMapName = configMapName;
        return this;
    }

    /**
     * Get configMapName
     *
     * @return configMapName
     **/
    @javax.annotation.Nullable
    @ApiModelProperty(value = "")

    public String getConfigMapName() {
        return configMapName;
    }


    public void setConfigMapName(String configMapName) {
        this.configMapName = configMapName;
    }


    public V1beta1FlinkClusterSpecHadoopConfig mountPath(String mountPath) {

        this.mountPath = mountPath;
        return this;
    }

    /**
     * Get mountPath
     *
     * @return mountPath
     **/
    @javax.annotation.Nullable
    @ApiModelProperty(value = "")

    public String getMountPath() {
        return mountPath;
    }


    public void setMountPath(String mountPath) {
        this.mountPath = mountPath;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        V1beta1FlinkClusterSpecHadoopConfig v1beta1FlinkClusterSpecHadoopConfig =
                (V1beta1FlinkClusterSpecHadoopConfig) o;
        return Objects.equals(this.configMapName, v1beta1FlinkClusterSpecHadoopConfig.configMapName) &&
                Objects.equals(this.mountPath, v1beta1FlinkClusterSpecHadoopConfig.mountPath);
    }

    @Override
    public int hashCode() {
        return Objects.hash(configMapName, mountPath);
    }


    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class V1beta1FlinkClusterSpecHadoopConfig {\n");
        sb.append("    configMapName: ").append(toIndentedString(configMapName)).append("\n");
        sb.append("    mountPath: ").append(toIndentedString(mountPath)).append("\n");
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
