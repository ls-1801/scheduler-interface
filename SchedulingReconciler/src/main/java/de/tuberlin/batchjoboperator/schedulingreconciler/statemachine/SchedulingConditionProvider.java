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
    private static final Set<String> strategyBasedCondition = Set.of(
            AWAIT_NUMBER_OF_SLOTS_CONDITION,
            AWAIT_SLOTS_AVAILABLE_CONDITION
    );

    public SchedulingConditionProvider(SchedulingContext context) {
        this.context = context;
        this.conditions = context.getResource().getStatus().getConditions().stream()
                                 .map(c -> (Condition<SchedulingContext>) c)
                                 .collect(Collectors.groupingBy(Condition::getCondition, Collectors.toSet()));
    }

    @Override
    public Set<Condition<SchedulingContext>> getCondition(String conditionName) {

        return this.conditions.computeIfAbsent(conditionName, cn -> {
            /*
              Some Conditions might only be necessary if the scheduling strategy requires them.
              Currently, the QueueBasedStrategy scheduling only requires a number of slots to be available,
              where the SlotBasedStrategy only requires specific slots to be available

              The QueueBasedStrategy will only return conditions for the AWAIT_NUMBER_OF_SLOTS_CONDITION String, and an
              emptySet for every other String. Returning an emptySet is fine, in combination with the any operator
              because the parent OnCondition object is configures with the Any Operator.
             */
            if (strategyBasedCondition.contains(cn)) {
                return context.getStrategy().awaitSlotsConditions(cn);
            }

            var condition = constructorMap.get(conditionName).get();
            condition.initialize(context);

            return Collections.singleton(condition);
        });
    }
}
