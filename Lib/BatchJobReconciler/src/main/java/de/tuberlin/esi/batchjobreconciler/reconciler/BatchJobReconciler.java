package de.tuberlin.esi.batchjobreconciler.reconciler;

import de.tuberlin.esi.batchjobreconciler.reconciler.conditions.AwaitCreationRequest;
import de.tuberlin.esi.batchjobreconciler.reconciler.conditions.AwaitEnqueueRequest;
import de.tuberlin.esi.common.crd.batchjob.AbstractBatchJobCondition;
import de.tuberlin.esi.common.crd.batchjob.BatchJob;
import de.tuberlin.esi.common.crd.batchjob.BatchJobState;
import de.tuberlin.esi.common.statemachine.Action;
import de.tuberlin.esi.common.statemachine.Condition;
import de.tuberlin.esi.common.statemachine.UpdateResult;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Pod;
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
import io.javaoperatorsdk.operator.processing.event.ResourceID;
import io.javaoperatorsdk.operator.processing.event.source.EventSource;
import io.javaoperatorsdk.operator.processing.event.source.informer.InformerEventSource;
import io.javaoperatorsdk.operator.processing.event.source.informer.Mappers;
import k8s.flinkoperator.FlinkCluster;
import k8s.sparkoperator.SparkApplication;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static de.tuberlin.esi.batchjobreconciler.reconciler.BatchJobStateMachine.STATE_MACHINE;
import static de.tuberlin.esi.common.constants.CommonConstants.FLINK_POD_LABEL;
import static de.tuberlin.esi.common.constants.CommonConstants.MANAGED_BY_LABEL_NAME;
import static de.tuberlin.esi.common.constants.CommonConstants.MANAGED_BY_LABEL_VALUE;
import static de.tuberlin.esi.common.constants.CommonConstants.SPARK_POD_LABEL;
import static de.tuberlin.esi.common.statemachine.Action.getConditions;
import static de.tuberlin.esi.common.util.General.getNullSafe;
import static de.tuberlin.esi.common.util.General.lastElement;
import static de.tuberlin.esi.common.util.General.requireFirstElement;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.BooleanUtils.isFalse;

@ControllerConfiguration
@RequiredArgsConstructor
public class BatchJobReconciler implements Reconciler<BatchJob>, EventSourceInitializer<BatchJob> {

    private static final Logger log = LoggerFactory.getLogger(BatchJobReconciler.class);


    private final KubernetesClient kubernetesClient;
    private final String namespace;
    private final boolean testmode;


    public BatchJobReconciler(KubernetesClient kubernetesClient, String namespace) {
        this(kubernetesClient, namespace, false);
    }

    public static void releaseRequest(Set<Condition<BatchJobContext>> conditions, BatchJobContext context) {
        log.info("release requested");
        context.removeApplication();
        context.getResource().getStatus().setActiveScheduling(null);
    }

    public static void startRunningEvent(Set<Condition<BatchJobContext>> conditions, BatchJobContext context) {
        context.getResource().getStatus().startSchedulingEvent(context.getResource());
    }

    public static Action<BatchJobContext> stopRunningEvent(boolean succesful) {
        return (Set<Condition<BatchJobContext>> conditions, BatchJobContext context) -> {
            context.getResource().getStatus().stopLatestSchedulingEvent(succesful);
        };
    }

    public static void creationRequest(Set<Condition<BatchJobContext>> conditions, BatchJobContext context) {
        var creationRequestCondition = requireFirstElement(getConditions(conditions, AwaitCreationRequest.class));
        var creationRequest = requireNonNull(creationRequestCondition.getCreationRequest());
        log.debug("Creation Request: {}", creationRequest);
        context.createApplication(creationRequestCondition.getCreationRequest());

        var status = context.getResource().getStatus();
        status.setSlots(creationRequest.getTestbedName());
        status.setSlotIds(creationRequest.getSlotIds());
        status.setReplication(creationRequest.getReplication());
    }

    private Set<ResourceID> associatedPrimaryResources(HasMetadata pod) {
        if (pod.getMetadata().getLabels().containsKey(FLINK_POD_LABEL)) {
            return Collections.singleton(new ResourceID(pod.getMetadata().getLabels().get(FLINK_POD_LABEL), namespace));
        }

        if (pod.getMetadata().getLabels().containsKey(SPARK_POD_LABEL)) {
            return Collections.singleton(new ResourceID(pod.getMetadata().getLabels().get(SPARK_POD_LABEL), namespace));
        }

        return Collections.emptySet();
    }

