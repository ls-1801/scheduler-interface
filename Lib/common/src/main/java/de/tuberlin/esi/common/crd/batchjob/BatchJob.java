package de.tuberlin.esi.common.crd.batchjob;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.ShortNames;
import io.fabric8.kubernetes.model.annotation.Version;

@Group("esi.tu-berlin.de")
@Version("v1alpha1")
@ShortNames("bj")
public class BatchJob extends CustomResource<BatchJobSpec, BatchJobStatus> implements Namespaced {
    @Override
    protected BatchJobStatus initStatus() {
        return new BatchJobStatus();
    }

    @JsonIgnore
    public boolean isSpark() {
        return this.getSpec().getSparkSpec() != null;
    }

    @JsonIgnore
    public boolean isFlink() {
        return this.getSpec().getFlinkSpec() != null;
    }
}