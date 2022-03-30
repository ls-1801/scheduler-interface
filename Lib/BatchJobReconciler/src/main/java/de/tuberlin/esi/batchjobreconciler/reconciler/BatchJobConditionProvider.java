package de.tuberlin.esi.batchjobreconciler.reconciler;

import de.tuberlin.esi.batchjobreconciler.reconciler.conditions.BatchJobCondition;
import de.tuberlin.esi.common.crd.batchjob.BatchJob;
import de.tuberlin.esi.common.statemachine.Condition;
import de.tuberlin.esi.common.statemachine.ConditionProvider;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
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
                          .collect(Collectors.toMap(Condition::getCondition, Function.identity()));
        return new BatchJobConditionProvider(map, context);
    }

    @Override
    public Set<Condition<BatchJobContext>> getCondition(String conditionName) {
        return Collections.singleton(conditionMap.computeIfAbsent(conditionName, (name) -> {
            var condition = BatchJobCondition.constructors.get(conditionName).get();
            condition.initialize(context);
            return condition;
        }));
    }
}
