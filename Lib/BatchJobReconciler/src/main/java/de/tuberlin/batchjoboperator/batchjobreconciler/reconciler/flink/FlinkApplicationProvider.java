package de.tuberlin.batchjoboperator.batchjobreconciler.reconciler.flink;

import de.tuberlin.batchjoboperator.batchjobreconciler.reconciler.ApplicationSpecific;
import de.tuberlin.batchjoboperator.common.crd.NamespacedName;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.KubernetesClient;
import k8s.flinkoperator.FlinkCluster;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static de.tuberlin.batchjoboperator.common.constants.CommonConstants.FLINK_POD_LABEL;
import static de.tuberlin.batchjoboperator.common.crd.NamespacedName.getName;
import static de.tuberlin.batchjoboperator.common.util.General.getNullSafe;

@RequiredArgsConstructor
@Slf4j
public class FlinkApplicationProvider implements ApplicationSpecific {
    private final KubernetesClient client;
    private final NamespacedName name;
    private final Set<String> failedStates =
            Set.of(
                    "Failed"
            );

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

    private boolean stateInSet(Set<String> set) {
        return getNullSafe(() -> getApplication().getStatus().getState())
                .map(set::contains)
                .orElse(false);
    }

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
        if (getApplication() == null)
            return false;

        var isInCompletedState = stateInSet(completedStates);

        var tms = getApplication().getStatus().getComponents().getTaskManagerStatefulSet().getState().equals("Deleted");
        var jms = getApplication().getStatus().getComponents().getJobManagerStatefulSet().getState().equals("Deleted");

        log.debug("Is {} Completed?, isInCompletedState: {}, tms: {}, jms: {}",
                getName(getApplication()), isInCompletedState, tms, jms
        );

        return isInCompletedState && tms && jms;
    }

    @Override
    public boolean isRunning() {
        if (getApplication() == null)
            return false;

        return stateInSet(runningStates);
    }

    @Override
    public List<Pod> getPods() {
        if (getApplication() == null)
            return Collections.emptyList();

        if (pods == null) {
            pods = client.pods()
                         .inNamespace(name.getNamespace())
                         .withLabels(Map.of(FLINK_POD_LABEL, name.getName(), "component", "taskmanager"))
                         .list().getItems();
        }

        return pods;

    }

    @Override
    public boolean isExisting() {
        boolean applicationDeleted = getApplication() == null;
        boolean podsDeleted = client.pods().inNamespace(name.getNamespace())
                                    .withLabel(FLINK_POD_LABEL, name.getName())
                                    .list().getItems().isEmpty();

        log.debug("Is Flink Application Deleted? ApplicationDeleted: {}, PodsDeleted: {}", applicationDeleted,
                podsDeleted);

        return !(applicationDeleted && podsDeleted);

    }

    @Override
    public void delete() {
        client.resources(FlinkCluster.class).inNamespace(name.getNamespace())
              .withName(name.getName())
              .delete();
    }

    @Override
    public boolean isFailed() {
        if (getApplication() == null)
            return false;

        if (getApplication().getStatus() == null)
            return false;

        return stateInSet(failedStates);
    }
}
