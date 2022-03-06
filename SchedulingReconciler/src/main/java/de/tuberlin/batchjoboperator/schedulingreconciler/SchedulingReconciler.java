package de.tuberlin.batchjoboperator.schedulingreconciler;

import de.tuberlin.batchjoboperator.common.UpdateResult;
import de.tuberlin.batchjoboperator.common.crd.batchjob.BatchJob;
import de.tuberlin.batchjoboperator.common.crd.scheduling.AbstractSchedulingJobCondition;
import de.tuberlin.batchjoboperator.common.crd.scheduling.Scheduling;
import de.tuberlin.batchjoboperator.common.crd.scheduling.SchedulingState;
import de.tuberlin.batchjoboperator.common.crd.slots.Slot;
import de.tuberlin.batchjoboperator.schedulingreconciler.statemachine.SchedulingConditionProvider;
import de.tuberlin.batchjoboperator.schedulingreconciler.statemachine.SchedulingContext;
import de.tuberlin.batchjoboperator.schedulingreconciler.statemachine.SchedulingStateMachine;
import io.fabric8.kubernetes.api.model.HasMetadata;
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
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static de.tuberlin.batchjoboperator.common.constants.SchedulingConstants.ACTIVE_SCHEDULING_LABEL_NAME;
import static de.tuberlin.batchjoboperator.common.constants.SchedulingConstants.ACTIVE_SCHEDULING_LABEL_NAMESPACE;
import static de.tuberlin.batchjoboperator.common.constants.SchedulingConstants.APPLICATION_CREATION_REQUEST_REPLICATION;
import static de.tuberlin.batchjoboperator.common.constants.SchedulingConstants.APPLICATION_CREATION_REQUEST_SLOTS_NAME;
import static de.tuberlin.batchjoboperator.common.constants.SchedulingConstants.APPLICATION_CREATION_REQUEST_SLOTS_NAMESPACE;
import static de.tuberlin.batchjoboperator.common.constants.SchedulingConstants.APPLICATION_CREATION_REQUEST_SLOT_IDS;
import static de.tuberlin.batchjoboperator.common.util.General.getNullSafe;

@ControllerConfiguration
@RequiredArgsConstructor
@Slf4j
public class SchedulingReconciler implements Reconciler<Scheduling>, EventSourceInitializer<Scheduling> {

    private final KubernetesClient client;


    public <T extends HasMetadata> Set<ResourceID> fromLabels(T hasMetadata) {
        if (hasMetadata.getMetadata().getLabels() == null) {
            log.warn("{}/{} does not have labels", hasMetadata.getMetadata().getName(), hasMetadata.getKind());
            return Collections.emptySet();
        }

        var name = hasMetadata.getMetadata().getLabels().get(ACTIVE_SCHEDULING_LABEL_NAME);
        var namespace = hasMetadata.getMetadata().getLabels().get(ACTIVE_SCHEDULING_LABEL_NAMESPACE);

        if (name == null || namespace == null) {
            log.warn("{}/{} does not have all labels", hasMetadata.getMetadata().getName(), hasMetadata.getKind());
            return Collections.emptySet();
        }

        return Set.of(new ResourceID(name, namespace));
    }

    @Override
    public List<EventSource> prepareEventSources(EventSourceContext<Scheduling> context) {
        SharedIndexInformer<BatchJob> batchJobInformer =
                client.resources(BatchJob.class)
                      .inAnyNamespace()
                      .runnableInformer(0);

        SharedIndexInformer<Slot> slotsInformer =
                client.resources(Slot.class)
                      .inAnyNamespace()
                      .runnableInformer(0);

        return List.of(
                new InformerEventSource<>(batchJobInformer, this::fromLabels),
                new InformerEventSource<>(slotsInformer, this::fromLabels)
        );
    }

    private void removeLabelsFromSlot(Slot slot) {
        if (slot.getMetadata().getLabels() == null) {
            return;
        }

        slot.getMetadata().getLabels().remove(ACTIVE_SCHEDULING_LABEL_NAME);
        slot.getMetadata().getLabels().remove(ACTIVE_SCHEDULING_LABEL_NAMESPACE);
        client.resources(Slot.class).inNamespace(slot.getMetadata().getNamespace()).patch(slot);
    }



    private void removeLabelsFromJob(BatchJob job) {
        if (job.getMetadata().getLabels() == null) {
            return;
        }

        client.resources(BatchJob.class)
              .inNamespace(job.getMetadata().getNamespace())
              .withName(job.getMetadata().getName())
              .edit((editJob) -> {
                  if (editJob.getMetadata().getLabels() == null) {
                      return editJob;
                  }

                  editJob.getMetadata().getLabels().remove(ACTIVE_SCHEDULING_LABEL_NAME);
                  editJob.getMetadata().getLabels().remove(ACTIVE_SCHEDULING_LABEL_NAMESPACE);
                  editJob.getMetadata().getLabels().remove(APPLICATION_CREATION_REQUEST_SLOTS_NAME);
                  editJob.getMetadata().getLabels().remove(APPLICATION_CREATION_REQUEST_SLOTS_NAMESPACE);
                  editJob.getMetadata().getLabels().remove(APPLICATION_CREATION_REQUEST_SLOT_IDS);
                  editJob.getMetadata().getLabels().remove(APPLICATION_CREATION_REQUEST_REPLICATION);

                  return editJob;
              });


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
                                                        .map(a -> (AbstractSchedulingJobCondition) a)
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
        log.debug("Conditions: {}",
                getNullSafe(() -> resource.getStatus().getConditions().stream().map(c -> c.getCondition())
                                          .collect(Collectors.toSet())));

        log.debug("RetryCount/LastAttempt: {}", context.getRetryInfo()
                                                       .map(r -> Pair.of(r.getAttemptCount(), r.isLastAttempt()))
                                                       .orElse(null));
    }

    @Override
    public UpdateControl<Scheduling> reconcile(Scheduling resource, Context context) {
        debugLog(resource, context);
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
                                                    .map(a -> (AbstractSchedulingJobCondition) a)
                                                    .collect(Collectors.toSet()));

        resource.getStatus().setState(SchedulingState.valueOf(newStatus.getNewState()));

        log.debug("StateMachine did advance to");
        log.debug("New State: {}", newStatus.getNewState());
        log.debug("New Conditions: {}", resource.getStatus().getConditions().stream().map(c -> c.getCondition())
                                                .collect(Collectors.toSet()));
        log.debug("#".repeat(80));

        return UpdateControl.updateStatus(resource);

    }

    @Override
    public DeleteControl cleanup(Scheduling resource, Context context) {
        log.info("Cleaning up for: {}", resource.getMetadata().getName());
        client.resources(Slot.class).inAnyNamespace().withLabels(
                      Map.of(ACTIVE_SCHEDULING_LABEL_NAME,
                              resource.getMetadata().getName(),
                              ACTIVE_SCHEDULING_LABEL_NAMESPACE,
                              resource.getMetadata().getNamespace()))
              .list().getItems().forEach(this::removeLabelsFromSlot);

        client.resources(BatchJob.class).inAnyNamespace().withLabels(
                      Map.of(ACTIVE_SCHEDULING_LABEL_NAME,
                              resource.getMetadata().getName(),
                              ACTIVE_SCHEDULING_LABEL_NAMESPACE,
                              resource.getMetadata().getNamespace()))
              .list().getItems().forEach(this::removeLabelsFromJob);

        return Reconciler.super.cleanup(resource, context);
    }


}