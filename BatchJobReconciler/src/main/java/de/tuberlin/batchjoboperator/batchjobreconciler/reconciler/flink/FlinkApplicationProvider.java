package de.tuberlin.batchjoboperator.batchjobreconciler.reconciler.flink;

import de.tuberlin.batchjoboperator.batchjobreconciler.reconciler.ApplicationSpecific;
import de.tuberlin.batchjoboperator.common.NamespacedName;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.KubernetesClient;
import k8s.flinkoperator.FlinkCluster;
import lombok.RequiredArgsConstructor;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RequiredArgsConstructor
public class FlinkApplicationProvider implements ApplicationSpecific {
    private final KubernetesClient client;
    private final NamespacedName name;
    private final Set<String> runningStates =
            Set.of(
                    "Running"
            );
    private final Set<String> completedStates =
            Set.of(
                    "Stopped"
            );
    private FlinkCluster cache = null;
    private List<Pod> pods = null;

    @Override
    public FlinkCluster getApplication() {
        return getApplicationInternal();
    }

    private FlinkCluster getApplicationInternal() {
        if (cache == null) {
            cache = client.resources(FlinkCluster.class)
                          .inNamespace(name.getNamespace())
                          .withName(name.getName())
                          .get();
        }

        return cache;
    }

    @Override
    public boolean isCompleted() {
        if (!isExisting())
            return false;

        return completedStates.contains(getApplication().getStatus().getState());

    }

    @Override
    public boolean isRunning() {
        if (!isExisting())
            return false;

        return runningStates.contains(getApplication().getStatus().getState());
    }

    @Override
    public List<Pod> getPods() {
        if (!isExisting())
            return Collections.emptyList();

        if (pods == null) {
            pods = client.pods()
                         .inNamespace(name.getNamespace())
                         .withLabels(Map.of("cluster", name.getName(), "component", "taskmanager"))
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
        client.resources(FlinkCluster.class).inNamespace(name.getNamespace())
              .withName(name.getName())
              .delete();
    }
}
