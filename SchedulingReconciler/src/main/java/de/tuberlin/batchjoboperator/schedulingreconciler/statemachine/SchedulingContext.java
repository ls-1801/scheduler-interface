package de.tuberlin.batchjoboperator.schedulingreconciler.statemachine;

import de.tuberlin.batchjoboperator.common.crd.NamespacedName;
import de.tuberlin.batchjoboperator.common.crd.batchjob.BatchJob;
import de.tuberlin.batchjoboperator.common.crd.batchjob.BatchJobState;
import de.tuberlin.batchjoboperator.common.crd.batchjob.CreationRequest;
import de.tuberlin.batchjoboperator.common.crd.scheduling.Scheduling;
import de.tuberlin.batchjoboperator.common.crd.scheduling.SchedulingJobState;
import de.tuberlin.batchjoboperator.common.crd.slots.Slot;
import de.tuberlin.batchjoboperator.common.crd.slots.SlotOccupationStatus;
import de.tuberlin.batchjoboperator.common.crd.slots.SlotState;
import de.tuberlin.batchjoboperator.common.statemachine.StateMachineContext;
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
import java.util.Set;
import java.util.stream.Collectors;

import static de.tuberlin.batchjoboperator.common.constants.SchedulingConstants.ACTIVE_SCHEDULING_LABEL_NAME;
import static de.tuberlin.batchjoboperator.common.constants.SchedulingConstants.ACTIVE_SCHEDULING_LABEL_NAMESPACE;
import static de.tuberlin.batchjoboperator.common.crd.NamespacedName.getName;
import static de.tuberlin.batchjoboperator.common.crd.NamespacedName.getNamespace;
import static de.tuberlin.batchjoboperator.common.util.General.getNullSafe;

@Slf4j
@RequiredArgsConstructor
public class SchedulingContext implements StateMachineContext {
    @Getter
    private final Scheduling resource;

    private static final Set<SchedulingJobState.SchedulingJobStateEnum> alreadyScheduledStates = Set.of(
            SchedulingJobState.SchedulingJobStateEnum.SUBMITTED,
            SchedulingJobState.SchedulingJobStateEnum.SCHEDULED,
            SchedulingJobState.SchedulingJobStateEnum.COMPLETED
    );

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

    private int schedulingRetryCounter = 0;

    public Set<NamespacedName> getAlreadyScheduledJobs() {
        return getResource().getStatus().getJobStates().stream()
                            .filter(js -> alreadyScheduledStates.contains(js.getState()))
                            .map(SchedulingJobState::getName)
                            .collect(Collectors.toSet());
    }


    private Set<Integer> freeSlots;

    public Set<Integer> getFreeSlots() {
        if (freeSlots == null) {
            this.freeSlots = getTestbed().getStatus().getSlots().stream()
                                         .filter(occ -> occ.getState() == SlotState.FREE)
                                         .map(SlotOccupationStatus::getPosition)
                                         .collect(Collectors.toSet());
        }

        return freeSlots;
    }

    public Slot getTestbed() {
        return getTestbed(false);
    }

