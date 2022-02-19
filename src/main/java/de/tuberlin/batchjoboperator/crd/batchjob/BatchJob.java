package de.tuberlin.batchjoboperator.crd.batchjob;

import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.ShortNames;
import io.fabric8.kubernetes.model.annotation.Version;

@Group("batchjob.gcr.io")
@Version("v1alpha1")
@ShortNames("job")
public class BatchJob extends CustomResource<BatchJobSpec, BatchJobStatus> implements Namespaced {
    @Override
    protected BatchJobStatus initStatus() {
        return new BatchJobStatus();
    }
}