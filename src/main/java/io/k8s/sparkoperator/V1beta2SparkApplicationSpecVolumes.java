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
 * V1beta2SparkApplicationSpecVolumes
 */
@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaClientCodegen", date = "2022-01-30T13:32" +
        ":37.998Z[Etc/UTC]")
public class V1beta2SparkApplicationSpecVolumes {

    private V1beta2SparkApplicationSpecAwsElasticBlockStore awsElasticBlockStore;


    private V1beta2SparkApplicationSpecAzureDisk azureDisk;


    private V1beta2SparkApplicationSpecAzureFile azureFile;


    private V1beta2SparkApplicationSpecCephfs cephfs;


    private V1beta2SparkApplicationSpecCinder cinder;


    private V1beta2SparkApplicationSpecConfigMap configMap;


    private V1beta2SparkApplicationSpecCsi csi;


    private V1beta2SparkApplicationSpecDownwardAPI downwardAPI;


    private V1beta2SparkApplicationSpecEmptyDir emptyDir;


    private V1beta2SparkApplicationSpecFc fc;


    private V1beta2SparkApplicationSpecFlexVolume flexVolume;


    private V1beta2SparkApplicationSpecFlocker flocker;


    private V1beta2SparkApplicationSpecGcePersistentDisk gcePersistentDisk;


    private V1beta2SparkApplicationSpecGitRepo gitRepo;


    private V1beta2SparkApplicationSpecGlusterfs glusterfs;


    private V1beta2SparkApplicationSpecHostPath hostPath;


    private V1beta2SparkApplicationSpecIscsi iscsi;


    private String name;


    private V1beta2SparkApplicationSpecNfs nfs;


    private V1beta2SparkApplicationSpecPersistentVolumeClaim persistentVolumeClaim;


    private V1beta2SparkApplicationSpecPhotonPersistentDisk photonPersistentDisk;


    private V1beta2SparkApplicationSpecPortworxVolume portworxVolume;


    private V1beta2SparkApplicationSpecProjected projected;


    private V1beta2SparkApplicationSpecQuobyte quobyte;


    private V1beta2SparkApplicationSpecRbd rbd;


    private V1beta2SparkApplicationSpecScaleIO scaleIO;


    private V1beta2SparkApplicationSpecSecret secret;


    private V1beta2SparkApplicationSpecStorageos storageos;


    private V1beta2SparkApplicationSpecVsphereVolume vsphereVolume;


    public V1beta2SparkApplicationSpecVolumes awsElasticBlockStore(V1beta2SparkApplicationSpecAwsElasticBlockStore awsElasticBlockStore) {

        this.awsElasticBlockStore = awsElasticBlockStore;
        return this;
    }

    /**
     * Get awsElasticBlockStore
     *
     * @return awsElasticBlockStore
     **/
    @javax.annotation.Nullable
    @ApiModelProperty(value = "")

    public V1beta2SparkApplicationSpecAwsElasticBlockStore getAwsElasticBlockStore() {
        return awsElasticBlockStore;
    }


    public void setAwsElasticBlockStore(V1beta2SparkApplicationSpecAwsElasticBlockStore awsElasticBlockStore) {
        this.awsElasticBlockStore = awsElasticBlockStore;
    }


    public V1beta2SparkApplicationSpecVolumes azureDisk(V1beta2SparkApplicationSpecAzureDisk azureDisk) {

        this.azureDisk = azureDisk;
        return this;
    }

    /**
     * Get azureDisk
     *
     * @return azureDisk
     **/
    @javax.annotation.Nullable
    @ApiModelProperty(value = "")

    public V1beta2SparkApplicationSpecAzureDisk getAzureDisk() {
        return azureDisk;
    }


    public void setAzureDisk(V1beta2SparkApplicationSpecAzureDisk azureDisk) {
        this.azureDisk = azureDisk;
    }


    public V1beta2SparkApplicationSpecVolumes azureFile(V1beta2SparkApplicationSpecAzureFile azureFile) {

        this.azureFile = azureFile;
        return this;
    }

    /**
     * Get azureFile
     *
     * @return azureFile
     **/
    @javax.annotation.Nullable
    @ApiModelProperty(value = "")

    public V1beta2SparkApplicationSpecAzureFile getAzureFile() {
        return azureFile;
    }


