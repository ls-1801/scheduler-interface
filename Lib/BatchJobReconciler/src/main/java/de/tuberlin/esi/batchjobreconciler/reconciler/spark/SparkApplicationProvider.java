package de.tuberlin.esi.batchjobreconciler.reconciler.spark;

import de.tuberlin.esi.batchjobreconciler.reconciler.ApplicationSpecific;
import de.tuberlin.esi.common.crd.NamespacedName;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.KubernetesClient;
import k8s.sparkoperator.SparkApplication;
import k8s.sparkoperator.SparkApplicationStatusState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static de.tuberlin.esi.common.constants.CommonConstants.SPARK_POD_LABEL;
import static de.tuberlin.esi.common.util.General.getNullSafe;

@RequiredArgsConstructor
@Slf4j
public class SparkApplicationProvider implements ApplicationSpecific {
    private final KubernetesClient client;
    private final NamespacedName name;

    private final Set<String> completedState = Set.of(
            SparkApplicationStatusState.CompletedState.getState()
    );

    private final Set<String> runningState = Set.of(
            SparkApplicationStatusState.RunningState.getState()
    );

    private final Set<String> failedState = Set.of(
            SparkApplicationStatusState.FailedState.getState(),
            SparkApplicationStatusState.FailedSubmissionState.getState(),
            SparkApplicationStatusState.FailingState.getState()
    );

    private List<Pod> pods = null;
    private SparkApplication cache = null;

    private boolean stateInSet(Set<String> set) {
        return getNullSafe(() -> getApplication().getStatus().getApplicationState().getState().getState())
                .map(set::contains)
                .orElse(false);
    }

    @Override
    public boolean isCompleted() {
        if (getApplication() == null) {
            return false;
        }

        return stateInSet(completedState);
    }

    @Override
    public boolean isRunning() {
        if (getApplication() == null) {
            return false;
        }

        var applicationRunning = stateInSet(runningState);


        var allExecutorsRunning = getNullSafe(() -> getApplication().getStatus().getExecutorState().values().stream()
                                                                    .allMatch(runningState::contains)).orElse(false);
        return applicationRunning && allExecutorsRunning;
    }

    @Override
    public List<Pod> getPods() {
        if (getApplication() == null)
            return Collections.emptyList();

        if (pods == null) {
            pods = client.pods()
                         .inNamespace(name.getNamespace())
                         .withLabels(Map.of(
                                 SPARK_POD_LABEL, name.getName(),
                                 "spark-role", "executor")
                         )
                         .list().getItems();
        }

        return pods;

    }

    @Override
    public boolean isExisting() {
        boolean applicationDeleted = getApplication() == null;
        boolean podsDeleted = client.pods().inNamespace(name.getNamespace())
                                    .withLabel(SPARK_POD_LABEL, name.getName())
                                    .list().getItems().isEmpty();

        log.debug("Is Spark Application Deleted? ApplicationDeleted: {}, PodsDeleted: {}", applicationDeleted,
                podsDeleted);

        return !(applicationDeleted && podsDeleted);
    }

    @Override
    public void delete() {
        client.resources(SparkApplication.class)
              .inNamespace(name.getNamespace())
              .withName(name.getName())
              .delete();
    }


    @Override
    public boolean isFailed() {
        if (getApplication() == null)
            return false;

        return stateInSet(failedState);
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
