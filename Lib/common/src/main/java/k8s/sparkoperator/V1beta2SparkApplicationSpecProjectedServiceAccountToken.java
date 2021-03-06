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
 * V1beta2SparkApplicationSpecProjectedServiceAccountToken
 */
@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaClientCodegen", date = "2022-01-30T13:32" +
        ":37.998Z[Etc/UTC]")
public class V1beta2SparkApplicationSpecProjectedServiceAccountToken {

    private String audience;


    private Long expirationSeconds;


    private String path;


    public V1beta2SparkApplicationSpecProjectedServiceAccountToken audience(String audience) {

        this.audience = audience;
        return this;
    }

    /**
     * Get audience
     *
     * @return audience
     **/
    @javax.annotation.Nullable
    @ApiModelProperty(value = "")

    public String getAudience() {
        return audience;
    }


    public void setAudience(String audience) {
        this.audience = audience;
    }


    public V1beta2SparkApplicationSpecProjectedServiceAccountToken expirationSeconds(Long expirationSeconds) {

        this.expirationSeconds = expirationSeconds;
        return this;
    }

    /**
     * Get expirationSeconds
     *
     * @return expirationSeconds
     **/
    @javax.annotation.Nullable
    @ApiModelProperty(value = "")

    public Long getExpirationSeconds() {
        return expirationSeconds;
    }


    public void setExpirationSeconds(Long expirationSeconds) {
        this.expirationSeconds = expirationSeconds;
    }


    public V1beta2SparkApplicationSpecProjectedServiceAccountToken path(String path) {

        this.path = path;
        return this;
    }

    /**
     * Get path
     *
     * @return path
     **/
    @ApiModelProperty(required = true, value = "")

    public String getPath() {
        return path;
    }


    public void setPath(String path) {
        this.path = path;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        V1beta2SparkApplicationSpecProjectedServiceAccountToken v1beta2SparkApplicationSpecProjectedServiceAccountToken = (V1beta2SparkApplicationSpecProjectedServiceAccountToken) o;
        return Objects.equals(this.audience, v1beta2SparkApplicationSpecProjectedServiceAccountToken.audience) &&
                Objects.equals(this.expirationSeconds,
                        v1beta2SparkApplicationSpecProjectedServiceAccountToken.expirationSeconds) &&
                Objects.equals(this.path, v1beta2SparkApplicationSpecProjectedServiceAccountToken.path);
    }

    @Override
    public int hashCode() {
        return Objects.hash(audience, expirationSeconds, path);
    }


    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class V1beta2SparkApplicationSpecProjectedServiceAccountToken {\n");
        sb.append("    audience: ").append(toIndentedString(audience)).append("\n");
        sb.append("    expirationSeconds: ").append(toIndentedString(expirationSeconds)).append("\n");
        sb.append("    path: ").append(toIndentedString(path)).append("\n");
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

