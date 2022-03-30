package de.tuberlin.esi.schedulingreconciler.strategy;

import de.tuberlin.esi.common.crd.NamespacedName;
import de.tuberlin.esi.common.statemachine.Condition;
import de.tuberlin.esi.schedulingreconciler.statemachine.SchedulingContext;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface SchedulingStrategy {
    List<NamespacedName> getJobsDistinctInOrder();

    Map<NamespacedName, Long> getReplication();

    Set<Condition<SchedulingContext>> awaitSlotsConditions(String conditionName);

    Set<Integer> getSlotsForJob(NamespacedName job);

    List<NamespacedName> orderRunnableJobs(Set<NamespacedName> jobs);

    boolean isQueueEmpty();

    boolean allowedToSkipJobs();
}

