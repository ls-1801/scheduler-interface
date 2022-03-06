package de.tuberlin.batchjoboperator.schedulingreconciler.statemachine;

import de.tuberlin.batchjoboperator.common.Condition;
import de.tuberlin.batchjoboperator.common.ConditionProvider;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static de.tuberlin.batchjoboperator.schedulingreconciler.statemachine.SchedulingCondition.AWAIT_NUMBER_OF_SLOTS_CONDITION;
import static de.tuberlin.batchjoboperator.schedulingreconciler.statemachine.SchedulingCondition.AWAIT_SLOTS_AVAILABLE_CONDITION;
import static de.tuberlin.batchjoboperator.schedulingreconciler.statemachine.SchedulingCondition.constructorMap;

public class SchedulingConditionProvider implements ConditionProvider<SchedulingContext> {
    private final SchedulingContext context;
    private final Map<String, Set<Condition<SchedulingContext>>> conditions;

    public SchedulingConditionProvider(SchedulingContext context) {
        this.context = context;
        this.conditions = context.getResource().getStatus().getConditions().stream()
                                 .map(c -> (Condition<SchedulingContext>) c)
                                 .collect(Collectors.groupingBy(Condition::getCondition, Collectors.toSet()));
    }

    @Override
    public Set<Condition<SchedulingContext>> getCondition(String conditionName) {
        return this.conditions.computeIfAbsent(conditionName, (cn) -> {
            if (cn.equals(AWAIT_NUMBER_OF_SLOTS_CONDITION) || cn.equals(AWAIT_SLOTS_AVAILABLE_CONDITION)) {
                return this.context.getStrategy().awaitSlotsConditions();
            }

            var condition = constructorMap.get(conditionName).get();
            condition.initialize(context);

            return Collections.singleton(condition);
        });
    }
}
