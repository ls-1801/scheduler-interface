package de.tuberlin.esi.common.crd.slots;

import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.ShortNames;
import io.fabric8.kubernetes.model.annotation.Version;

@Group("batchjob.gcr.io")
@Version("v1alpha1")
@ShortNames("slot")
public class Slot extends CustomResource<SlotSpec, SlotStatus> implements Namespaced {

    @Override
    protected SlotStatus initStatus() {
        return new SlotStatus();
    }

    public SlotStatus defaultStatus() {
        return initStatus();
    }
}
