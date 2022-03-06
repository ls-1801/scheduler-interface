package de.tuberlin.batchjoboperator.schedulingreconciler.reconciler;

import de.tuberlin.batchjoboperator.common.NamespacedName;
import de.tuberlin.batchjoboperator.common.crd.batchjob.BatchJob;
import de.tuberlin.batchjoboperator.common.crd.scheduling.Scheduling;
import de.tuberlin.batchjoboperator.common.crd.scheduling.SchedulingStatusState;
import de.tuberlin.batchjoboperator.common.crd.slots.Slot;
import de.tuberlin.batchjoboperator.common.crd.slots.SlotIDsAnnotationString;
import de.tuberlin.batchjoboperator.common.crd.slots.SlotOccupationStatus;
import de.tuberlin.batchjoboperator.common.crd.slots.SlotState;
import de.tuberlin.batchjoboperator.common.crd.slots.SlotsStatusState;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.informers.SharedIndexInformer;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.ControllerConfiguration;
import io.javaoperatorsdk.operator.api.reconciler.DeleteControl;
import io.javaoperatorsdk.operator.api.reconciler.EventSourceContext;
import io.javaoperatorsdk.operator.api.reconciler.EventSourceInitializer;
import io.javaoperatorsdk.operator.api.reconciler.Reconciler;
import io.javaoperatorsdk.operator.api.reconciler.UpdateControl;
import io.javaoperatorsdk.operator.processing.event.ResourceID;
import io.javaoperatorsdk.operator.processing.event.source.EventSource;
import io.javaoperatorsdk.operator.processing.event.source.informer.InformerEventSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static de.tuberlin.batchjoboperator.common.constants.SchedulingConstants.ACTIVE_SCHEDULING_LABEL_NAME;
import static de.tuberlin.batchjoboperator.common.constants.SchedulingConstants.ACTIVE_SCHEDULING_LABEL_NAMESPACE;
import static de.tuberlin.batchjoboperator.common.constants.SchedulingConstants.APPLICATION_CREATION_REQUEST_REPLICATION;
import static de.tuberlin.batchjoboperator.common.constants.SchedulingConstants.APPLICATION_CREATION_REQUEST_SLOTS_NAME;
import static de.tuberlin.batchjoboperator.common.constants.SchedulingConstants.APPLICATION_CREATION_REQUEST_SLOTS_NAMESPACE;
import static de.tuberlin.batchjoboperator.common.constants.SchedulingConstants.APPLICATION_CREATION_REQUEST_SLOT_IDS;
import static de.tuberlin.batchjoboperator.schedulingreconciler.reconciler.SchedulingJobCondition.AWAITING_JOB_COMPLETION;
import static de.tuberlin.batchjoboperator.schedulingreconciler.reconciler.SchedulingJobCondition.AWAITING_JOB_ENQUEUE;
import static de.tuberlin.batchjoboperator.schedulingreconciler.reconciler.SchedulingJobCondition.AWAITING_JOB_START;
import static de.tuberlin.batchjoboperator.schedulingreconciler.reconciler.SchedulingJobCondition.AWAITING_JOB_SUBMISSION;
import static de.tuberlin.batchjoboperator.schedulingreconciler.reconciler.SchedulingJobCondition.AWAITING_NUMBER_OF_SLOTS_AVAILABLE;
import static de.tuberlin.batchjoboperator.schedulingreconciler.reconciler.SchedulingJobCondition.AWAITING_PRECEDING_JOB_SUBMISSION;

@ControllerConfiguration
@RequiredArgsConstructor
@Slf4j
public class SchedulingReconciler implements Reconciler<Scheduling>, EventSourceInitializer<Scheduling> {

    private final KubernetesClient client;

    public static List<NamespacedName> getDistinctJobsInOrder(List<NamespacedName> queue) {
        return new ArrayList<>(new LinkedHashSet<>(queue));
    }

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

