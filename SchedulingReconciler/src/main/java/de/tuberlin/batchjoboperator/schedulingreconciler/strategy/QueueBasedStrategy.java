package de.tuberlin.batchjoboperator.schedulingreconciler.strategy;

import de.tuberlin.batchjoboperator.common.Condition;
import de.tuberlin.batchjoboperator.common.NamespacedName;
import de.tuberlin.batchjoboperator.common.crd.batchjob.BatchJobState;
import de.tuberlin.batchjoboperator.schedulingreconciler.statemachine.AwaitNumberOfSlotsAvailableCondition;
import de.tuberlin.batchjoboperator.schedulingreconciler.statemachine.SchedulingContext;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

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
    public Set<Condition<SchedulingContext>> awaitSlotsConditions() {
        var first = this.enqueuedJobs().stream().findFirst();

        var replications = getReplication();

        return first.map(j -> new AwaitNumberOfSlotsAvailableCondition(j, replications.get(j).intValue()))
                    .map(c -> (Condition<SchedulingContext>) c)
                    .map(Collections::singleton)
                    .orElseGet(Collections::emptySet);

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
