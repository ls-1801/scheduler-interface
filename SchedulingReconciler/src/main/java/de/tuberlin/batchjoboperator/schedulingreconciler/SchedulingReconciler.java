package de.tuberlin.batchjoboperator.schedulingreconciler;

import de.tuberlin.batchjoboperator.common.crd.NamespacedName;
import de.tuberlin.batchjoboperator.common.crd.batchjob.BatchJob;
import de.tuberlin.batchjoboperator.common.crd.scheduling.AbstractSchedulingJobCondition;
import de.tuberlin.batchjoboperator.common.crd.scheduling.Scheduling;
import de.tuberlin.batchjoboperator.common.crd.scheduling.SchedulingState;
import de.tuberlin.batchjoboperator.common.crd.slots.Slot;
import de.tuberlin.batchjoboperator.common.statemachine.UpdateResult;
import de.tuberlin.batchjoboperator.schedulingreconciler.statemachine.SchedulingConditionProvider;
import de.tuberlin.batchjoboperator.schedulingreconciler.statemachine.SchedulingContext;
import de.tuberlin.batchjoboperator.schedulingreconciler.statemachine.SchedulingStateMachine;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.CollectionUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static de.tuberlin.batchjoboperator.common.constants.SchedulingConstants.ACTIVE_SCHEDULING_LABEL_NAME;
import static de.tuberlin.batchjoboperator.common.constants.SchedulingConstants.ACTIVE_SCHEDULING_LABEL_NAMESPACE;
import static de.tuberlin.batchjoboperator.common.util.General.getNullSafe;
import static java.util.stream.Collectors.toSet;

@ControllerConfiguration
@RequiredArgsConstructor
@Slf4j
public class SchedulingReconciler implements Reconciler<Scheduling>, EventSourceInitializer<Scheduling> {

    private final KubernetesClient client;
    private Set<ResourceID> schedulings = new HashSet<>();

    @Value("${NAMESPACE:default}")
    private final String namespace;

    @Override
    public List<EventSource> prepareEventSources(EventSourceContext<Scheduling> context) {

        log.info("Watching Batch Jobs and Testbeds in namespace: {}", namespace);

        this.schedulings =
                client.resources(Scheduling.class).inNamespace(namespace).list().getItems().stream()
                      .map(ResourceID::fromResource)
                      .collect(toSet());

        SharedIndexInformer<BatchJob> batchJobInformer =
                client.resources(BatchJob.class)
                      .inNamespace(namespace)
                      .runnableInformer(1000);

        SharedIndexInformer<Slot> slotsInformer =
                client.resources(Slot.class)
                      .inNamespace(namespace)
                      .runnableInformer(1000);

        return List.of(
                new InformerEventSource<>(batchJobInformer, (k) -> this.schedulings),
                new InformerEventSource<>(slotsInformer, (k) -> this.schedulings)
        );
    }


    private UpdateControl<Scheduling> handleError(Scheduling resource, Context context,
                                                  UpdateResult<SchedulingContext> newStatus) {

        // Retrying might resolve certain problems, like dealing with outdated resources.
        // Ideally the Controller is idempotent and retrying would not cause any side effects
        // Only if the lastAttempt is reached the status will be updated to the error state, because retrying is most
        // likely not resolving the problem
        var lastAttempt =
                context.getRetryInfo().map(RetryInfo::isLastAttempt).orElse(true);

        log.error("StateMachine encountered an error:\n{}\n", newStatus.getError());

        if (lastAttempt) {
            resource.getStatus().setConditions(newStatus.getNewConditions().stream()
                                                        .map(AbstractSchedulingJobCondition.class::cast)
                                                        .collect(Collectors.toSet()));
            resource.getStatus().setState(SchedulingState.valueOf(newStatus.getNewState()));
            resource.getStatus().setProblems(List.of(newStatus.getError()));
            log.debug("Last Retry Attempt updating state to FailedState");
            log.debug("#".repeat(80));
            return UpdateControl.updateStatus(resource);
        }

        // Retry is triggered by throwing an exception during reconciliation
        log.debug("Retry is triggered");
        log.debug("#".repeat(80));
        throw new RuntimeException(newStatus.getError());
    }

    private void debugLog(Scheduling resource, Context context) {
        log.debug("#".repeat(80));
        log.debug("Reconciling Scheduling: {}", resource.getMetadata().getName());
        log.debug("RV:         {}", resource.getMetadata().getResourceVersion());
        log.debug("Status:     {}", resource.getStatus().getState());
        if (resource.getStatus().getState() == SchedulingState.FailedState) {
            log.debug("Error:     {}", resource.getStatus().getProblems());
        }
        log.debug("Jobs:       {}", resource.getStatus().getJobStates());
        log.debug("Conditions: {}",
                getNullSafe(() -> resource.getStatus().getConditions().stream()
                                          .map(AbstractSchedulingJobCondition::getCondition)
                                          .collect(Collectors.toSet())));

        log.debug("Slot:       {}", context.getSecondaryResource(Slot.class).orElse(null));
        log.debug("Job:        {}", context.getSecondaryResource(BatchJob.class).orElse(null));

        log.debug("RetryCount/LastAttempt: {}", context.getRetryInfo()
                                                       .map(r -> Pair.of(r.getAttemptCount(), r.isLastAttempt()))
                                                       .orElse(null));
    }

    private void addToResources(Scheduling scheduling) {
        this.schedulings.add(ResourceID.fromResource(scheduling));
    }


    @Override
    public UpdateControl<Scheduling> reconcile(Scheduling resource, Context context) {
        debugLog(resource, context);
        addToResources(resource);

        var stateMachineContext = new SchedulingContext(resource, client);
        var conditionProvider = new SchedulingConditionProvider(stateMachineContext);

        var state = getNullSafe(() -> resource.getStatus().getState().name()).orElse(null);

        var update = SchedulingStateMachine.STATE_MACHINE.runMachineUntilItStops(
                state,
                conditionProvider,
                stateMachineContext
        );

        var newStatus = CollectionUtils.lastElement(update);

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
                                                    .map(AbstractSchedulingJobCondition.class::cast)
                                                    .collect(Collectors.toSet()));

        resource.getStatus().setState(SchedulingState.valueOf(newStatus.getNewState()));

        log.debug("StateMachine did advance to");
        log.debug("New State: {}", newStatus.getNewState());
        log.debug("New Conditions: {}", resource.getStatus().getConditions().stream()
                                                .map(AbstractSchedulingJobCondition::getCondition)
                                                .collect(Collectors.toSet()));
        log.debug("#".repeat(80));

        return UpdateControl.updateStatus(resource);

    }

    @Override
    public DeleteControl cleanup(Scheduling resource, Context context) {
        log.info("Cleaning up for: {}", resource.getMetadata().getName());
        var schedulingContext = new SchedulingContext(resource, client);
        client.resources(Slot.class).inNamespace(namespace).withLabels(
                      Map.of(ACTIVE_SCHEDULING_LABEL_NAME,
                              resource.getMetadata().getName(),
                              ACTIVE_SCHEDULING_LABEL_NAMESPACE,
                              resource.getMetadata().getNamespace()))
              .list().getItems().stream()
              .map(NamespacedName::of)
              .forEach(schedulingContext::releaseTestbedIfClaimed);

        client.resources(BatchJob.class).inNamespace(namespace).list()
              .getItems().stream()
              .map(NamespacedName::of)
              .forEach(schedulingContext::releaseJobIfClaimed);

        this.schedulings.remove(ResourceID.fromResource(resource));
        return Reconciler.super.cleanup(resource, context);
    }


}