package de.tuberlin.esi.testbedreconciler.reconciler;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.primitives.Ints;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.Quantity;
import lombok.experimental.Delegate;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static de.tuberlin.esi.common.constants.SlotsConstants.SLOT_POD_IS_GHOSTPOD_NAME;
import static de.tuberlin.esi.common.constants.SlotsConstants.SLOT_POD_SLOT_ID_NAME;

@Slf4j
public class ApplicationPodView extends Pod {

    @Delegate
    private final Pod pod;

    private ApplicationPodView(Pod pod) {
        this.pod = pod;
    }

    @Nonnull
    public static ApplicationPodView wrap(@Nonnull Pod pod) {
        if (pod instanceof ApplicationPodView)
            return (ApplicationPodView) pod;

        return new ApplicationPodView(pod);
    }

    public Optional<String> getLabel(String labelName) {
        return Optional.ofNullable(pod.getMetadata())
                       .map(ObjectMeta::getLabels)
                       .map(map -> map.get(labelName));
    }

    public void setLabel(String labelName, String value) {
        if (pod.getMetadata().getLabels() == null) {
            pod.getMetadata().setLabels(new HashMap<>());
        }

        pod.getMetadata().getLabels().put(labelName, value);
    }

    public Optional<String> setAnnotation(String annotationName) {
        return Optional.ofNullable(pod.getMetadata())
                       .map(ObjectMeta::getAnnotations)
                       .map(map -> map.get(annotationName));
    }

    public void setAnnotation(String annotationName, String value) {
        if (pod.getMetadata().getAnnotations() == null) {
            pod.getMetadata().setAnnotations(new HashMap<>());
        }

        pod.getMetadata().getAnnotations().put(annotationName, value);
    }

    @Nullable
    @JsonIgnore
    public String getNodeName() {
        return pod.getSpec().getNodeName();
    }

    @JsonIgnore
    public String getName() {
        return pod.getMetadata().getName();
    }

    @JsonIgnore
    public String getNamespace() {
        return pod.getMetadata().getNamespace();
    }

    @Nonnull
    @JsonIgnore
    public Map<String, Quantity> getRequestMap() {
        if (pod.getSpec().getContainers() == null) {
            log.error("Pod does not have containers, but a request map is used");
            return Collections.emptyMap();
        }

        if (pod.getSpec().getContainers().size() != 1) {
            log.error("Pod either has no or multiple containers, but a request map is used");
            return Collections.emptyMap();
        }

        if (pod.getSpec().getContainers().get(0).getResources() == null || pod.getSpec().getContainers().get(0)
                                                                              .getResources().getRequests() == null) {
            log.error("Pods container has no resource requests");
            return Collections.emptyMap();
        }

        return pod.getSpec().getContainers().get(0).getResources().getRequests();
    }

    @JsonIgnore
    @Nullable
    public Integer getSlotId() {
        return getLabel(SLOT_POD_SLOT_ID_NAME)
                .map(Ints::tryParse).orElse(null);
    }

    @Override
    public String toString() {
        return MessageFormat.format("[{0}] {1}",
                getNamespace(),
                getName()
        );
    }

    public boolean isGhostPod() {
        var isGhostPod = getLabel(SLOT_POD_IS_GHOSTPOD_NAME).orElse("false");
        return Boolean.parseBoolean(isGhostPod);
    }
}
