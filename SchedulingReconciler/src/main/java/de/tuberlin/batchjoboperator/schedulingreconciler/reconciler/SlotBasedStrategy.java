package de.tuberlin.batchjoboperator.schedulingreconciler.reconciler;

import de.tuberlin.batchjoboperator.common.NamespacedName;
import de.tuberlin.batchjoboperator.common.crd.batchjob.BatchJob;
import de.tuberlin.batchjoboperator.common.crd.scheduling.SlotScheduling;
import de.tuberlin.batchjoboperator.common.crd.scheduling.SlotSchedulingItem;
import de.tuberlin.batchjoboperator.common.crd.scheduling.SlotSchedulingMode;
import de.tuberlin.batchjoboperator.common.crd.slots.Slot;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
public class SlotBasedStrategy implements SchedulingStrategy {

    private final SlotScheduling spec;

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
    public void initialConditions(SchedulingJobConditions conditions, Slot slots, List<BatchJob> jobs) {
        var replication = getReplication();
        var slotsName = NamespacedName.of(slots);

        for (var item : spec.getJobs()) {
            conditions.addCondition(
                    new AwaitSlotsAvailableCondition(
                            item.getName(),
                            slotsName,
                            item.getSlotIds()));
        }
    }

    @Override
    public Set<Integer> getSlotsForJob(NamespacedName name, Set<Integer> freeSlots, Set<NamespacedName> slotsReserved) {
        var jobMap = spec.getJobs().stream().collect(
                Collectors.toMap(SlotSchedulingItem::getName, SlotSchedulingItem::getSlotIds));

        if (!jobMap.containsKey(name)) {
            log.error("Asked to find Slots for a job that is either not part of the Spec or already reserved");
            return Collections.emptySet();
        }

        if (!freeSlots.containsAll(jobMap.get(name))) {
            log.error("Condition should have prevented that job from beeing runnable");
            return Collections.emptySet();
        }

        return jobMap.get(name);
    }

    @Override
    public List<NamespacedName> orderRunnableJobs(Set<NamespacedName> runnableJobs, Set<NamespacedName> slotsReserved) {
        var queueWithoutAlreadyRunning =
                getJobsDistinctInOrder().stream().filter(slotsReserved::contains).collect(Collectors.toList());

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
    public boolean allowedToSkipJobs() {
        return spec.getMode() == SlotSchedulingMode.RELAXED;
    }
}
