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


import io.fabric8.kubernetes.api.model.Affinity;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.PodDNSConfig;
import io.fabric8.kubernetes.api.model.PodSecurityContext;
import io.fabric8.kubernetes.api.model.Toleration;
import io.fabric8.kubernetes.api.model.VolumeMount;
import io.swagger.annotations.ApiModelProperty;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * V1beta2SparkApplicationSpecExecutor
 */
@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaClientCodegen", date = "2022-01-30T13:32" +
        ":37.998Z[Etc/UTC]")
public class V1beta2SparkApplicationSpecExecutor {

    private Affinity affinity;


    private Map<String, String> annotations = null;


    private List<V1beta2SparkApplicationSpecDriverConfigMaps> configMaps = null;


    private String coreLimit;


    private String coreRequest;


    private Integer cores;


    private Boolean deleteOnTermination;


    private PodDNSConfig dnsConfig;


    private List<V1beta2SparkApplicationSpecDriverEnv> env = null;


    private List<V1beta2SparkApplicationSpecDriverEnvFrom> envFrom = null;


    private Map<String, V1beta2SparkApplicationSpecDriverEnvSecretKeyRefs> envSecretKeyRefs = null;


    private Map<String, String> envVars = null;


    private V1beta2SparkApplicationSpecDriverGpu gpu;


    private List<V1beta2SparkApplicationSpecDriverHostAliases> hostAliases = null;


    private Boolean hostNetwork;


    private String image;


    private List<Container> initContainers = null;


    private Integer instances;


    private String javaOptions;


    private Map<String, String> labels = null;


    private String memory;


    private String memoryOverhead;


    private Map<String, String> nodeSelector = null;


    private String schedulerName;


    private List<V1beta2SparkApplicationSpecDriverSecrets> secrets = null;


    private PodSecurityContext securityContext;


    private String serviceAccount;


    private Boolean shareProcessNamespace;


    private List<Container> sidecars = null;


    private Long terminationGracePeriodSeconds;


    private List<Toleration> tolerations = null;


    private List<VolumeMount> volumeMounts = null;


    public V1beta2SparkApplicationSpecExecutor affinity(Affinity affinity) {

        this.affinity = affinity;
        return this;
    }

    /**
     * Get affinity
     *
     * @return affinity
     **/
    @javax.annotation.Nullable
    @ApiModelProperty(value = "")

    public Affinity getAffinity() {
        return affinity;
    }


    public void setAffinity(Affinity affinity) {
        this.affinity = affinity;
    }


    public V1beta2SparkApplicationSpecExecutor annotations(Map<String, String> annotations) {

        this.annotations = annotations;
        return this;
    }

    public V1beta2SparkApplicationSpecExecutor putAnnotationsItem(String key, String annotationsItem) {
        if (this.annotations == null) {
            this.annotations = new HashMap<>();
        }
        this.annotations.put(key, annotationsItem);
        return this;
    }

    /**
     * Get annotations
     *
     * @return annotations
     **/
    @ApiModelProperty(value = "")
    @Nonnull
    public Map<String, String> getAnnotations() {
        if (annotations == null) {
            annotations = new HashMap<>();
        }

        return annotations;
    }


    public void setAnnotations(Map<String, String> annotations) {
        this.annotations = annotations;
    }


    public V1beta2SparkApplicationSpecExecutor configMaps(List<V1beta2SparkApplicationSpecDriverConfigMaps> configMaps) {

        this.configMaps = configMaps;
        return this;
    }

    public V1beta2SparkApplicationSpecExecutor addConfigMapsItem(V1beta2SparkApplicationSpecDriverConfigMaps configMapsItem) {
        if (this.configMaps == null) {
            this.configMaps = new ArrayList<>();
        }
        this.configMaps.add(configMapsItem);
        return this;
    }

    /**
     * Get configMaps
     *
     * @return configMaps
     **/
    @javax.annotation.Nullable
    @ApiModelProperty(value = "")

    public List<V1beta2SparkApplicationSpecDriverConfigMaps> getConfigMaps() {
        return configMaps;
    }


    public void setConfigMaps(List<V1beta2SparkApplicationSpecDriverConfigMaps> configMaps) {
        this.configMaps = configMaps;
    }


    public V1beta2SparkApplicationSpecExecutor coreLimit(String coreLimit) {

        this.coreLimit = coreLimit;
        return this;
    }

