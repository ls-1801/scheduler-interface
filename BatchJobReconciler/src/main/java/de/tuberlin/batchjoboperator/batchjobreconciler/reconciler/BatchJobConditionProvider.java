package de.tuberlin.batchjoboperator.batchjobreconciler.reconciler;

import de.tuberlin.batchjoboperator.batchjobreconciler.reconciler.conditions.BatchJobCondition;
import de.tuberlin.batchjoboperator.common.Condition;
import de.tuberlin.batchjoboperator.common.ConditionProvider;
import de.tuberlin.batchjoboperator.common.crd.batchjob.BatchJob;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

class BatchJobConditionProvider implements ConditionProvider<BatchJobContext> {
    private final Map<String, Condition<BatchJobContext>> conditionMap;
    private final BatchJobContext context;

    private BatchJobConditionProvider(Map<String, Condition<BatchJobContext>> conditionMap, BatchJobContext context) {
        this.conditionMap = conditionMap;
        this.context = context;
    }

    public static BatchJobConditionProvider formResource(BatchJob batchJob, BatchJobContext context) {
        var map = batchJob.getStatus().getConditions().stream()
                          .map(a -> (Condition<BatchJobContext>) a)
                          .collect(Collectors.toMap(c -> c.getCondition(), Function.identity()));
        return new BatchJobConditionProvider(map, context);
    }

    public void updateConditions(BatchJob batchJob) {

    }


    @Override
    public Condition<BatchJobContext> getCondition(String conditionName) {
        return conditionMap.computeIfAbsent(conditionName, (name) -> {
            var condition = BatchJobCondition.constructors.get(conditionName).get();
            condition.initialize(context);
            return condition;
        });
    }
}
