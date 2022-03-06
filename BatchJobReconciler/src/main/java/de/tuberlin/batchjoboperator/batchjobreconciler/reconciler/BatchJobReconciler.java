package de.tuberlin.batchjoboperator.batchjobreconciler.reconciler;

import de.tuberlin.batchjoboperator.batchjobreconciler.reconciler.conditions.AwaitCreationRequest;
import de.tuberlin.batchjoboperator.batchjobreconciler.reconciler.conditions.AwaitReleaseCondition;
import de.tuberlin.batchjoboperator.common.Action;
import de.tuberlin.batchjoboperator.common.Condition;
import de.tuberlin.batchjoboperator.common.UpdateResult;
import de.tuberlin.batchjoboperator.common.crd.batchjob.AbstractBatchJobCondition;
import de.tuberlin.batchjoboperator.common.crd.batchjob.BatchJob;
import de.tuberlin.batchjoboperator.common.crd.batchjob.BatchJobState;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.informers.SharedIndexInformer;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.ControllerConfiguration;
import io.javaoperatorsdk.operator.api.reconciler.DeleteControl;
import io.javaoperatorsdk.operator.api.reconciler.EventSourceContext;
import io.javaoperatorsdk.operator.api.reconciler.EventSourceInitializer;
import io.javaoperatorsdk.operator.api.reconciler.Reconciler;
import io.javaoperatorsdk.operator.api.reconciler.RetryInfo;
import io.javaoperatorsdk.operator.api.reconciler.UpdateControl;
import io.javaoperatorsdk.operator.processing.event.source.EventSource;
import io.javaoperatorsdk.operator.processing.event.source.informer.InformerEventSource;
import io.javaoperatorsdk.operator.processing.event.source.informer.Mappers;
import k8s.flinkoperator.FlinkCluster;
import k8s.sparkoperator.SparkApplication;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static de.tuberlin.batchjoboperator.batchjobreconciler.reconciler.BatchJobStateMachine.STATE_MACHINE;
import static de.tuberlin.batchjoboperator.common.Action.getCondition;
import static de.tuberlin.batchjoboperator.common.constants.CommonConstants.MANAGED_BY_LABEL_NAME;
import static de.tuberlin.batchjoboperator.common.constants.CommonConstants.MANAGED_BY_LABEL_VALUE;
import static de.tuberlin.batchjoboperator.common.util.General.getNullSafe;

/**
 * A very simple sample controller that creates a service with a label.
 */
@ControllerConfiguration(generationAwareEventProcessing = false)
@RequiredArgsConstructor
public class BatchJobReconciler implements Reconciler<BatchJob>, EventSourceInitializer<BatchJob> {

    private static final Logger log = LoggerFactory.getLogger(BatchJobReconciler.class);

    private final KubernetesClient kubernetesClient;

    public static void releaseRequest(Set<Condition<BatchJobContext>> conditions, BatchJobContext context) {
        log.info("release requested");
        var releaseCondition = getCondition(conditions, AwaitReleaseCondition.class).get();
        context.removeApplication(releaseCondition.getName());
    }

    public static void startRunningEvent(Set<Condition<BatchJobContext>> conditions, BatchJobContext context) {
        context.getResource().getStatus().startSchedulingEvent(context.getResource().getMetadata());
    }

    public static Action<BatchJobContext> stopRunningEvent(boolean succesful) {
        return (Set<Condition<BatchJobContext>> conditions, BatchJobContext context) -> {
            context.getResource().getStatus().stopLatestSchedulingEvent(succesful);
        };
    }

    public static void creationRequest(Set<Condition<BatchJobContext>> conditions, BatchJobContext context) {
        var creationRequestCondition = getCondition(conditions, AwaitCreationRequest.class).get();
        log.info("Creation Request: {}", creationRequestCondition.getCreationRequest());
        context.createApplication(Objects.requireNonNull(creationRequestCondition.getCreationRequest()));
    }

