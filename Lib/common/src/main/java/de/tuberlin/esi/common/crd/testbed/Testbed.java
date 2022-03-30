package de.tuberlin.esi.common.crd.testbed;

import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.ShortNames;
import io.fabric8.kubernetes.model.annotation.Version;

@Group("esi.tu-berlin.de")
@Version("v1alpha1")
@ShortNames("tb")
public class Testbed extends CustomResource<TestbedSpec, TestbedStatus> implements Namespaced {

    @Override
    protected TestbedStatus initStatus() {
        return new TestbedStatus();
    }

    public TestbedStatus defaultStatus() {
        return initStatus();
    }
}
