package de.tuberlin.batchjoboperator.schedulingreconciler.statemachine;

import de.tuberlin.batchjoboperator.common.NamespacedName;
import de.tuberlin.batchjoboperator.common.StateMachineContext;
import de.tuberlin.batchjoboperator.common.crd.batchjob.BatchJob;
import de.tuberlin.batchjoboperator.common.crd.scheduling.Scheduling;
import de.tuberlin.batchjoboperator.common.crd.scheduling.SchedulingJobState;
import de.tuberlin.batchjoboperator.common.crd.slots.Slot;
import de.tuberlin.batchjoboperator.common.crd.slots.SlotIDsAnnotationString;
import de.tuberlin.batchjoboperator.common.crd.slots.SlotOccupationStatus;
import de.tuberlin.batchjoboperator.common.crd.slots.SlotState;
import de.tuberlin.batchjoboperator.schedulingreconciler.strategy.QueueBasedStrategy;
import de.tuberlin.batchjoboperator.schedulingreconciler.strategy.SchedulingStrategy;
import de.tuberlin.batchjoboperator.schedulingreconciler.strategy.SlotBasedStrategy;
import io.fabric8.kubernetes.client.KubernetesClient;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;
import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedHashSet;
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

@Slf4j
@RequiredArgsConstructor
public class SchedulingContext implements StateMachineContext {
    @Getter
    private final Scheduling resource;

    @Getter
    private final KubernetesClient client;

    @Getter
    private final Set<NamespacedName> jobsSubmittedDuringCurrentCycle = new LinkedHashSet<>();
    private final HashMap<NamespacedName, BatchJob> jobCache = new HashMap<>();
    private SchedulingStrategy strategy;
    private Slot slot;

    @Nullable
    public BatchJob getJob(NamespacedName jobName) {
        return jobCache.computeIfAbsent(jobName, (n) ->
                client.resources(BatchJob.class).inNamespace(n.getNamespace()).withName(n.getName()).get()
        );
    }

    public List<NamespacedName> getAllJobs() {
        return getStrategy().getJobsDistinctInOrder();
    }


    private static final Set<SchedulingJobState.SchedulingJobStateEnum> alreadyScheduledStates = Set.of(
            SchedulingJobState.SchedulingJobStateEnum.Submitted,
            SchedulingJobState.SchedulingJobStateEnum.Scheduled,
            SchedulingJobState.SchedulingJobStateEnum.Completed
    );

    public Set<NamespacedName> getAlreadyScheduledJobs() {
        return getResource().getStatus().getJobStates().stream()
                            .filter(js -> alreadyScheduledStates.contains(js.getState()))
                            .map(SchedulingJobState::getName)
                            .collect(Collectors.toSet());
    }


    private Set<Integer> freeSlots;

    public Set<Integer> getFreeSlots() {
        if (freeSlots == null) {
            this.freeSlots = getSlots().getStatus().getSlots().stream().filter(occ -> occ.getState() == SlotState.FREE)
                                       .map(SlotOccupationStatus::getPosition)
                                       .collect(Collectors.toSet());
        }

        return freeSlots;
    }

    public Slot getSlots() {
        return getSlots(false);
    }

    public Slot getSlots(boolean forceUpdate) {
        if (slot == null || forceUpdate) {
            this.slot =
                    getNullSafe(() ->
                            client.resources(Slot.class)
                                  .inNamespace(resource.getSpec().getSlots().getNamespace())
                                  .withName(resource.getSpec().getSlots().getName())
                                  .get()
                    ).orElse(null);
        }

        return slot;
    }

    public SchedulingStrategy getStrategy() {
        if (strategy == null) {
            strategy = resource.getSpec().getQueueBased() != null ? new QueueBasedStrategy(this) :
                    new SlotBasedStrategy(this);
        }

        return strategy;
    }

    public void acquireJob(NamespacedName key) {
        log.info("Acquire Job {}", key);
        var updatedJob =
                client.resources(BatchJob.class).inNamespace(key.getNamespace()).withName(key.getName())
                      .edit((job) -> {
                          if (job.getMetadata().getLabels() == null) {
                              job.getMetadata().setLabels(new HashMap<>());
                          }

                          var resourceName = NamespacedName.of(resource);
                          job.getMetadata().getLabels().put(ACTIVE_SCHEDULING_LABEL_NAME, resourceName.getName());
                          job.getMetadata().getLabels()
                             .put(ACTIVE_SCHEDULING_LABEL_NAMESPACE, resourceName.getNamespace());

                          return job;
                      });

        this.jobCache.put(key, updatedJob);
    }