    /**
     * Get coreLimit
     *
     * @return coreLimit
     **/
    @javax.annotation.Nullable
    @ApiModelProperty(value = "")

    public String getCoreLimit() {
        return coreLimit;
    }


    public void setCoreLimit(String coreLimit) {
        this.coreLimit = coreLimit;
    }


    public V1beta2SparkApplicationSpecExecutor coreRequest(String coreRequest) {

        this.coreRequest = coreRequest;
        return this;
    }

    /**
     * Get coreRequest
     *
     * @return coreRequest
     **/
    @javax.annotation.Nullable
    @ApiModelProperty(value = "")

    public String getCoreRequest() {
        return coreRequest;
    }


    public void setCoreRequest(String coreRequest) {
        this.coreRequest = coreRequest;
    }


    public V1beta2SparkApplicationSpecExecutor cores(Integer cores) {

        this.cores = cores;
        return this;
    }

    /**
     * Get cores
     * minimum: 1
     *
     * @return cores
     **/
    @javax.annotation.Nullable
    @ApiModelProperty(value = "")

    public Integer getCores() {
        return cores;
    }


    public void setCores(Integer cores) {
        this.cores = cores;
    }


    public V1beta2SparkApplicationSpecExecutor deleteOnTermination(Boolean deleteOnTermination) {

        this.deleteOnTermination = deleteOnTermination;
        return this;
    }

    /**
     * Get deleteOnTermination
     *
     * @return deleteOnTermination
     **/
    @javax.annotation.Nullable
    @ApiModelProperty(value = "")

    public Boolean getDeleteOnTermination() {
        return deleteOnTermination;
    }


    public void setDeleteOnTermination(Boolean deleteOnTermination) {
        this.deleteOnTermination = deleteOnTermination;
    }


    public V1beta2SparkApplicationSpecExecutor dnsConfig(PodDNSConfig dnsConfig) {

        this.dnsConfig = dnsConfig;
        return this;
    }

    /**
     * Get dnsConfig
     *
     * @return dnsConfig
     **/
    @javax.annotation.Nullable
    @ApiModelProperty(value = "")

    public PodDNSConfig getDnsConfig() {
        return dnsConfig;
    }


    public void setDnsConfig(PodDNSConfig dnsConfig) {
        this.dnsConfig = dnsConfig;
    }


    public V1beta2SparkApplicationSpecExecutor env(List<V1beta2SparkApplicationSpecDriverEnv> env) {

        this.env = env;
        return this;
    }

    public V1beta2SparkApplicationSpecExecutor addEnvItem(V1beta2SparkApplicationSpecDriverEnv envItem) {
        if (this.env == null) {
            this.env = new ArrayList<>();
        }
        this.env.add(envItem);
        return this;
    }

    /**
     * Get env
     *
     * @return env
     **/
    @javax.annotation.Nullable
    @ApiModelProperty(value = "")

    public List<V1beta2SparkApplicationSpecDriverEnv> getEnv() {
        return env;
    }


    public void setEnv(List<V1beta2SparkApplicationSpecDriverEnv> env) {
        this.env = env;
    }


    public V1beta2SparkApplicationSpecExecutor envFrom(List<V1beta2SparkApplicationSpecDriverEnvFrom> envFrom) {

        this.envFrom = envFrom;
        return this;
    }

    public V1beta2SparkApplicationSpecExecutor addEnvFromItem(V1beta2SparkApplicationSpecDriverEnvFrom envFromItem) {
        if (this.envFrom == null) {
            this.envFrom = new ArrayList<>();
        }
        this.envFrom.add(envFromItem);
        return this;
    }

    /**
     * Get envFrom
     *
     * @return envFrom
     **/
    @javax.annotation.Nullable
    @ApiModelProperty(value = "")

    public List<V1beta2SparkApplicationSpecDriverEnvFrom> getEnvFrom() {
        return envFrom;
    }


    public void setEnvFrom(List<V1beta2SparkApplicationSpecDriverEnvFrom> envFrom) {
        this.envFrom = envFrom;
    }


    public V1beta2SparkApplicationSpecExecutor envSecretKeyRefs(Map<String,
            V1beta2SparkApplicationSpecDriverEnvSecretKeyRefs> envSecretKeyRefs) {

        this.envSecretKeyRefs = envSecretKeyRefs;
        return this;
    }

