package de.tuberlin.batchjoboperator.batchjobreconciler.reconciler;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Pod;

import java.util.List;

public interface ApplicationSpecific {
    boolean isCompleted();

    boolean isRunning();

    List<Pod> getPods();

    boolean isExisting();

    void delete();

    <T extends HasMetadata> T getApplication();
}
