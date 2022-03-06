package de.tuberlin.batchjoboperator.schedulingreconciler.reconciler;

import de.tuberlin.batchjoboperator.common.NamespacedName;
import de.tuberlin.batchjoboperator.common.crd.batchjob.BatchJob;
import de.tuberlin.batchjoboperator.common.crd.slots.Slot;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface SchedulingStrategy {
    List<NamespacedName> getJobsDistinctInOrder();

    Map<NamespacedName, Long> getReplication();

    void initialConditions(SchedulingJobConditions conditions, Slot slots, List<BatchJob> jobs);

    Set<Integer> getSlotsForJob(NamespacedName job, Set<Integer> freeSlots, Set<NamespacedName> slotsReserved);

    List<NamespacedName> orderRunnableJobs(Set<NamespacedName> jobs, Set<NamespacedName> slotsReserved);

    boolean allowedToSkipJobs();
}