    public V1beta2SparkApplicationSpecExecutor putEnvSecretKeyRefsItem(String key,
                                                                       V1beta2SparkApplicationSpecDriverEnvSecretKeyRefs envSecretKeyRefsItem) {
        if (this.envSecretKeyRefs == null) {
            this.envSecretKeyRefs = new HashMap<>();
        }
        this.envSecretKeyRefs.put(key, envSecretKeyRefsItem);
        return this;
    }

    /**
     * Get envSecretKeyRefs
     *
     * @return envSecretKeyRefs
     **/
    @javax.annotation.Nullable
    @ApiModelProperty(value = "")

    public Map<String, V1beta2SparkApplicationSpecDriverEnvSecretKeyRefs> getEnvSecretKeyRefs() {
        return envSecretKeyRefs;
    }


    public void setEnvSecretKeyRefs(Map<String, V1beta2SparkApplicationSpecDriverEnvSecretKeyRefs> envSecretKeyRefs) {
        this.envSecretKeyRefs = envSecretKeyRefs;
    }


    public V1beta2SparkApplicationSpecExecutor envVars(Map<String, String> envVars) {

        this.envVars = envVars;
        return this;
    }

    public V1beta2SparkApplicationSpecExecutor putEnvVarsItem(String key, String envVarsItem) {
        if (this.envVars == null) {
            this.envVars = new HashMap<>();
        }
        this.envVars.put(key, envVarsItem);
        return this;
    }

    /**
     * Get envVars
     *
     * @return envVars
     **/
    @javax.annotation.Nullable
    @ApiModelProperty(value = "")

    public Map<String, String> getEnvVars() {
        return envVars;
    }


    public void setEnvVars(Map<String, String> envVars) {
        this.envVars = envVars;
    }


    public V1beta2SparkApplicationSpecExecutor gpu(V1beta2SparkApplicationSpecDriverGpu gpu) {

        this.gpu = gpu;
        return this;
    }

    /**
     * Get gpu
     *
     * @return gpu
     **/
    @javax.annotation.Nullable
    @ApiModelProperty(value = "")

    public V1beta2SparkApplicationSpecDriverGpu getGpu() {
        return gpu;
    }


    public void setGpu(V1beta2SparkApplicationSpecDriverGpu gpu) {
        this.gpu = gpu;
    }


    public V1beta2SparkApplicationSpecExecutor hostAliases(List<V1beta2SparkApplicationSpecDriverHostAliases> hostAliases) {

        this.hostAliases = hostAliases;
        return this;
    }

    public V1beta2SparkApplicationSpecExecutor addHostAliasesItem(V1beta2SparkApplicationSpecDriverHostAliases hostAliasesItem) {
        if (this.hostAliases == null) {
            this.hostAliases = new ArrayList<>();
        }
        this.hostAliases.add(hostAliasesItem);
        return this;
    }

    /**
     * Get hostAliases
     *
     * @return hostAliases
     **/
    @javax.annotation.Nullable
    @ApiModelProperty(value = "")

    public List<V1beta2SparkApplicationSpecDriverHostAliases> getHostAliases() {
        return hostAliases;
    }


    public void setHostAliases(List<V1beta2SparkApplicationSpecDriverHostAliases> hostAliases) {
        this.hostAliases = hostAliases;
    }


    public V1beta2SparkApplicationSpecExecutor hostNetwork(Boolean hostNetwork) {

        this.hostNetwork = hostNetwork;
        return this;
    }

    /**
     * Get hostNetwork
     *
     * @return hostNetwork
     **/
    @javax.annotation.Nullable
    @ApiModelProperty(value = "")

    public Boolean getHostNetwork() {
        return hostNetwork;
    }


    public void setHostNetwork(Boolean hostNetwork) {
        this.hostNetwork = hostNetwork;
    }


    public V1beta2SparkApplicationSpecExecutor image(String image) {

        this.image = image;
        return this;
    }

    /**
     * Get image
     *
     * @return image
     **/
    @javax.annotation.Nullable
    @ApiModelProperty(value = "")

    public String getImage() {
        return image;
    }


    public void setImage(String image) {
        this.image = image;
    }


    public V1beta2SparkApplicationSpecExecutor initContainers(List<Container> initContainers) {

        this.initContainers = initContainers;
        return this;
    }

