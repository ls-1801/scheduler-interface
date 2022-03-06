package de.tuberlin.batchjoboperator.schedulingreconciler.reconciler;

import de.tuberlin.batchjoboperator.common.NamespacedName;
import de.tuberlin.batchjoboperator.common.crd.batchjob.BatchJob;
import de.tuberlin.batchjoboperator.common.crd.slots.Slot;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.function.Predicate.not;

@Slf4j
@RequiredArgsConstructor
public class QueueBasedStrategy implements SchedulingStrategy {

    private final List<NamespacedName> queue;

    @Override
    public List<NamespacedName> getJobsDistinctInOrder() {
        return new ArrayList<>(new LinkedHashSet<>(queue));
    }

    @Override
    public Map<NamespacedName, Long> getReplication() {
        return queue.stream().collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
    }

    @Override
    public void initialConditions(SchedulingJobConditions conditions, Slot slots, List<BatchJob> jobs) {
        var order = getJobsDistinctInOrder();
        var replication = getReplication();
        var slotsName = NamespacedName.of(slots);

        for (NamespacedName jobName : order) {
            conditions.addCondition(
                    new AwaitNumberOfSlotsAvailableCondition(
                            jobName,
                            slotsName,
                            replication.get(jobName).intValue()));
        }
    }

    @Override
    public Set<Integer> getSlotsForJob(NamespacedName name, Set<Integer> freeSlots, Set<NamespacedName> slotsReserved) {
        return new QueueBasedSlotSelectionStrategy(queue, freeSlots, slotsReserved)
                .getSlotsForJob(name);
    }

    @Override
    public List<NamespacedName> orderRunnableJobs(Set<NamespacedName> runnableJobs, Set<NamespacedName> slotsReserved) {
        var queueWithoutAlreadyRunning =
                getJobsDistinctInOrder().stream().filter(not(slotsReserved::contains)).collect(Collectors.toList());
        log.info("Runnable jobs: {}", runnableJobs);
        var listOfRunnableJobs = new ArrayList<NamespacedName>();

        for (var job : queueWithoutAlreadyRunning) {
            if (!runnableJobs.contains(job))
                break;

            listOfRunnableJobs.add(job);
        }
        log.info("Runnable jobs: {}", listOfRunnableJobs);
        return listOfRunnableJobs;
    }

    @Override
    public boolean allowedToSkipJobs() {
        return false;
    }
}
