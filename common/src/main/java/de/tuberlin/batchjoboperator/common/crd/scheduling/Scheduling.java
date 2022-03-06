package de.tuberlin.batchjoboperator.common.crd.scheduling;

import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.ShortNames;
import io.fabric8.kubernetes.model.annotation.Version;


@Group("batchjob.gcr.io")
@Version("v1alpha1")
@ShortNames("scheduling")
public class Scheduling extends CustomResource<SchedulingSpec, SchedulingStatus> implements Namespaced {
    @Override
    protected SchedulingStatus initStatus() {
        return new SchedulingStatus();
    }
}