    public V1beta2SparkApplicationSpecExecutor addInitContainersItem(Container initContainersItem) {
        if (this.initContainers == null) {
            this.initContainers = new ArrayList<Container>();
        }
        this.initContainers.add(initContainersItem);
        return this;
    }

    /**
     * Get initContainers
     *
     * @return initContainers
     **/
    @javax.annotation.Nullable
    @ApiModelProperty(value = "")

    public List<Container> getInitContainers() {
        return initContainers;
    }


    public void setInitContainers(List<Container> initContainers) {
        this.initContainers = initContainers;
    }


    public V1beta2SparkApplicationSpecExecutor instances(Integer instances) {

        this.instances = instances;
        return this;
    }

    /**
     * Get instances
     * minimum: 1
     *
     * @return instances
     **/
    @javax.annotation.Nullable
    @ApiModelProperty(value = "")

    public Integer getInstances() {
        return instances;
    }


    public void setInstances(Integer instances) {
        this.instances = instances;
    }


    public V1beta2SparkApplicationSpecExecutor javaOptions(String javaOptions) {

        this.javaOptions = javaOptions;
        return this;
    }

    /**
     * Get javaOptions
     *
     * @return javaOptions
     **/
    @javax.annotation.Nullable
    @ApiModelProperty(value = "")

    public String getJavaOptions() {
        return javaOptions;
    }


    public void setJavaOptions(String javaOptions) {
        this.javaOptions = javaOptions;
    }


    public V1beta2SparkApplicationSpecExecutor labels(Map<String, String> labels) {

        this.labels = labels;
        return this;
    }

    public V1beta2SparkApplicationSpecExecutor putLabelsItem(String key, String labelsItem) {
        if (this.labels == null) {
            this.labels = new HashMap<>();
        }
        this.labels.put(key, labelsItem);
        return this;
    }

    /**
     * Get labels
     *
     * @return labels
     **/
    @javax.annotation.Nullable
    @ApiModelProperty(value = "")
    @Nonnull
    public Map<String, String> getLabels() {
        if (labels == null) {
            labels = new HashMap<>();
        }

        return labels;
    }


    public void setLabels(Map<String, String> labels) {
        this.labels = labels;
    }


    public V1beta2SparkApplicationSpecExecutor memory(String memory) {

        this.memory = memory;
        return this;
    }

    /**
     * Get memory
     *
     * @return memory
     **/
    @javax.annotation.Nullable
    @ApiModelProperty(value = "")

    public String getMemory() {
        return memory;
    }


    public void setMemory(String memory) {
        this.memory = memory;
    }


    public V1beta2SparkApplicationSpecExecutor memoryOverhead(String memoryOverhead) {

        this.memoryOverhead = memoryOverhead;
        return this;
    }

    /**
     * Get memoryOverhead
     *
     * @return memoryOverhead
     **/
    @javax.annotation.Nullable
    @ApiModelProperty(value = "")

    public String getMemoryOverhead() {
        return memoryOverhead;
    }


    public void setMemoryOverhead(String memoryOverhead) {
        this.memoryOverhead = memoryOverhead;
    }


    public V1beta2SparkApplicationSpecExecutor nodeSelector(Map<String, String> nodeSelector) {

        this.nodeSelector = nodeSelector;
        return this;
    }

    public V1beta2SparkApplicationSpecExecutor putNodeSelectorItem(String key, String nodeSelectorItem) {
        if (this.nodeSelector == null) {
            this.nodeSelector = new HashMap<>();
        }
        this.nodeSelector.put(key, nodeSelectorItem);
        return this;
    }

    /**
     * Get nodeSelector
     *
     * @return nodeSelector
     **/
    @javax.annotation.Nullable
    @ApiModelProperty(value = "")

    public Map<String, String> getNodeSelector() {
        return nodeSelector;
    }


    public void setNodeSelector(Map<String, String> nodeSelector) {
        this.nodeSelector = nodeSelector;
    }


    public V1beta2SparkApplicationSpecExecutor podSecurityContext(V1beta2SparkApplicationSpecDriverPodSecurityContext podSecurityContext) {

        return this;
    }


    public V1beta2SparkApplicationSpecExecutor schedulerName(String schedulerName) {

        this.schedulerName = schedulerName;
        return this;
    }

    /**
     * Get schedulerName
     *
     * @return schedulerName
     **/
    @javax.annotation.Nullable
    @ApiModelProperty(value = "")

