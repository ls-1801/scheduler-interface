package de.tuberlin.esi.schedulingreconciler.strategy;

import de.tuberlin.esi.common.crd.NamespacedName;
import de.tuberlin.esi.common.crd.batchjob.BatchJobState;
import de.tuberlin.esi.common.statemachine.Condition;
import de.tuberlin.esi.schedulingreconciler.statemachine.AwaitNumberOfSlotsAvailableCondition;
import de.tuberlin.esi.schedulingreconciler.statemachine.SchedulingCondition;
import de.tuberlin.esi.schedulingreconciler.statemachine.SchedulingContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.mutable.MutableInt;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.Collections.emptySet;
import static java.util.Objects.requireNonNull;
import static java.util.function.Predicate.not;

@Slf4j
public class QueueBasedStrategy implements SchedulingStrategy {

    private final List<NamespacedName> queue;
    private final SchedulingContext context;

    public QueueBasedStrategy(SchedulingContext context) {
        this.context = context;
        this.queue = requireNonNull(context.getResource().getSpec().getQueueBased());
    }

    private List<NamespacedName> enqueuedJobs() {
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

        log.debug("Enqueued Jobs: {}", requireScheduling);
        return queue.stream().filter(requireScheduling::contains)
                    .collect(Collectors.toList());
    }

    @Override
    public List<NamespacedName> getJobsDistinctInOrder() {
        return new ArrayList<>(new LinkedHashSet<>(queue));
    }

    @Override
    public Map<NamespacedName, Long> getReplication() {
        return queue.stream().collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
    }

    @Override
    public Set<Condition<SchedulingContext>> awaitSlotsConditions(String conditionName) {
        if (!SchedulingCondition.AWAIT_NUMBER_OF_SLOTS_CONDITION.equals(conditionName)) {
            return emptySet();
        }


        var stream = this.enqueuedJobs().stream().distinct();
        var replications = getReplication();

        // We Increment the amount of slots required for each Job.
        // The first job only needs the required amount of slots for itself.
        // However, the second job could be scheduled in the same cycle if enough for both slots are available.
        // Subsequent cycle will reset the amount of slots required. So if the third job cannot be scheduled because it
        // required 4 + 4 + 4 Slots once the first two jobs are scheduled this method will be called again and only 4
        // slots are required now.
        var slotCounter = new MutableInt(0);
        return stream.map(j -> new AwaitNumberOfSlotsAvailableCondition(j,
                             slotCounter.addAndGet(replications.get(j).intValue())))
                     .map(c -> (Condition<SchedulingContext>) c)
                     .collect(Collectors.toSet());
    }

    @Override
    public Set<Integer> getSlotsForJob(NamespacedName name) {
        return new QueueBasedSlotSelectionStrategy(queue, context.getFreeSlots(), context.getAlreadyScheduledJobs())
                .getSlotsForJob(name);
    }

    @Override
    public List<NamespacedName> orderRunnableJobs(Set<NamespacedName> runnableJobs) {
        var alreadyScheduled = context.getAlreadyScheduledJobs();
        var queueWithoutAlreadyRunning =
                getJobsDistinctInOrder().stream().filter(not(alreadyScheduled::contains)).collect(Collectors.toList());
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
    public boolean isQueueEmpty() {
        return enqueuedJobs().isEmpty();
    }

    @Override
    public boolean allowedToSkipJobs() {
        return false;
    }
}
