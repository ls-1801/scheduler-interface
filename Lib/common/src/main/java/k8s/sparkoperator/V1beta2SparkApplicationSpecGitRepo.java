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
 * V1beta2SparkApplicationSpecGitRepo
 */
@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaClientCodegen", date = "2022-01-30T13:32" +
        ":37.998Z[Etc/UTC]")
public class V1beta2SparkApplicationSpecGitRepo {

    private String directory;


    private String repository;


    private String revision;


    public V1beta2SparkApplicationSpecGitRepo directory(String directory) {

        this.directory = directory;
        return this;
    }

    /**
     * Get directory
     *
     * @return directory
     **/
    @javax.annotation.Nullable
    @ApiModelProperty(value = "")

    public String getDirectory() {
        return directory;
    }


    public void setDirectory(String directory) {
        this.directory = directory;
    }


    public V1beta2SparkApplicationSpecGitRepo repository(String repository) {

        this.repository = repository;
        return this;
    }

    /**
     * Get repository
     *
     * @return repository
     **/
    @ApiModelProperty(required = true, value = "")

    public String getRepository() {
        return repository;
    }


    public void setRepository(String repository) {
        this.repository = repository;
    }


    public V1beta2SparkApplicationSpecGitRepo revision(String revision) {

        this.revision = revision;
        return this;
    }

    /**
     * Get revision
     *
     * @return revision
     **/
    @javax.annotation.Nullable
    @ApiModelProperty(value = "")

    public String getRevision() {
        return revision;
    }


    public void setRevision(String revision) {
        this.revision = revision;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        V1beta2SparkApplicationSpecGitRepo v1beta2SparkApplicationSpecGitRepo = (V1beta2SparkApplicationSpecGitRepo) o;
        return Objects.equals(this.directory, v1beta2SparkApplicationSpecGitRepo.directory) &&
                Objects.equals(this.repository, v1beta2SparkApplicationSpecGitRepo.repository) &&
                Objects.equals(this.revision, v1beta2SparkApplicationSpecGitRepo.revision);
    }

    @Override
    public int hashCode() {
        return Objects.hash(directory, repository, revision);
    }


    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class V1beta2SparkApplicationSpecGitRepo {\n");
        sb.append("    directory: ").append(toIndentedString(directory)).append("\n");
        sb.append("    repository: ").append(toIndentedString(repository)).append("\n");
        sb.append("    revision: ").append(toIndentedString(revision)).append("\n");
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