    public String getSchedulerName() {
        return schedulerName;
    }


    public void setSchedulerName(String schedulerName) {
        this.schedulerName = schedulerName;
    }


    public V1beta2SparkApplicationSpecExecutor secrets(List<V1beta2SparkApplicationSpecDriverSecrets> secrets) {

        this.secrets = secrets;
        return this;
    }

    public V1beta2SparkApplicationSpecExecutor addSecretsItem(V1beta2SparkApplicationSpecDriverSecrets secretsItem) {
        if (this.secrets == null) {
            this.secrets = new ArrayList<>();
        }
        this.secrets.add(secretsItem);
        return this;
    }

    /**
     * Get secrets
     *
     * @return secrets
     **/
    @javax.annotation.Nullable
    @ApiModelProperty(value = "")

    public List<V1beta2SparkApplicationSpecDriverSecrets> getSecrets() {
        return secrets;
    }


    public void setSecrets(List<V1beta2SparkApplicationSpecDriverSecrets> secrets) {
        this.secrets = secrets;
    }


    public V1beta2SparkApplicationSpecExecutor securityContext(PodSecurityContext securityContext) {

        this.securityContext = securityContext;
        return this;
    }

    /**
     * Get securityContext
     *
     * @return securityContext
     **/
    @javax.annotation.Nullable
    @ApiModelProperty(value = "")

    public PodSecurityContext getSecurityContext() {
        return securityContext;
    }


    public void setSecurityContext(PodSecurityContext securityContext) {
        this.securityContext = securityContext;
    }


    public V1beta2SparkApplicationSpecExecutor serviceAccount(String serviceAccount) {

        this.serviceAccount = serviceAccount;
        return this;
    }

    /**
     * Get serviceAccount
     *
     * @return serviceAccount
     **/
    @javax.annotation.Nullable
    @ApiModelProperty(value = "")

    public String getServiceAccount() {
        return serviceAccount;
    }


    public void setServiceAccount(String serviceAccount) {
        this.serviceAccount = serviceAccount;
    }


    public V1beta2SparkApplicationSpecExecutor shareProcessNamespace(Boolean shareProcessNamespace) {

        this.shareProcessNamespace = shareProcessNamespace;
        return this;
    }

    /**
     * Get shareProcessNamespace
     *
     * @return shareProcessNamespace
     **/
    @javax.annotation.Nullable
    @ApiModelProperty(value = "")

    public Boolean getShareProcessNamespace() {
        return shareProcessNamespace;
    }


    public void setShareProcessNamespace(Boolean shareProcessNamespace) {
        this.shareProcessNamespace = shareProcessNamespace;
    }


    public V1beta2SparkApplicationSpecExecutor sidecars(List<Container> sidecars) {

        this.sidecars = sidecars;
        return this;
    }

    public V1beta2SparkApplicationSpecExecutor addSidecarsItem(Container sidecarsItem) {
        if (this.sidecars == null) {
            this.sidecars = new ArrayList<Container>();
        }
        this.sidecars.add(sidecarsItem);
        return this;
    }

    /**
     * Get sidecars
     *
     * @return sidecars
     **/
    @javax.annotation.Nullable
    @ApiModelProperty(value = "")

    public List<Container> getSidecars() {
        return sidecars;
    }


    public void setSidecars(List<Container> sidecars) {
        this.sidecars = sidecars;
    }


    public V1beta2SparkApplicationSpecExecutor terminationGracePeriodSeconds(Long terminationGracePeriodSeconds) {

        this.terminationGracePeriodSeconds = terminationGracePeriodSeconds;
        return this;
    }

    /**
     * Get terminationGracePeriodSeconds
     *
     * @return terminationGracePeriodSeconds
     **/
    @javax.annotation.Nullable
    @ApiModelProperty(value = "")

    public Long getTerminationGracePeriodSeconds() {
        return terminationGracePeriodSeconds;
    }


    public void setTerminationGracePeriodSeconds(Long terminationGracePeriodSeconds) {
        this.terminationGracePeriodSeconds = terminationGracePeriodSeconds;
    }


    public V1beta2SparkApplicationSpecExecutor tolerations(List<Toleration> tolerations) {

        this.tolerations = tolerations;
        return this;
    }

    public V1beta2SparkApplicationSpecExecutor addTolerationsItem(Toleration tolerationsItem) {
        if (this.tolerations == null) {
            this.tolerations = new ArrayList<Toleration>();
        }
        this.tolerations.add(tolerationsItem);
        return this;
    }

