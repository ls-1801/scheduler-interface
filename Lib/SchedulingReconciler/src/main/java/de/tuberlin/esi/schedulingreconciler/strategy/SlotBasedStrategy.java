package de.tuberlin.esi.schedulingreconciler.strategy;

import de.tuberlin.esi.common.crd.NamespacedName;
import de.tuberlin.esi.common.crd.batchjob.BatchJobState;
import de.tuberlin.esi.common.crd.scheduling.SlotScheduling;
import de.tuberlin.esi.common.crd.scheduling.SlotSchedulingItem;
import de.tuberlin.esi.common.crd.scheduling.SlotSchedulingMode;
import de.tuberlin.esi.common.statemachine.Condition;
import de.tuberlin.esi.schedulingreconciler.statemachine.AwaitSlotsAvailableCondition;
import de.tuberlin.esi.schedulingreconciler.statemachine.SchedulingCondition;
import de.tuberlin.esi.schedulingreconciler.statemachine.SchedulingContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Collections.emptySet;
import static java.util.function.Predicate.not;

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
                                       .filter(jobName -> !context.getAlreadyScheduledJobs().contains(jobName))
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
    public Set<Condition<SchedulingContext>> awaitSlotsConditions(String conditionName) {
        if (!SchedulingCondition.AWAIT_SLOTS_AVAILABLE_CONDITION.equals(conditionName)) {
            return emptySet();
        }

        var usedSlots = new HashSet<Integer>();
        var conditions = new HashSet<Condition<SchedulingContext>>();
        for (SlotSchedulingItem item : enqueuedJobs()) {
            usedSlots.addAll(item.getSlotIds());
            conditions.add(new AwaitSlotsAvailableCondition(item.getName(), Set.copyOf(usedSlots)));

            /*
            Here the same mechanism is used as it is in the QueueBasedStrategy, where a Job in Strict Mode can only
            run if its predecessor can also be scheduled. However, in relaxed mode this is not necessary, and we can
            clear the used slots.
             */
            if (spec.getMode() == SlotSchedulingMode.RELAXED) {
                usedSlots.clear();
            }
        }

        return Set.copyOf(conditions);
    }


    @Override
    public Set<Integer> getSlotsForJob(NamespacedName name) {
        var jobMap = spec.getJobs().stream().collect(
                Collectors.toMap(SlotSchedulingItem::getName, SlotSchedulingItem::getSlotIds));

        if (!jobMap.containsKey(name)) {
            log.error("Asked to find Slots for a job that is either not part of the Spec or already reserved");
            return emptySet();
        }

        if (!context.getFreeSlots().containsAll(jobMap.get(name))) {
            log.error("Condition should have prevented that job from beeing runnable");
            return emptySet();
        }

        return jobMap.get(name);
    }

    @Override
    public List<NamespacedName> orderRunnableJobs(Set<NamespacedName> runnableJobs) {
        var alreadyScheduledJobs = context.getAlreadyScheduledJobs();
        var queueWithoutAlreadyRunning = getJobsDistinctInOrder().stream()
                                                                 .filter(not(alreadyScheduledJobs::contains))
                                                                 .collect(Collectors.toList());

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