    @Override
    public List<EventSource> prepareEventSources(EventSourceContext<BatchJob> context) {
        SharedIndexInformer<SparkApplication> sparkApplicationSharedIndexInformer =
                kubernetesClient.resources(SparkApplication.class)
                                .inAnyNamespace()
                                .withLabel(MANAGED_BY_LABEL_NAME, MANAGED_BY_LABEL_VALUE)
                                .runnableInformer(0);
        SharedIndexInformer<FlinkCluster> flinkApplicationSharedIndexInformer =
                kubernetesClient.resources(FlinkCluster.class)
                                .inAnyNamespace()
                                .withLabel(MANAGED_BY_LABEL_NAME, MANAGED_BY_LABEL_VALUE)
                                .runnableInformer(0);


        return List.of(
                new InformerEventSource<>(sparkApplicationSharedIndexInformer, Mappers.fromOwnerReference()),
                new InformerEventSource<>(flinkApplicationSharedIndexInformer, Mappers.fromOwnerReference())
        );
    }

    @Override
    public DeleteControl cleanup(BatchJob resource, Context context) {
        log.info("Cleaning up for: {}", resource.getMetadata().getName());
        return Reconciler.super.cleanup(resource, context);
    }

    private void debugLog(BatchJob resource, Context context) {
        log.debug("#".repeat(80));
        log.debug("Reconciling BatchJob: {}", resource.getMetadata().getName());
        log.debug("RV:         {}", resource.getMetadata().getResourceVersion());
        log.debug("LABELS:     {}", resource.getMetadata().getLabels());
        log.debug("Status:     {}", resource.getStatus().getState());
        log.debug("Conditions: {}",
                getNullSafe(() -> resource.getStatus().getConditions().stream().map(c -> c.getCondition())
                                          .collect(Collectors.toSet())));

        log.debug("RetryCount/LastAttempt: {}", context.getRetryInfo()
                                                       .map(r -> Pair.of(r.getAttemptCount(), r.isLastAttempt()))
                                                       .orElse(null));
    }

    private UpdateControl<BatchJob> handleError(BatchJob resource, Context context,
                                                UpdateResult<BatchJobContext> newStatus) {

        // Retrying might resolve certain problems, like dealing with outdated resources.
        // Ideally the Controller is idempotent and retrying would not cause any side effects
        // Only if the lastAttempt is reached the status will be updated to the error state, because retrying is most
        // likely not resolving the problem
        var lastAttempt =
                context.getRetryInfo().map(RetryInfo::isLastAttempt).orElse(true);

        log.error("StateMachine encountered an error:\n{}\n", newStatus.getError());

        if (lastAttempt) {
            resource.getStatus().setConditions(newStatus.getNewConditions().stream()
                                                        .map(a -> (AbstractBatchJobCondition) a)
                                                        .collect(Collectors.toSet()));
            resource.getStatus().setState(BatchJobState.valueOf(newStatus.getNewState()));
            log.debug("Last Retry Attempt updating state to FailedState");
            log.debug("#".repeat(80));
            return UpdateControl.updateStatus(resource);
        }

        // Retry is triggered by throwing an exception during reconciliation
        log.debug("Retry is triggered");
        log.debug("#".repeat(80));
        throw new RuntimeException(newStatus.getError());
    }

    @Override
    public UpdateControl<BatchJob> reconcile(BatchJob resource, Context context) {
        debugLog(resource, context);

        var machineContext = new BatchJobContext(resource, kubernetesClient);
        var provider = BatchJobConditionProvider.formResource(resource, machineContext);
        var stateName = getNullSafe(() -> resource.getStatus().getState().name()).orElse(null);


        var statusUpdates = STATE_MACHINE.runMachineUntilItStops(stateName, provider, machineContext);

        var newStatus = CollectionUtils.lastElement(statusUpdates);

        // StateMachine did not advance hence no update is required
        if (newStatus == null) {
            log.debug("StateMachine did not advance");
            log.debug("#".repeat(80));
            return UpdateControl.noUpdate();
        }

        // StateMachine encountered an Error, either from a condition or one of the actions threw an exception
        if (newStatus.isError()) {
            return handleError(resource, context, newStatus);
        }


        // Update State and Conditions
        resource.getStatus().setConditions(newStatus.getNewConditions().stream()
                                                    .map(a -> (AbstractBatchJobCondition) a)
                                                    .collect(Collectors.toSet()));
        resource.getStatus().setState(BatchJobState.valueOf(newStatus.getNewState()));

        log.debug("StateMachine did advance to");
        log.debug("New State: {}", newStatus.getNewState());
        log.debug("New Conditions: {}", resource.getStatus().getConditions().stream().map(c -> c.getCondition())
                                                .collect(Collectors.toSet()));
        log.debug("#".repeat(80));

        return UpdateControl.updateStatus(resource);
    }

}