    /**
     * Get tolerations
     *
     * @return tolerations
     **/
    @javax.annotation.Nullable
    @ApiModelProperty(value = "")

    public List<Toleration> getTolerations() {
        if (tolerations == null) {
            this.tolerations = new ArrayList<Toleration>();
        }

        return tolerations;
    }


    public void setTolerations(List<Toleration> tolerations) {
        this.tolerations = tolerations;
    }


    public V1beta2SparkApplicationSpecExecutor volumeMounts(List<VolumeMount> volumeMounts) {

        this.volumeMounts = volumeMounts;
        return this;
    }

    public V1beta2SparkApplicationSpecExecutor addVolumeMountsItem(VolumeMount volumeMountsItem) {
        if (this.volumeMounts == null) {
            this.volumeMounts = new ArrayList<VolumeMount>();
        }
        this.volumeMounts.add(volumeMountsItem);
        return this;
    }

    /**
     * Get volumeMounts
     *
     * @return volumeMounts
     **/
    @javax.annotation.Nullable
    @ApiModelProperty(value = "")

    public List<VolumeMount> getVolumeMounts() {
        return volumeMounts;
    }


    public void setVolumeMounts(List<VolumeMount> volumeMounts) {
        this.volumeMounts = volumeMounts;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        V1beta2SparkApplicationSpecExecutor v1beta2SparkApplicationSpecExecutor =
                (V1beta2SparkApplicationSpecExecutor) o;
        return Objects.equals(this.affinity, v1beta2SparkApplicationSpecExecutor.affinity) &&
                Objects.equals(this.annotations, v1beta2SparkApplicationSpecExecutor.annotations) &&
                Objects.equals(this.configMaps, v1beta2SparkApplicationSpecExecutor.configMaps) &&
                Objects.equals(this.coreLimit, v1beta2SparkApplicationSpecExecutor.coreLimit) &&
                Objects.equals(this.coreRequest, v1beta2SparkApplicationSpecExecutor.coreRequest) &&
                Objects.equals(this.cores, v1beta2SparkApplicationSpecExecutor.cores) &&
                Objects.equals(this.deleteOnTermination, v1beta2SparkApplicationSpecExecutor.deleteOnTermination) &&
                Objects.equals(this.dnsConfig, v1beta2SparkApplicationSpecExecutor.dnsConfig) &&
                Objects.equals(this.env, v1beta2SparkApplicationSpecExecutor.env) &&
                Objects.equals(this.envFrom, v1beta2SparkApplicationSpecExecutor.envFrom) &&
                Objects.equals(this.envSecretKeyRefs, v1beta2SparkApplicationSpecExecutor.envSecretKeyRefs) &&
                Objects.equals(this.envVars, v1beta2SparkApplicationSpecExecutor.envVars) &&
                Objects.equals(this.gpu, v1beta2SparkApplicationSpecExecutor.gpu) &&
                Objects.equals(this.hostAliases, v1beta2SparkApplicationSpecExecutor.hostAliases) &&
                Objects.equals(this.hostNetwork, v1beta2SparkApplicationSpecExecutor.hostNetwork) &&
                Objects.equals(this.image, v1beta2SparkApplicationSpecExecutor.image) &&
                Objects.equals(this.initContainers, v1beta2SparkApplicationSpecExecutor.initContainers) &&
                Objects.equals(this.instances, v1beta2SparkApplicationSpecExecutor.instances) &&
                Objects.equals(this.javaOptions, v1beta2SparkApplicationSpecExecutor.javaOptions) &&
                Objects.equals(this.labels, v1beta2SparkApplicationSpecExecutor.labels) &&
                Objects.equals(this.memory, v1beta2SparkApplicationSpecExecutor.memory) &&
                Objects.equals(this.memoryOverhead, v1beta2SparkApplicationSpecExecutor.memoryOverhead) &&
                Objects.equals(this.nodeSelector, v1beta2SparkApplicationSpecExecutor.nodeSelector) &&
                Objects.equals(this.schedulerName, v1beta2SparkApplicationSpecExecutor.schedulerName) &&
                Objects.equals(this.secrets, v1beta2SparkApplicationSpecExecutor.secrets) &&
                Objects.equals(this.securityContext, v1beta2SparkApplicationSpecExecutor.securityContext) &&
                Objects.equals(this.serviceAccount, v1beta2SparkApplicationSpecExecutor.serviceAccount) &&
                Objects.equals(this.shareProcessNamespace, v1beta2SparkApplicationSpecExecutor.shareProcessNamespace) &&
                Objects.equals(this.sidecars, v1beta2SparkApplicationSpecExecutor.sidecars) &&
                Objects.equals(this.terminationGracePeriodSeconds,
                        v1beta2SparkApplicationSpecExecutor.terminationGracePeriodSeconds) &&
                Objects.equals(this.tolerations, v1beta2SparkApplicationSpecExecutor.tolerations) &&
                Objects.equals(this.volumeMounts, v1beta2SparkApplicationSpecExecutor.volumeMounts);
    }

