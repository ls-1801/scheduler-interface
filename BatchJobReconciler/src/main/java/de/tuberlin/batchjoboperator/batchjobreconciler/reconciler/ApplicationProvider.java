package de.tuberlin.batchjoboperator.batchjobreconciler.reconciler;

import io.fabric8.kubernetes.api.model.Pod;

import java.util.List;

public interface ApplicationProvider {
    boolean isCompleted();

    boolean isRunning();

    List<Pod> getPods();

    boolean isExisting();

    void delete();
}