    public void setAzureFile(V1beta2SparkApplicationSpecAzureFile azureFile) {
        this.azureFile = azureFile;
    }


    public V1beta2SparkApplicationSpecVolumes cephfs(V1beta2SparkApplicationSpecCephfs cephfs) {

        this.cephfs = cephfs;
        return this;
    }

    /**
     * Get cephfs
     *
     * @return cephfs
     **/
    @javax.annotation.Nullable
    @ApiModelProperty(value = "")

    public V1beta2SparkApplicationSpecCephfs getCephfs() {
        return cephfs;
    }


    public void setCephfs(V1beta2SparkApplicationSpecCephfs cephfs) {
        this.cephfs = cephfs;
    }


    public V1beta2SparkApplicationSpecVolumes cinder(V1beta2SparkApplicationSpecCinder cinder) {

        this.cinder = cinder;
        return this;
    }

    /**
     * Get cinder
     *
     * @return cinder
     **/
    @javax.annotation.Nullable
    @ApiModelProperty(value = "")

    public V1beta2SparkApplicationSpecCinder getCinder() {
        return cinder;
    }


    public void setCinder(V1beta2SparkApplicationSpecCinder cinder) {
        this.cinder = cinder;
    }


    public V1beta2SparkApplicationSpecVolumes configMap(V1beta2SparkApplicationSpecConfigMap configMap) {

        this.configMap = configMap;
        return this;
    }

    /**
     * Get configMap
     *
     * @return configMap
     **/
    @javax.annotation.Nullable
    @ApiModelProperty(value = "")

    public V1beta2SparkApplicationSpecConfigMap getConfigMap() {
        return configMap;
    }


    public void setConfigMap(V1beta2SparkApplicationSpecConfigMap configMap) {
        this.configMap = configMap;
    }


    public V1beta2SparkApplicationSpecVolumes csi(V1beta2SparkApplicationSpecCsi csi) {

        this.csi = csi;
        return this;
    }

    /**
     * Get csi
     *
     * @return csi
     **/
    @javax.annotation.Nullable
    @ApiModelProperty(value = "")

    public V1beta2SparkApplicationSpecCsi getCsi() {
        return csi;
    }


    public void setCsi(V1beta2SparkApplicationSpecCsi csi) {
        this.csi = csi;
    }


    public V1beta2SparkApplicationSpecVolumes downwardAPI(V1beta2SparkApplicationSpecDownwardAPI downwardAPI) {

        this.downwardAPI = downwardAPI;
        return this;
    }

    /**
     * Get downwardAPI
     *
     * @return downwardAPI
     **/
    @javax.annotation.Nullable
    @ApiModelProperty(value = "")

    public V1beta2SparkApplicationSpecDownwardAPI getDownwardAPI() {
        return downwardAPI;
    }


    public void setDownwardAPI(V1beta2SparkApplicationSpecDownwardAPI downwardAPI) {
        this.downwardAPI = downwardAPI;
    }


    public V1beta2SparkApplicationSpecVolumes emptyDir(V1beta2SparkApplicationSpecEmptyDir emptyDir) {

        this.emptyDir = emptyDir;
        return this;
    }

    /**
     * Get emptyDir
     *
     * @return emptyDir
     **/
    @javax.annotation.Nullable
    @ApiModelProperty(value = "")

    public V1beta2SparkApplicationSpecEmptyDir getEmptyDir() {
        return emptyDir;
    }


    public void setEmptyDir(V1beta2SparkApplicationSpecEmptyDir emptyDir) {
        this.emptyDir = emptyDir;
    }


    public V1beta2SparkApplicationSpecVolumes fc(V1beta2SparkApplicationSpecFc fc) {

        this.fc = fc;
        return this;
    }

    /**
     * Get fc
     *
     * @return fc
     **/
    @javax.annotation.Nullable
    @ApiModelProperty(value = "")

    public V1beta2SparkApplicationSpecFc getFc() {
        return fc;
    }


    public void setFc(V1beta2SparkApplicationSpecFc fc) {
        this.fc = fc;
    }


    public V1beta2SparkApplicationSpecVolumes flexVolume(V1beta2SparkApplicationSpecFlexVolume flexVolume) {

        this.flexVolume = flexVolume;
        return this;
    }

    /**
     * Get flexVolume
     *
     * @return flexVolume
     **/
    @javax.annotation.Nullable
    @ApiModelProperty(value = "")