    @Override
    public int hashCode() {
        return Objects.hash(affinity, annotations, configMaps, coreLimit, coreRequest, cores, deleteOnTermination,
                dnsConfig, env, envFrom, envSecretKeyRefs, envVars, gpu, hostAliases, hostNetwork, image,
                initContainers, instances, javaOptions, labels, memory, memoryOverhead, nodeSelector,
                schedulerName, secrets, securityContext, serviceAccount, shareProcessNamespace,
                sidecars, terminationGracePeriodSeconds, tolerations, volumeMounts);
    }


    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class V1beta2SparkApplicationSpecExecutor {\n");
        sb.append("    affinity: ").append(toIndentedString(affinity)).append("\n");
        sb.append("    annotations: ").append(toIndentedString(annotations)).append("\n");
        sb.append("    configMaps: ").append(toIndentedString(configMaps)).append("\n");
        sb.append("    coreLimit: ").append(toIndentedString(coreLimit)).append("\n");
        sb.append("    coreRequest: ").append(toIndentedString(coreRequest)).append("\n");
        sb.append("    cores: ").append(toIndentedString(cores)).append("\n");
        sb.append("    deleteOnTermination: ").append(toIndentedString(deleteOnTermination)).append("\n");
        sb.append("    dnsConfig: ").append(toIndentedString(dnsConfig)).append("\n");
        sb.append("    env: ").append(toIndentedString(env)).append("\n");
        sb.append("    envFrom: ").append(toIndentedString(envFrom)).append("\n");
        sb.append("    envSecretKeyRefs: ").append(toIndentedString(envSecretKeyRefs)).append("\n");
        sb.append("    envVars: ").append(toIndentedString(envVars)).append("\n");
        sb.append("    gpu: ").append(toIndentedString(gpu)).append("\n");
        sb.append("    hostAliases: ").append(toIndentedString(hostAliases)).append("\n");
        sb.append("    hostNetwork: ").append(toIndentedString(hostNetwork)).append("\n");
        sb.append("    image: ").append(toIndentedString(image)).append("\n");
        sb.append("    initContainers: ").append(toIndentedString(initContainers)).append("\n");
        sb.append("    instances: ").append(toIndentedString(instances)).append("\n");
        sb.append("    javaOptions: ").append(toIndentedString(javaOptions)).append("\n");
        sb.append("    labels: ").append(toIndentedString(labels)).append("\n");
        sb.append("    memory: ").append(toIndentedString(memory)).append("\n");
        sb.append("    memoryOverhead: ").append(toIndentedString(memoryOverhead)).append("\n");
        sb.append("    nodeSelector: ").append(toIndentedString(nodeSelector)).append("\n");
        sb.append("    schedulerName: ").append(toIndentedString(schedulerName)).append("\n");
        sb.append("    secrets: ").append(toIndentedString(secrets)).append("\n");
        sb.append("    securityContext: ").append(toIndentedString(securityContext)).append("\n");
        sb.append("    serviceAccount: ").append(toIndentedString(serviceAccount)).append("\n");
        sb.append("    shareProcessNamespace: ").append(toIndentedString(shareProcessNamespace)).append("\n");
        sb.append("    sidecars: ").append(toIndentedString(sidecars)).append("\n");
        sb.append("    terminationGracePeriodSeconds: ").append(toIndentedString(terminationGracePeriodSeconds))
          .append("\n");
        sb.append("    tolerations: ").append(toIndentedString(tolerations)).append("\n");
        sb.append("    volumeMounts: ").append(toIndentedString(volumeMounts)).append("\n");
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
