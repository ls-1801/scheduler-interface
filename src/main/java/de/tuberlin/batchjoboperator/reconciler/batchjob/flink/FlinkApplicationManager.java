package de.tuberlin.batchjoboperator.reconciler.batchjob.flink;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.common.primitives.Longs;
import de.tuberlin.batchjoboperator.crd.batchjob.BatchJob;
import de.tuberlin.batchjoboperator.crd.batchjob.BatchJobState;
import de.tuberlin.batchjoboperator.reconciler.batchjob.common.AbstractApplicationManager;
import de.tuberlin.batchjoboperator.reconciler.batchjob.common.statemachine.AbstractState;
import de.tuberlin.batchjoboperator.reconciler.batchjob.common.statemachine.ManagedApplicationEvent;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.zjsonpatch.JsonDiff;
import io.javaoperatorsdk.operator.api.reconciler.UpdateControl;
import io.k8s.flinkoperator.FlinkCluster;
import io.k8s.flinkoperator.V1beta1FlinkClusterStatus;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import java.util.Objects;

import static de.tuberlin.batchjoboperator.reconciler.batchjob.common.statemachine.ManagedApplicationEvent.COMPLETED;
import static de.tuberlin.batchjoboperator.reconciler.batchjob.common.statemachine.ManagedApplicationEvent.ERROR;
import static de.tuberlin.batchjoboperator.reconciler.batchjob.common.statemachine.ManagedApplicationEvent.NO_APPLICATION;
import static de.tuberlin.batchjoboperator.reconciler.batchjob.common.statemachine.ManagedApplicationEvent.NO_CHANGE;
import static de.tuberlin.batchjoboperator.reconciler.batchjob.common.statemachine.ManagedApplicationEvent.RUNNING;
import static de.tuberlin.batchjoboperator.reconciler.batchjob.common.statemachine.ManagedApplicationEvent.SCHEDULED;
import static java.util.Optional.ofNullable;

@Slf4j
@ThreadSafe
@AllArgsConstructor
public class FlinkApplicationManager extends AbstractApplicationManager {
    private static final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    private final KubernetesClient kubernetesClient;
    private final FlinkApplicationManagerService service;

    private final FlinkCluster mostRecentState;
    @Nullable
    private final FlinkCluster previousState;

    @Override
    protected Pair<AbstractState.Action, ManagedApplicationEvent> getAction(@Nonnull BatchJob resource) {

        var event = getEventType(resource);
        var action = new AbstractState.Action() {
            private boolean wasDeleted = false;

            @Override
            public UpdateControl<BatchJob> notExpected(String stateName) {
                throw new RuntimeException(event + " caused an unexpected State transition from: " + stateName);
            }

            @Override
            public UpdateControl<BatchJob> updateState(BatchJobState newState) {
                if (Objects.equals(resource.getStatus().getState(), newState)) return noChange();
                log.info("{} caused BatchJob changes state from {} to {}", event, resource.getStatus()
                                                                                          .getState(), newState);
                resource.getStatus().setState(newState);

                if (resourceVersionChanged(resource) && mostRecentState != null) {
                    resource.getStatus().setLatestResourceVersion(NumberUtils.toLong(mostRecentState.getMetadata()
                                                                                                    .getResourceVersion()));
                }

                return UpdateControl.updateStatus(resource);
            }

            @Override
            public UpdateControl<BatchJob> noChange() {
                if (wasDeleted) {
                    resource.getStatus().setLatestResourceVersion(null);
                    return UpdateControl.updateStatus(resource);
                }

                if (resourceVersionChanged(resource) && mostRecentState != null) {
                    resource.getStatus().setLatestResourceVersion(NumberUtils.toLong(mostRecentState.getMetadata()
                                                                                                    .getResourceVersion()));
                    return UpdateControl.updateStatus(resource);
                }

                return UpdateControl.noUpdate();
            }

            @Override
            public UpdateControl<BatchJob> deleteApplication() {
                log.info("Delete FlinkCluster");
                kubernetesClient.resources(FlinkCluster.class).delete(mostRecentState);
                service.removeManager(mostRecentState);
                wasDeleted = true;
                return UpdateControl.updateStatus(resource);
            }
        };


        return Pair.of(action, event);
    }

    private ManagedApplicationEvent getEventType(BatchJob resource) {

        if (!resourceVersionChanged(resource)) {
            return ManagedApplicationEvent.NO_CHANGE;
        }


        var event = ofNullable(mostRecentState).map(CustomResource::getStatus)
                                               .map(V1beta1FlinkClusterStatus::getState)
                                               .map(this::toEventEnum).orElse(NO_APPLICATION);

        var previousEvent = ofNullable(previousState).map(CustomResource::getStatus)
                                                     .map(V1beta1FlinkClusterStatus::getState)
                                                     .map(this::toEventEnum).orElse(NO_APPLICATION);

        if (event == previousEvent) {
            debugJsonDiff();
            return NO_CHANGE;
        }

        log.debug("Application went from {} to {}", previousEvent, event);
        return event;
    }

    @SneakyThrows
    private void debugJsonDiff() {
        if (previousState == null || mostRecentState == null) {
            log.info("Either PreviousState ({}) or mostRecentState ({}) was null", previousState == null ? "null" :
                    "not null", mostRecentState == null ? "null" : "not null");

            return;
        }

        var prev = mapper.valueToTree(previousState);
        var recent = mapper.valueToTree(mostRecentState);
        log.info("DIFF: {}", mapper.writeValueAsString(JsonDiff.asJson(prev, recent)));

    }

    private ManagedApplicationEvent toEventEnum(String state) {
        switch (state) {
            case "Creating":
                return SCHEDULED;
            case "Running":
            case "Stopping":
            case "PartiallyStopped":
                return RUNNING;
            case "Stopped":
                return COMPLETED;
        }
        log.info("Current State: {}", state);
        return ERROR;
    }

    private boolean resourceVersionChanged(BatchJob resource) {
        var latestObservedStateVersion = ofNullable(resource.getStatus().getLatestResourceVersion()).orElse(0L);

        var mostRecentStateVersion = ofNullable(this.mostRecentState).map(CustomResource::getMetadata)
                                                                     .map(ObjectMeta::getResourceVersion)
                                                                     .map(Longs::tryParse)
                                                                     .orElse(0L);

        boolean didChange = mostRecentStateVersion > latestObservedStateVersion;

        if (didChange) {
            log.info("FlinkCluster Resource Version changed from {} to {}", latestObservedStateVersion,
                    mostRecentStateVersion);
        }

        return didChange;
    }
}
