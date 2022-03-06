package de.tuberlin.batchjoboperator.schedulingreconciler.strategy;

import de.tuberlin.batchjoboperator.common.Condition;
import de.tuberlin.batchjoboperator.common.NamespacedName;
import de.tuberlin.batchjoboperator.common.crd.batchjob.BatchJobState;
import de.tuberlin.batchjoboperator.common.crd.scheduling.SlotScheduling;
import de.tuberlin.batchjoboperator.common.crd.scheduling.SlotSchedulingItem;
import de.tuberlin.batchjoboperator.common.crd.scheduling.SlotSchedulingMode;
import de.tuberlin.batchjoboperator.schedulingreconciler.statemachine.AwaitSlotsAvailableCondition;
import de.tuberlin.batchjoboperator.schedulingreconciler.statemachine.SchedulingContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
public class SlotBasedStrategy implements SchedulingStrategy {

    private final SlotScheduling spec;


    private final SchedulingContext context;

    public SlotBasedStrategy(SchedulingContext context) {
        this.context = context;
        this.spec = Objects.requireNonNull(context.getResource().getSpec().getSlotBased());
    }

    private List<SlotSchedulingItem> enqueuedJobs() {
        var requireScheduling = context.getAllJobs().stream()
                                       // Job has to be in the InQueue State
                                       .filter(jobName ->
                                               context.getJob(jobName).getStatus()
                                                      .getState() == BatchJobState.InQueueState
                                       )
                                       // And not been scheduled during the current Cycle
                                       .filter(jobName -> !context.getJobsSubmittedDuringCurrentCycle()
                                                                  .contains(jobName))
                                       .collect(Collectors.toSet());

        return spec.getJobs().stream()
                   .filter(item -> requireScheduling.contains(item.getName()))
                   .collect(Collectors.toList());
    }

    @Override
    public List<NamespacedName> getJobsDistinctInOrder() {
        var names = spec.getJobs().stream().map(SlotSchedulingItem::getName)
                        .collect(Collectors.toList());

        if (names.size() != new HashSet<>(names).size()) {
            throw new RuntimeException("SlotScheduling needs distinct Jobs");
        }

        return names;
    }

    @Override
    public Map<NamespacedName, Long> getReplication() {
        return spec.getJobs().stream().collect(Collectors.toMap(
                SlotSchedulingItem::getName,
                j -> Long.valueOf(j.getSlotIds().size())));
    }

    @Override
    public Set<Condition<SchedulingContext>> awaitSlotsConditions() {
        var stream = enqueuedJobs().stream()
                                   .map(j -> new AwaitSlotsAvailableCondition(j.getName(), j.getSlotIds()))
                                   .map(c -> (Condition<SchedulingContext>) c);

        if (spec.getMode() == SlotSchedulingMode.STRICT) {
            return stream.findFirst().map(Collections::singleton).orElseGet(Collections::emptySet);
        }

        return stream.collect(Collectors.toSet());
    }


    @Override
    public Set<Integer> getSlotsForJob(NamespacedName name) {
        var jobMap = spec.getJobs().stream().collect(
                Collectors.toMap(SlotSchedulingItem::getName, SlotSchedulingItem::getSlotIds));

        if (!jobMap.containsKey(name)) {
            log.error("Asked to find Slots for a job that is either not part of the Spec or already reserved");
            return Collections.emptySet();
        }

        if (!context.getFreeSlots().containsAll(jobMap.get(name))) {
            log.error("Condition should have prevented that job from beeing runnable");
            return Collections.emptySet();
        }

        return jobMap.get(name);
    }

    @Override
    public List<NamespacedName> orderRunnableJobs(Set<NamespacedName> runnableJobs) {
        var alreadyScheduledJobs = context.getAlreadyScheduledJobs();
        var queueWithoutAlreadyRunning =
                getJobsDistinctInOrder().stream().filter(alreadyScheduledJobs::contains).collect(Collectors.toList());

        var listOfRunnableJobs = new ArrayList<NamespacedName>();

        for (var job : queueWithoutAlreadyRunning) {
            if (!runnableJobs.contains(job) && spec.getMode() == SlotSchedulingMode.STRICT)
                break;

            if (!runnableJobs.contains(job))
                continue;

            listOfRunnableJobs.add(job);
        }

        return listOfRunnableJobs;
    }

    @Override
    public boolean isQueueEmpty() {
        return enqueuedJobs().isEmpty();
    }

    @Override
    public boolean allowedToSkipJobs() {
        return spec.getMode() == SlotSchedulingMode.RELAXED;
    }
}