    public V1beta2SparkApplicationSpecFlexVolume getFlexVolume() {
        return flexVolume;
    }


    public void setFlexVolume(V1beta2SparkApplicationSpecFlexVolume flexVolume) {
        this.flexVolume = flexVolume;
    }


    public V1beta2SparkApplicationSpecVolumes flocker(V1beta2SparkApplicationSpecFlocker flocker) {

        this.flocker = flocker;
        return this;
    }

    /**
     * Get flocker
     *
     * @return flocker
     **/
    @javax.annotation.Nullable
    @ApiModelProperty(value = "")

    public V1beta2SparkApplicationSpecFlocker getFlocker() {
        return flocker;
    }


    public void setFlocker(V1beta2SparkApplicationSpecFlocker flocker) {
        this.flocker = flocker;
    }


    public V1beta2SparkApplicationSpecVolumes gcePersistentDisk(V1beta2SparkApplicationSpecGcePersistentDisk gcePersistentDisk) {

        this.gcePersistentDisk = gcePersistentDisk;
        return this;
    }

    /**
     * Get gcePersistentDisk
     *
     * @return gcePersistentDisk
     **/
    @javax.annotation.Nullable
    @ApiModelProperty(value = "")

    public V1beta2SparkApplicationSpecGcePersistentDisk getGcePersistentDisk() {
        return gcePersistentDisk;
    }


    public void setGcePersistentDisk(V1beta2SparkApplicationSpecGcePersistentDisk gcePersistentDisk) {
        this.gcePersistentDisk = gcePersistentDisk;
    }


    public V1beta2SparkApplicationSpecVolumes gitRepo(V1beta2SparkApplicationSpecGitRepo gitRepo) {

        this.gitRepo = gitRepo;
        return this;
    }

    /**
     * Get gitRepo
     *
     * @return gitRepo
     **/
    @javax.annotation.Nullable
    @ApiModelProperty(value = "")

    public V1beta2SparkApplicationSpecGitRepo getGitRepo() {
        return gitRepo;
    }


    public void setGitRepo(V1beta2SparkApplicationSpecGitRepo gitRepo) {
        this.gitRepo = gitRepo;
    }


    public V1beta2SparkApplicationSpecVolumes glusterfs(V1beta2SparkApplicationSpecGlusterfs glusterfs) {

        this.glusterfs = glusterfs;
        return this;
    }

    /**
     * Get glusterfs
     *
     * @return glusterfs
     **/
    @javax.annotation.Nullable
    @ApiModelProperty(value = "")

    public V1beta2SparkApplicationSpecGlusterfs getGlusterfs() {
        return glusterfs;
    }


    public void setGlusterfs(V1beta2SparkApplicationSpecGlusterfs glusterfs) {
        this.glusterfs = glusterfs;
    }


    public V1beta2SparkApplicationSpecVolumes hostPath(V1beta2SparkApplicationSpecHostPath hostPath) {

        this.hostPath = hostPath;
        return this;
    }

    /**
     * Get hostPath
     *
     * @return hostPath
     **/
    @javax.annotation.Nullable
    @ApiModelProperty(value = "")

    public V1beta2SparkApplicationSpecHostPath getHostPath() {
        return hostPath;
    }


    public void setHostPath(V1beta2SparkApplicationSpecHostPath hostPath) {
        this.hostPath = hostPath;
    }


    public V1beta2SparkApplicationSpecVolumes iscsi(V1beta2SparkApplicationSpecIscsi iscsi) {

        this.iscsi = iscsi;
        return this;
    }

    /**
     * Get iscsi
     *
     * @return iscsi
     **/
    @javax.annotation.Nullable
    @ApiModelProperty(value = "")

    public V1beta2SparkApplicationSpecIscsi getIscsi() {
        return iscsi;
    }


    public void setIscsi(V1beta2SparkApplicationSpecIscsi iscsi) {
        this.iscsi = iscsi;
    }