    private void addLabelsToSlot(Slot slot, Scheduling resource) {
        if (slot.getMetadata().getLabels() == null) {
            slot.getMetadata().setLabels(new HashMap<>());
        }

        slot.getMetadata().getLabels().put(ACTIVE_SCHEDULING_LABEL_NAME, resource.getMetadata().getName());
        slot.getMetadata().getLabels().put(ACTIVE_SCHEDULING_LABEL_NAMESPACE,
                resource.getMetadata().getNamespace());
        client.resources(Slot.class).inNamespace(slot.getMetadata().getNamespace()).patch(slot);
    }
//
//    private void addLabelsToJob(BatchJob job, Scheduling resource) {
//        if (job.getMetadata().getLabels() == null) {
//            job.getMetadata().setLabels(new HashMap<>());
//        }
//
//        job.getMetadata().getLabels().put(ACTIVE_SCHEDULING_LABEL_NAME, resource.getMetadata().getName());
//        job.getMetadata().getLabels().put(ACTIVE_SCHEDULING_LABEL_NAMESPACE,
//                resource.getMetadata().getNamespace());
//        client.resources(BatchJob.class).inNamespace(job.getMetadata().getNamespace()).patch(job);
//    }

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

                  var size = editJob.getMetadata().getLabels().size();
                  editJob.getMetadata().getLabels().remove(ACTIVE_SCHEDULING_LABEL_NAME);
                  editJob.getMetadata().getLabels().remove(ACTIVE_SCHEDULING_LABEL_NAMESPACE);
                  editJob.getMetadata().getLabels().remove(APPLICATION_CREATION_REQUEST_SLOTS_NAME);
                  editJob.getMetadata().getLabels().remove(APPLICATION_CREATION_REQUEST_SLOTS_NAMESPACE);
                  editJob.getMetadata().getLabels().remove(APPLICATION_CREATION_REQUEST_SLOT_IDS);
                  editJob.getMetadata().getLabels().remove(APPLICATION_CREATION_REQUEST_REPLICATION);

                  if (editJob.getMetadata().getLabels().size() != size) {
                      incrementGeneration(job);
                  }