    public Slot getTestbed(boolean forceUpdate) {
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

    public void acquireJob(NamespacedName jobName) {
        log.info("Acquire Job {}", jobName);
        this.acquiringJobEvent(jobName);
        var resourceName = NamespacedName.of(resource);
        var updatedJob =
                client.resources(BatchJob.class).inNamespace(jobName.getNamespace()).withName(jobName.getName())
                      .edit(job -> {
                          var activeScheduling = job.getStatus().getActiveScheduling();

                          if (job.getStatus().getState() == BatchJobState.FailedState) {
                              throw new JobInFailedStateException(job);
                          }

                          if (activeScheduling != null && !activeScheduling.equals(resourceName)) {
                              throw new JobClaimedByAnotherSchedulingException(jobName, activeScheduling);
                          }

                          job.getSpec().setActiveScheduling(resourceName);
                          return job;
                      });

        this.jobCache.put(jobName, updatedJob);
    }

    public void acquireSlot(NamespacedName testedName) {
        log.info("Acquire Testbed {}", testedName);
        var schedulingName = NamespacedName.of(resource);
        this.slot = client.resources(Slot.class).inNamespace(testedName.getNamespace()).withName(testedName.getName())
                          .edit((editSlots) -> {
                              if (editSlots.getMetadata().getLabels() == null) {
                                  editSlots.getMetadata().setLabels(new HashMap<>());
                              }

                              if (editSlots.getMetadata().getLabels().containsKey(ACTIVE_SCHEDULING_LABEL_NAME) &&
                                      editSlots.getMetadata().getLabels().containsKey(ACTIVE_SCHEDULING_LABEL_NAMESPACE)
                              ) {
                                  var activeScheduling = new NamespacedName(
                                          editSlots.getMetadata().getLabels().getOrDefault(ACTIVE_SCHEDULING_LABEL_NAME,
                                                  ""),
                                          editSlots.getMetadata().getLabels()
                                                   .getOrDefault(ACTIVE_SCHEDULING_LABEL_NAMESPACE, "")
                                  );
                                  if (!activeScheduling.equals(schedulingName)) {
                                      throw new JobClaimedByAnotherSchedulingException(testedName, activeScheduling);
                                  }
                              }

                              editSlots.getMetadata().getLabels()
                                       .put(ACTIVE_SCHEDULING_LABEL_NAME, schedulingName.getName());
                              editSlots.getMetadata().getLabels()
                                       .put(ACTIVE_SCHEDULING_LABEL_NAMESPACE, schedulingName.getNamespace());

                              return editSlots;
                          });
    }

    public void submitJob(NamespacedName name,
                          Set<Integer> slotsUsed) {
        log.info("Requesting Creation for {}", name);
        var resourceName = NamespacedName.of(resource);
        client.resources(BatchJob.class).inNamespace(name.getNamespace()).withName(name.getName())
              .edit((BatchJob job) -> {
                  var activeScheduling = job.getSpec().getActiveScheduling();
                  if (activeScheduling == null || !activeScheduling.equals(resourceName)) {
                      throw new JobClaimedByAnotherSchedulingException(name, activeScheduling);
                  }

                  var request = new CreationRequest(slotsUsed, NamespacedName.of(slot), slotsUsed.size());
                  var currentRequest = job.getSpec().getCreationRequest();
                  if (currentRequest != null && !request.equals(currentRequest)) {
                      throw new JobClaimedByAnotherSchedulingException(name, activeScheduling);
                  }

                  job.getSpec().setCreationRequest(request);

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
        logJobEvent(name, SchedulingJobState.SchedulingJobStateEnum.SCHEDULED);
    }

    public void jobCompletedEvent(NamespacedName name) {
        logJobEvent(name, SchedulingJobState.SchedulingJobStateEnum.COMPLETED);
    }

    private void submitJob(NamespacedName name) {
        logJobEvent(name, SchedulingJobState.SchedulingJobStateEnum.SUBMITTED);
        jobsSubmittedDuringCurrentCycle.add(name);
    }

    public void jobInQueueEvent(NamespacedName name) {
        logJobEvent(name, SchedulingJobState.SchedulingJobStateEnum.IN_QUEUE);
    }

    public void acquiringJobEvent(NamespacedName name) {
        logJobEvent(name, SchedulingJobState.SchedulingJobStateEnum.ACQUIRING);
    }

    public int schedulingRetryCounterIncAndGet() {
        return ++schedulingRetryCounter;
    }

    public void releaseJobIfClaimed(NamespacedName name) {
        client.resources(BatchJob.class)
              .inNamespace(name.getNamespace())
              .withName(name.getName())
              .edit(editJob -> {
                  var activeScheduling = editJob.getStatus().getActiveScheduling();
                  if (NamespacedName.of(resource).equals(activeScheduling)) {
                      editJob.getSpec().setActiveScheduling(null);
                      editJob.getSpec().setCreationRequest(null);
                  }

                  return editJob;
              });
    }

    public void releaseTestbedIfClaimed(NamespacedName name) {
        client.resources(Slot.class).inNamespace(name.getNamespace()).withName(name.getName())
              .edit(editTestbed -> {
                  if (editTestbed.getMetadata().getLabels() == null ||
                          !editTestbed.getMetadata().getLabels().getOrDefault(ACTIVE_SCHEDULING_LABEL_NAME, "")
                                      .equals(getName(resource)) ||
                          !editTestbed.getMetadata().getLabels().getOrDefault(ACTIVE_SCHEDULING_LABEL_NAMESPACE, "")
                                      .equals(getNamespace(resource))
                  ) {
                      return editTestbed;
                  }

                  editTestbed.getMetadata().getLabels().remove(ACTIVE_SCHEDULING_LABEL_NAME);
                  editTestbed.getMetadata().getLabels().remove(ACTIVE_SCHEDULING_LABEL_NAMESPACE);
                  return editTestbed;
              });
    }

}