    public V1beta2SparkApplicationSpecVolumes name(String name) {

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


    public V1beta2SparkApplicationSpecVolumes nfs(V1beta2SparkApplicationSpecNfs nfs) {

        this.nfs = nfs;
        return this;
    }

    /**
     * Get nfs
     *
     * @return nfs
     **/
    @javax.annotation.Nullable
    @ApiModelProperty(value = "")

    public V1beta2SparkApplicationSpecNfs getNfs() {
        return nfs;
    }


    public void setNfs(V1beta2SparkApplicationSpecNfs nfs) {
        this.nfs = nfs;
    }


    public V1beta2SparkApplicationSpecVolumes persistentVolumeClaim(V1beta2SparkApplicationSpecPersistentVolumeClaim persistentVolumeClaim) {

        this.persistentVolumeClaim = persistentVolumeClaim;
        return this;
    }

    /**
     * Get persistentVolumeClaim
     *
     * @return persistentVolumeClaim
     **/
    @javax.annotation.Nullable
    @ApiModelProperty(value = "")

    public V1beta2SparkApplicationSpecPersistentVolumeClaim getPersistentVolumeClaim() {
        return persistentVolumeClaim;
    }


    public void setPersistentVolumeClaim(V1beta2SparkApplicationSpecPersistentVolumeClaim persistentVolumeClaim) {
        this.persistentVolumeClaim = persistentVolumeClaim;
    }


    public V1beta2SparkApplicationSpecVolumes photonPersistentDisk(V1beta2SparkApplicationSpecPhotonPersistentDisk photonPersistentDisk) {

        this.photonPersistentDisk = photonPersistentDisk;
        return this;
    }

    /**
     * Get photonPersistentDisk
     *
     * @return photonPersistentDisk
     **/
    @javax.annotation.Nullable
    @ApiModelProperty(value = "")

    public V1beta2SparkApplicationSpecPhotonPersistentDisk getPhotonPersistentDisk() {
        return photonPersistentDisk;
    }


    public void setPhotonPersistentDisk(V1beta2SparkApplicationSpecPhotonPersistentDisk photonPersistentDisk) {
        this.photonPersistentDisk = photonPersistentDisk;
    }


    public V1beta2SparkApplicationSpecVolumes portworxVolume(V1beta2SparkApplicationSpecPortworxVolume portworxVolume) {

        this.portworxVolume = portworxVolume;
        return this;
    }

    /**
     * Get portworxVolume
     *
     * @return portworxVolume
     **/
    @javax.annotation.Nullable
    @ApiModelProperty(value = "")

    public V1beta2SparkApplicationSpecPortworxVolume getPortworxVolume() {
        return portworxVolume;
    }


    public void setPortworxVolume(V1beta2SparkApplicationSpecPortworxVolume portworxVolume) {
        this.portworxVolume = portworxVolume;
    }


    public V1beta2SparkApplicationSpecVolumes projected(V1beta2SparkApplicationSpecProjected projected) {

        this.projected = projected;
        return this;
    }

    /**
     * Get projected
     *
     * @return projected
     **/
    @javax.annotation.Nullable
    @ApiModelProperty(value = "")

    public V1beta2SparkApplicationSpecProjected getProjected() {
        return projected;
    }


    public void setProjected(V1beta2SparkApplicationSpecProjected projected) {
        this.projected = projected;
    }


    public V1beta2SparkApplicationSpecVolumes quobyte(V1beta2SparkApplicationSpecQuobyte quobyte) {

        this.quobyte = quobyte;
        return this;
    }

    /**
     * Get quobyte
     *
     * @return quobyte
     **/
    @javax.annotation.Nullable
    @ApiModelProperty(value = "")

    public V1beta2SparkApplicationSpecQuobyte getQuobyte() {
        return quobyte;
    }


    public void setQuobyte(V1beta2SparkApplicationSpecQuobyte quobyte) {
        this.quobyte = quobyte;
    }


    public V1beta2SparkApplicationSpecVolumes rbd(V1beta2SparkApplicationSpecRbd rbd) {

        this.rbd = rbd;
        return this;
    }

    /**
     * Get rbd
     *
     * @return rbd
     **/
    @javax.annotation.Nullable
    @ApiModelProperty(value = "")

    public V1beta2SparkApplicationSpecRbd getRbd() {
        return rbd;
    }


    public void setRbd(V1beta2SparkApplicationSpecRbd rbd) {
        this.rbd = rbd;
    }


    public V1beta2SparkApplicationSpecVolumes scaleIO(V1beta2SparkApplicationSpecScaleIO scaleIO) {

        this.scaleIO = scaleIO;
        return this;
    }

    /**
     * Get scaleIO
     *
     * @return scaleIO
     **/
    @javax.annotation.Nullable
    @ApiModelProperty(value = "")

    public V1beta2SparkApplicationSpecScaleIO getScaleIO() {
        return scaleIO;
    }


    public void setScaleIO(V1beta2SparkApplicationSpecScaleIO scaleIO) {
        this.scaleIO = scaleIO;
    }


    public V1beta2SparkApplicationSpecVolumes secret(V1beta2SparkApplicationSpecSecret secret) {

        this.secret = secret;
        return this;
    }

    /**
     * Get secret
     *
     * @return secret
     **/
    @javax.annotation.Nullable
    @ApiModelProperty(value = "")

    public V1beta2SparkApplicationSpecSecret getSecret() {
        return secret;
    }


    public void setSecret(V1beta2SparkApplicationSpecSecret secret) {
        this.secret = secret;
    }


    public V1beta2SparkApplicationSpecVolumes storageos(V1beta2SparkApplicationSpecStorageos storageos) {

        this.storageos = storageos;
        return this;
    }

    /**
     * Get storageos
     *
     * @return storageos
     **/
    @javax.annotation.Nullable
    @ApiModelProperty(value = "")

    public V1beta2SparkApplicationSpecStorageos getStorageos() {
        return storageos;
    }


    public void setStorageos(V1beta2SparkApplicationSpecStorageos storageos) {
        this.storageos = storageos;
    }


    public V1beta2SparkApplicationSpecVolumes vsphereVolume(V1beta2SparkApplicationSpecVsphereVolume vsphereVolume) {

        this.vsphereVolume = vsphereVolume;
        return this;
    }

    /**
     * Get vsphereVolume
     *
     * @return vsphereVolume
     **/
    @javax.annotation.Nullable
    @ApiModelProperty(value = "")

    public V1beta2SparkApplicationSpecVsphereVolume getVsphereVolume() {
        return vsphereVolume;
    }


    public void setVsphereVolume(V1beta2SparkApplicationSpecVsphereVolume vsphereVolume) {
        this.vsphereVolume = vsphereVolume;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        V1beta2SparkApplicationSpecVolumes v1beta2SparkApplicationSpecVolumes = (V1beta2SparkApplicationSpecVolumes) o;
        return Objects.equals(this.awsElasticBlockStore, v1beta2SparkApplicationSpecVolumes.awsElasticBlockStore) &&
                Objects.equals(this.azureDisk, v1beta2SparkApplicationSpecVolumes.azureDisk) &&
                Objects.equals(this.azureFile, v1beta2SparkApplicationSpecVolumes.azureFile) &&
                Objects.equals(this.cephfs, v1beta2SparkApplicationSpecVolumes.cephfs) &&
                Objects.equals(this.cinder, v1beta2SparkApplicationSpecVolumes.cinder) &&
                Objects.equals(this.configMap, v1beta2SparkApplicationSpecVolumes.configMap) &&
                Objects.equals(this.csi, v1beta2SparkApplicationSpecVolumes.csi) &&
                Objects.equals(this.downwardAPI, v1beta2SparkApplicationSpecVolumes.downwardAPI) &&
                Objects.equals(this.emptyDir, v1beta2SparkApplicationSpecVolumes.emptyDir) &&
                Objects.equals(this.fc, v1beta2SparkApplicationSpecVolumes.fc) &&
                Objects.equals(this.flexVolume, v1beta2SparkApplicationSpecVolumes.flexVolume) &&
                Objects.equals(this.flocker, v1beta2SparkApplicationSpecVolumes.flocker) &&
                Objects.equals(this.gcePersistentDisk, v1beta2SparkApplicationSpecVolumes.gcePersistentDisk) &&
                Objects.equals(this.gitRepo, v1beta2SparkApplicationSpecVolumes.gitRepo) &&
                Objects.equals(this.glusterfs, v1beta2SparkApplicationSpecVolumes.glusterfs) &&
                Objects.equals(this.hostPath, v1beta2SparkApplicationSpecVolumes.hostPath) &&
                Objects.equals(this.iscsi, v1beta2SparkApplicationSpecVolumes.iscsi) &&
                Objects.equals(this.name, v1beta2SparkApplicationSpecVolumes.name) &&
                Objects.equals(this.nfs, v1beta2SparkApplicationSpecVolumes.nfs) &&
                Objects.equals(this.persistentVolumeClaim, v1beta2SparkApplicationSpecVolumes.persistentVolumeClaim) &&
                Objects.equals(this.photonPersistentDisk, v1beta2SparkApplicationSpecVolumes.photonPersistentDisk) &&
                Objects.equals(this.portworxVolume, v1beta2SparkApplicationSpecVolumes.portworxVolume) &&
                Objects.equals(this.projected, v1beta2SparkApplicationSpecVolumes.projected) &&
                Objects.equals(this.quobyte, v1beta2SparkApplicationSpecVolumes.quobyte) &&
                Objects.equals(this.rbd, v1beta2SparkApplicationSpecVolumes.rbd) &&
                Objects.equals(this.scaleIO, v1beta2SparkApplicationSpecVolumes.scaleIO) &&
                Objects.equals(this.secret, v1beta2SparkApplicationSpecVolumes.secret) &&
                Objects.equals(this.storageos, v1beta2SparkApplicationSpecVolumes.storageos) &&
                Objects.equals(this.vsphereVolume, v1beta2SparkApplicationSpecVolumes.vsphereVolume);
    }

    @Override
    public int hashCode() {
        return Objects.hash(awsElasticBlockStore, azureDisk, azureFile, cephfs, cinder, configMap, csi, downwardAPI,
                emptyDir, fc, flexVolume, flocker, gcePersistentDisk, gitRepo, glusterfs, hostPath, iscsi, name, nfs,
                persistentVolumeClaim, photonPersistentDisk, portworxVolume, projected, quobyte, rbd, scaleIO, secret
                , storageos, vsphereVolume);
    }


    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class V1beta2SparkApplicationSpecVolumes {\n");
        sb.append("    awsElasticBlockStore: ").append(toIndentedString(awsElasticBlockStore)).append("\n");
        sb.append("    azureDisk: ").append(toIndentedString(azureDisk)).append("\n");
        sb.append("    azureFile: ").append(toIndentedString(azureFile)).append("\n");
        sb.append("    cephfs: ").append(toIndentedString(cephfs)).append("\n");
        sb.append("    cinder: ").append(toIndentedString(cinder)).append("\n");
        sb.append("    configMap: ").append(toIndentedString(configMap)).append("\n");
        sb.append("    csi: ").append(toIndentedString(csi)).append("\n");
        sb.append("    downwardAPI: ").append(toIndentedString(downwardAPI)).append("\n");
        sb.append("    emptyDir: ").append(toIndentedString(emptyDir)).append("\n");
        sb.append("    fc: ").append(toIndentedString(fc)).append("\n");
        sb.append("    flexVolume: ").append(toIndentedString(flexVolume)).append("\n");
        sb.append("    flocker: ").append(toIndentedString(flocker)).append("\n");
        sb.append("    gcePersistentDisk: ").append(toIndentedString(gcePersistentDisk)).append("\n");
        sb.append("    gitRepo: ").append(toIndentedString(gitRepo)).append("\n");
        sb.append("    glusterfs: ").append(toIndentedString(glusterfs)).append("\n");
        sb.append("    hostPath: ").append(toIndentedString(hostPath)).append("\n");
        sb.append("    iscsi: ").append(toIndentedString(iscsi)).append("\n");
        sb.append("    name: ").append(toIndentedString(name)).append("\n");
        sb.append("    nfs: ").append(toIndentedString(nfs)).append("\n");
        sb.append("    persistentVolumeClaim: ").append(toIndentedString(persistentVolumeClaim)).append("\n");
        sb.append("    photonPersistentDisk: ").append(toIndentedString(photonPersistentDisk)).append("\n");
        sb.append("    portworxVolume: ").append(toIndentedString(portworxVolume)).append("\n");
        sb.append("    projected: ").append(toIndentedString(projected)).append("\n");
        sb.append("    quobyte: ").append(toIndentedString(quobyte)).append("\n");
        sb.append("    rbd: ").append(toIndentedString(rbd)).append("\n");
        sb.append("    scaleIO: ").append(toIndentedString(scaleIO)).append("\n");
        sb.append("    secret: ").append(toIndentedString(secret)).append("\n");
        sb.append("    storageos: ").append(toIndentedString(storageos)).append("\n");
        sb.append("    vsphereVolume: ").append(toIndentedString(vsphereVolume)).append("\n");
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