                  return editJob;
              });


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

    private BatchJob getBatchJob(NamespacedName nn) {
        return client.resources(BatchJob.class).inNamespace(nn.getNamespace()).withName(nn.getName()).get();
    }

    private Slot getSlot(NamespacedName nn) {
        return client.resources(Slot.class).inNamespace(nn.getNamespace()).withName(nn.getName()).get();
    }

    private boolean verifyActiveSchedulingOwnsSlot(Slot slot, Scheduling resource) {
        if (slot.getMetadata().getLabels() == null) {
            slot.getMetadata().setLabels(new HashMap<>());
        }

        var name = slot.getMetadata().getLabels().get(ACTIVE_SCHEDULING_LABEL_NAME);
        var namespace = slot.getMetadata().getLabels().get(ACTIVE_SCHEDULING_LABEL_NAMESPACE);

        if (name == null || namespace == null) {
            addLabelsToSlot(slot, resource);
            return true;
        }

        return name.equals(resource.getMetadata().getName()) && namespace.equals(resource.getMetadata().getNamespace());
    }

    private boolean verifySlotIsReady(Slot slot) {
        var slotState = slot.getStatus().getState();
        return slotState == SlotsStatusState.RUNNING || slotState == SlotsStatusState.SUCCESS;
    }

    private Set<Integer> getFreeSlots(Slot slots) {
        if (slots.getStatus().getSlots() == null)
            return Collections.emptySet();

        return slots.getStatus().getSlots().stream().filter(slot -> slot.getState() == SlotState.FREE)
                    .map(SlotOccupationStatus::getPosition)
                    .collect(Collectors.toSet());
    }

    private List<SlotOccupationStatus> getFreeSlots(Slot slots, Set<Integer> slotIds) {
        if (slots.getStatus().getSlots() == null)
            return Collections.emptyList();

        return slots.getStatus().getSlots().stream()
                    .filter(occ -> slotIds.contains(occ.getPosition())).collect(Collectors.toList());
    }

    private void buildInitialConditions(SchedulingJobConditions conditions,
                                        List<NamespacedName> distinctNamesInOrder) {
        for (var job : distinctNamesInOrder) {
            conditions.addCondition(new AwaitingJobEnqueueCondition(job));
        }
    }

    private SchedulingStrategy getStrategy(Scheduling resource) {
        if (resource.getSpec().getQueueBased() != null)
            return new QueueBasedStrategy(resource.getSpec().getQueueBased());

        throw new RuntimeException("AAA");
    }

    private Optional<String> reconcileInternal(Scheduling resource, SchedulingJobConditions conditions) {
        var slots = getSlot(resource.getSpec().getSlots());
        if (slots == null) {
            return Optional.of("Slots do not exist");
        }
        if (!verifyActiveSchedulingOwnsSlot(slots, resource)) {
            return Optional.of("Not owning the Slot");
        }
        if (!verifySlotIsReady(slots)) {
            return Optional.of("Slots not ready");
        }

        var strategy = getStrategy(resource);
        var jobNames = strategy.getJobsDistinctInOrder();
        var replications = strategy.getReplication();
        var jobs = jobNames.stream().map(this::getBatchJob).collect(Collectors.toList());

        if (conditions.isEmpty()) {
            if (jobs.stream().anyMatch(Objects::isNull)) {
                return Optional.of("Job does not exist");
            }
            buildInitialConditions(conditions, jobNames);
            strategy.initialConditions(conditions, slots, jobs);
        }

        conditions.updateAll(client);

        if (conditions.anyError()) {
            return Optional.of("Job Conditions Error");
        }

        var awaitingEnqueue = conditions.anyWaiting(Set.of(AWAITING_JOB_ENQUEUE));
        if (!awaitingEnqueue.isEmpty()) {
            awaitingEnqueue.forEach(job -> requestEnqueue(job, resource));
            // All fine we just need to wait
            return Optional.empty();
        }

        if (conditions.anyAwaitSubmission()) {
            return Optional.empty();
        }


        log.info("Prerequisites are met");
        // These Jobs can be removed from the Queue when selecting free slots
        var slotsReserved = conditions.withConditions(Set.of(
                AWAITING_JOB_COMPLETION,
                AWAITING_JOB_START,
                AWAITING_JOB_SUBMISSION
        ));

        var freeSlots = getFreeSlots(slots);

        // Given a set of runnable jobs
        // enough/correct slots available
        var runnableJobs = conditions.getRunnable();
        var createdJobs = new HashSet<NamespacedName>();

        for (NamespacedName job : strategy.orderRunnableJobs(runnableJobs, slotsReserved)) {
            Set<Integer> slotIdsForJob = strategy.getSlotsForJob(job, freeSlots, slotsReserved);

            if (slotIdsForJob.size() < replications.get(job)) {
                log.info("Not enough Slots available");

                if (strategy.allowedToSkipJobs()) {
                    continue;
                }
                break;
            }
            freeSlots.removeAll(slotIdsForJob);
            slotsReserved.add(job);

            requestCreation(job, slots, slotIdsForJob, conditions);
            createdJobs.add(job);
        }

        createdJobs.forEach(job -> conditions.addCondition(new AwaitPreviousJobSubmittedCondition(job)));

        return Optional.empty();
    }

    @Override
    public UpdateControl<Scheduling> reconcile(Scheduling resource, Context context) {
        log.info("Reconciling: {}", resource.getMetadata().getName());
        var conditions = SchedulingJobConditions.of(resource);

        var errorOpt = reconcileInternal(resource, conditions);
        conditions.setResourceConditions(resource);

        if (errorOpt.isPresent()) {
            return error(resource, errorOpt.get());
        }

        if (!conditions.anyWaiting(Set.of(AWAITING_JOB_ENQUEUE)).isEmpty()) {
            resource.getStatus().setState(SchedulingStatusState.INITIAL);
            return UpdateControl.updateStatus(resource);
        }

        if (!conditions.anyWaiting(Set.of(
                AWAITING_NUMBER_OF_SLOTS_AVAILABLE,
                AWAITING_PRECEDING_JOB_SUBMISSION,
                AWAITING_JOB_COMPLETION,
                AWAITING_JOB_SUBMISSION,
                AWAITING_JOB_START
        )).isEmpty()) {
            resource.getStatus().setState(SchedulingStatusState.RUNNING);
            return UpdateControl.updateStatus(resource);
        }

        resource.getStatus().setState(SchedulingStatusState.COMPLETED);
        return UpdateControl.updateStatus(resource);
    }

    private boolean labelsHasKeys(HasMetadata hm, String... keys) {
        return labelsHasKeys(hm, Set.of(keys));
    }

    private boolean labelsHasKeys(HasMetadata hm, Set<String> keys) {
        if (hm.getMetadata().getLabels() == null) {
            hm.getMetadata().setLabels(new HashMap<>());
        }

        return keys.stream().allMatch(k -> hm.getMetadata().getLabels().containsKey(k));
    }

    private void incrementGeneration(HasMetadata hm) {
        log.info("Increment Generation");
        var generation = hm.getMetadata().getGeneration();
        if (generation != null) {
            generation += 1;
            log.info("Setting Generation to {}", generation);
            hm.getMetadata().setGeneration(generation);
        }
    }

    private void requestEnqueue(NamespacedName name, Scheduling resource) {
        log.info("Requesting Enqueue for {}", name);
        client.resources(BatchJob.class).inNamespace(name.getNamespace()).withName(name.getName())
              .edit((job) -> {
                  if (labelsHasKeys(job, ACTIVE_SCHEDULING_LABEL_NAME, ACTIVE_SCHEDULING_LABEL_NAMESPACE)) {
                      return job;
                  }
                  job.getMetadata().getLabels().putAll(Map.of(
                          ACTIVE_SCHEDULING_LABEL_NAME, resource.getMetadata().getName(),
                          ACTIVE_SCHEDULING_LABEL_NAMESPACE, resource.getMetadata().getNamespace()
                  ));

                  incrementGeneration(job);

                  return job;
              });


//    ((BatchJob job) -> {

//              });
    }

    private void requestCreation(NamespacedName name,
                                 Slot slots,
                                 Set<Integer> freeSlots,
                                 SchedulingJobConditions conditions) {
        log.info("Requesting Creation for {}", name);
        conditions.removeCondition(name, Set.of(
                AWAITING_PRECEDING_JOB_SUBMISSION,
                AWAITING_NUMBER_OF_SLOTS_AVAILABLE)
        );

        conditions.addCondition(new AwaitingJobSubmissionCondition(name));
        conditions.addCondition(new AwaitingJobStartCondition(name));
        conditions.addCondition(new AwaitingJobCompletionCondition(name));

        client.resources(BatchJob.class).inNamespace(name.getNamespace()).withName(name.getName())
              .edit((BatchJob job) -> {

                  if (labelsHasKeys(job,
                          APPLICATION_CREATION_REQUEST_SLOT_IDS,
                          APPLICATION_CREATION_REQUEST_REPLICATION,
                          APPLICATION_CREATION_REQUEST_SLOTS_NAME,
                          APPLICATION_CREATION_REQUEST_SLOTS_NAMESPACE
                  )) {
                      return job;
                  }

                  job.getMetadata().getLabels().putAll(Map.of(
                          APPLICATION_CREATION_REQUEST_SLOT_IDS,
                          SlotIDsAnnotationString.of(getFreeSlots(slots, freeSlots)).toString(),
                          APPLICATION_CREATION_REQUEST_REPLICATION,
                          freeSlots.size() + "",
                          APPLICATION_CREATION_REQUEST_SLOTS_NAME,
                          slots.getMetadata().getName(),
                          APPLICATION_CREATION_REQUEST_SLOTS_NAMESPACE,
                          slots.getMetadata().getNamespace()
                  ));
                  incrementGeneration(job);
                  return job;
              });
    }


    private UpdateControl<Scheduling> error(Scheduling resource, String msg) {
        log.error(msg);
        resource.getStatus().setState(SchedulingStatusState.ERROR);
        resource.getStatus().setProblems(Collections.singletonList(msg));
        return UpdateControl.updateStatus(resource);
    }

}