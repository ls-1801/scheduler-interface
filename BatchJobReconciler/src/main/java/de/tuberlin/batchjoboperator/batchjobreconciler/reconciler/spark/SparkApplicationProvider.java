package de.tuberlin.batchjoboperator.batchjobreconciler.reconciler.spark;

import de.tuberlin.batchjoboperator.batchjobreconciler.reconciler.ApplicationSpecific;
import de.tuberlin.batchjoboperator.common.NamespacedName;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.KubernetesClient;
import k8s.sparkoperator.SparkApplication;
import k8s.sparkoperator.SparkApplicationStatusState;
import lombok.RequiredArgsConstructor;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RequiredArgsConstructor
public class SparkApplicationProvider implements ApplicationSpecific {
    private final KubernetesClient client;
    private final NamespacedName name;

    private final Set<String> completedState = Set.of(
            SparkApplicationStatusState.CompletedState.getState()
    );

    private final Set<String> runningState = Set.of(
            SparkApplicationStatusState.RunningState.getState()
    );
    private List<Pod> pods = null;
    private SparkApplication cache = null;

    @Override
    public boolean isCompleted() {
        if (!isExisting()) {
            return false;
        }

        return completedState.contains(getApplication().getStatus().getApplicationState().getState().getState());
    }

    @Override
    public boolean isRunning() {
        if (!isExisting()) {
            return false;
        }

        return runningState.contains(getApplication().getStatus().getApplicationState().getState().getState());

    }

    @Override
    public List<Pod> getPods() {
        if (!isExisting())
            return Collections.emptyList();

        if (pods == null) {
            pods = client.pods()
                         .inNamespace(name.getNamespace())
                         .withLabels(Map.of(
                                 "sparkoperator.k8s.io/app-name", name.getName(),
                                 "spark-role", "executor")
                         )
                         .list().getItems();
        }

        return pods;

    }

    @Override
    public boolean isExisting() {
        return getApplication() != null;
    }

    @Override
    public void delete() {
        client.resources(SparkApplication.class)
              .inNamespace(name.getNamespace())
              .withName(name.getName())
              .delete();
    }

    @Override
    public SparkApplication getApplication() {
        return getApplicationInternal();
    }

    private SparkApplication getApplicationInternal() {
        if (cache == null) {
            cache = client.resources(SparkApplication.class)
                          .inNamespace(name.getNamespace())
                          .withName(name.getName())
                          .get();
        }

        return cache;
    }

}
