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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * V1beta1FlinkClusterStatusComponentsJobManagerIngress
 */
@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaClientCodegen", date = "2022-02-19T16:59" +
        ":54.980Z[Etc/UTC]")
public class V1beta1FlinkClusterStatusComponentsJobManagerIngress {
    @JsonProperty("name")
    private String name;

    @JsonProperty("state")
    private String state;

    @JsonProperty("urls")
    private List<String> urls = null;


    public V1beta1FlinkClusterStatusComponentsJobManagerIngress name(String name) {

        this.name = name;
        return this;
    }

    /**
     * Get name
     *
     * @return name
     **/
    @ApiModelProperty(required = true, value = "")

    public String getName() {
        return name;
    }


    public void setName(String name) {
        this.name = name;
    }


    public V1beta1FlinkClusterStatusComponentsJobManagerIngress state(String state) {

        this.state = state;
        return this;
    }

    /**
     * Get state
     *
     * @return state
     **/
    @ApiModelProperty(required = true, value = "")

    public String getState() {
        return state;
    }


    public void setState(String state) {
        this.state = state;
    }


    public V1beta1FlinkClusterStatusComponentsJobManagerIngress urls(List<String> urls) {

        this.urls = urls;
        return this;
    }

    public V1beta1FlinkClusterStatusComponentsJobManagerIngress addUrlsItem(String urlsItem) {
        if (this.urls == null) {
            this.urls = new ArrayList<>();
        }
        this.urls.add(urlsItem);
        return this;
    }

    /**
     * Get urls
     *
     * @return urls
     **/
    @javax.annotation.Nullable
    @ApiModelProperty(value = "")

    public List<String> getUrls() {
        return urls;
    }


    public void setUrls(List<String> urls) {
        this.urls = urls;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        V1beta1FlinkClusterStatusComponentsJobManagerIngress v1beta1FlinkClusterStatusComponentsJobManagerIngress =
                (V1beta1FlinkClusterStatusComponentsJobManagerIngress) o;
        return Objects.equals(this.name, v1beta1FlinkClusterStatusComponentsJobManagerIngress.name) &&
                Objects.equals(this.state, v1beta1FlinkClusterStatusComponentsJobManagerIngress.state) &&
                Objects.equals(this.urls, v1beta1FlinkClusterStatusComponentsJobManagerIngress.urls);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, state, urls);
    }


    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class V1beta1FlinkClusterStatusComponentsJobManagerIngress {\n");
        sb.append("    name: ").append(toIndentedString(name)).append("\n");
        sb.append("    state: ").append(toIndentedString(state)).append("\n");
        sb.append("    urls: ").append(toIndentedString(urls)).append("\n");
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

