package de.tuberlin.batchjoboperator.batchjobreconciler.reconciler.spark;

import de.tuberlin.batchjoboperator.batchjobreconciler.reconciler.ApplicationProvider;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.KubernetesClient;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class SparkApplicationProvider implements ApplicationProvider {
    private final KubernetesClient context;

    @Override
    public boolean isCompleted() {
        return false;
    }

    @Override
    public boolean isRunning() {
        return false;
    }

    @Override
    public List<Pod> getPods() {
        return null;
    }

    @Override
    public boolean isExisting() {
        return false;
    }

    @Override
    public void delete() {

    }
}