    public void acquireSlot(NamespacedName name) {
        log.info("Acquire Slot {}", name);

        this.slot = client.resources(Slot.class).inNamespace(name.getNamespace()).withName(name.getName())
                          .edit((slots) -> {
                              if (slots.getMetadata().getLabels() == null) {
                                  slots.getMetadata().setLabels(new HashMap<>());
                              }

                              var resourceName = NamespacedName.of(resource);
                              slots.getMetadata().getLabels()
                                   .put(ACTIVE_SCHEDULING_LABEL_NAME, resourceName.getName());
                              slots.getMetadata().getLabels()
                                   .put(ACTIVE_SCHEDULING_LABEL_NAMESPACE, resourceName.getNamespace());

                              return slots;
                          });
    }

    public void submitJob(NamespacedName name,
                          Set<Integer> slotsUsed) {
        log.info("Requesting Creation for {}", name);

        client.resources(BatchJob.class).inNamespace(name.getNamespace()).withName(name.getName())
              .edit((BatchJob job) -> {
                  job.getMetadata().getLabels().putAll(Map.of(
                          APPLICATION_CREATION_REQUEST_SLOT_IDS,
                          SlotIDsAnnotationString.ofIds(slotsUsed).toString(),
                          APPLICATION_CREATION_REQUEST_REPLICATION,
                          slotsUsed.size() + "",
                          APPLICATION_CREATION_REQUEST_SLOTS_NAME,
                          getSlots().getMetadata().getName(),
                          APPLICATION_CREATION_REQUEST_SLOTS_NAMESPACE,
                          getSlots().getMetadata().getNamespace()
                  ));
                  return job;
              });

        getResource().getStatus().getJobStates();
        getFreeSlots().removeAll(slotsUsed);
        submitJob(name);
    }

    @SneakyThrows
    private void logJobEvent(NamespacedName name, SchedulingJobState.SchedulingJobStateEnum eventType) {

        var jobState = new SchedulingJobState(name, eventType);

        if (resource.getStatus().getJobStates().contains(jobState))
            return;

        resource.getStatus().getJobStates().add(jobState);
        var event = new io.fabric8.kubernetes.api.model.EventBuilder()
                .withNewEventTime().withTime(Instant.now().toString()).endEventTime()
                .withNewInvolvedObject()
                .withName(resource.getMetadata().getName())
                .withNamespace(resource.getMetadata().getNamespace())
                .withApiVersion(resource.getApiVersion())
                .withKind(resource.getKind())
                .withResourceVersion(resource.getMetadata().getResourceVersion())
                .withUid(resource.getMetadata().getUid())
                .endInvolvedObject()
                .withKind("Event")
                .withMessage("Job Status has Changed to: " + jobState)
                .withNewMetadata()
                .withGenerateName(resource.getMetadata().getName() + "-job-event")
                .withNamespace(resource.getMetadata().getNamespace())
                .endMetadata()
                .withReason(jobState.toString())
                .withReportingComponent("Scheduling-Operator")
                .withReportingInstance("THE Scheduling-Operator")
                .withNewSource()
                .withComponent("Scheduling-Operator")
                .endSource()
                .withType("Normal")
                .withAction("State Change")
                .build();

        client.v1().events().inNamespace(resource.getMetadata().getNamespace()).create(event);
    }

    public void jobScheduledEvent(NamespacedName name) {
        logJobEvent(name, SchedulingJobState.SchedulingJobStateEnum.Scheduled);
    }

    public void jobCompletedEvent(NamespacedName name) {
        logJobEvent(name, SchedulingJobState.SchedulingJobStateEnum.Completed);
    }

    private void submitJob(NamespacedName name) {
        logJobEvent(name, SchedulingJobState.SchedulingJobStateEnum.Submitted);
        jobsSubmittedDuringCurrentCycle.add(name);
    }

    public void jobInQueueEvent(NamespacedName name) {
        logJobEvent(name, SchedulingJobState.SchedulingJobStateEnum.InQueue);
    }
}