    @Override
    public List<EventSource> prepareEventSources(EventSourceContext<BatchJob> context) {
        log.info("Watching to Spark and Flink Applications in namespace: {}", namespace);
        SharedIndexInformer<SparkApplication> sparkApplicationSharedIndexInformer =
                kubernetesClient.resources(SparkApplication.class)
                                .inNamespace(namespace)
                                .withLabel(MANAGED_BY_LABEL_NAME, MANAGED_BY_LABEL_VALUE)
                                .runnableInformer(0);

        SharedIndexInformer<FlinkCluster> flinkApplicationSharedIndexInformer =
                kubernetesClient.resources(FlinkCluster.class)
                                .inNamespace(namespace)
                                .withLabel(MANAGED_BY_LABEL_NAME, MANAGED_BY_LABEL_VALUE)
                                .runnableInformer(0);

        SharedIndexInformer<Pod> podSharedIndexInformer = kubernetesClient
                .pods().inNamespace(namespace)
                .runnableInformer(0);


        return List.of(
                new InformerEventSource<>(podSharedIndexInformer, this::associatedPrimaryResources),
                new InformerEventSource<>(sparkApplicationSharedIndexInformer, Mappers.fromOwnerReference(), null,
                        !testmode),
                new InformerEventSource<>(flinkApplicationSharedIndexInformer, Mappers.fromOwnerReference(), null,
                        !testmode)
        );
    }

    @Override
    public DeleteControl cleanup(BatchJob resource, Context context) {
        log.info("Cleaning up for: {}", resource.getMetadata().getName());
        return Reconciler.super.cleanup(resource, context);
    }

    public static void enqueueRequest(Set<Condition<BatchJobContext>> conditions, BatchJobContext context) {
        var condition = requireFirstElement(Action.getConditions(conditions, AwaitEnqueueRequest.class));
        context.getResource().getStatus().setActiveScheduling(condition.getActiveScheduling());
    }

    private void debugLog(BatchJob resource, Context context) {
        log.debug("#".repeat(80));
        log.debug("Reconciling BatchJob: {}", resource.getMetadata().getName());
        log.debug("RV:         {}", resource.getMetadata().getResourceVersion());
        log.debug("LABELS:     {}", resource.getMetadata().getLabels());
        log.debug("Status:     {}", resource.getStatus().getState());
        log.debug("Conditions: {}",
                getNullSafe(() -> resource.getStatus().getConditions().stream()
                                          .map(AbstractBatchJobCondition::getCondition)
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

        if (isFalse(lastAttempt)) {
            // Retry is triggered by throwing an exception during reconciliation
            log.debug("Retry is triggered");
            log.debug("#".repeat(80));
            throw new RuntimeException(newStatus.getError());
        }

        resource.getStatus().setConditions(newStatus.getNewConditions().stream()
                                                    .map(AbstractBatchJobCondition.class::cast)
                                                    .collect(Collectors.toSet()));
        resource.getStatus().setState(BatchJobState.valueOf(newStatus.getNewState()));
        resource.getStatus().setProblems(List.of(newStatus.getError()));
        log.debug("Last Retry Attempt updating state to FailedState");
        log.debug("#".repeat(80));
        return UpdateControl.updateStatus(resource);
    }

    @Override
    public UpdateControl<BatchJob> reconcile(BatchJob resource, Context context) {
        debugLog(resource, context);

        var machineContext = new BatchJobContext(resource, kubernetesClient);
        var provider = BatchJobConditionProvider.formResource(resource, machineContext);
        var stateName = getNullSafe(() -> resource.getStatus().getState().name()).orElse(null);


        var statusUpdates = STATE_MACHINE.runMachineUntilItStops(stateName, provider, machineContext);

        var newStatus = lastElement(statusUpdates);

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
                                                    .map(AbstractBatchJobCondition.class::cast)
                                                    .collect(Collectors.toSet()));
        resource.getStatus().setState(BatchJobState.valueOf(newStatus.getNewState()));

        log.debug("StateMachine did advance to");
        log.debug("New State: {}", newStatus.getNewState());
        log.debug("New Conditions: {}", resource.getStatus().getConditions().stream().
                                                map(AbstractBatchJobCondition::getCondition)
                                                .collect(Collectors.toSet()));
        log.debug("#".repeat(80));

        return UpdateControl.updateStatus(resource);
    }
}