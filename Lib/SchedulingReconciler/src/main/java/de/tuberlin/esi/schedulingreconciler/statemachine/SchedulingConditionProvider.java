package de.tuberlin.esi.schedulingreconciler.statemachine;

import de.tuberlin.esi.common.statemachine.Condition;
import de.tuberlin.esi.common.statemachine.ConditionProvider;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class SchedulingConditionProvider implements ConditionProvider<SchedulingContext> {
    private final SchedulingContext context;
    private final Map<String, Set<Condition<SchedulingContext>>> conditions;
    private static final Set<String> strategyBasedCondition = Set.of(
            SchedulingCondition.AWAIT_NUMBER_OF_SLOTS_CONDITION,
            SchedulingCondition.AWAIT_SLOTS_AVAILABLE_CONDITION
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
            // Some Conditions might only be necessary if the scheduling strategy requires them.
            // Currently, the QueueBasedStrategy scheduling only requires a number of slots to be available,
            // where the SlotBasedStrategy only requires specific slots to be available
            //
            // The QueueBasedStrategy will only return conditions for the AWAIT_NUMBER_OF_SLOTS_CONDITION String, and an
            // emptySet for every other String. Returning an emptySet is fine, in combination with the any operator
            // because the parent OnCondition object is configures with the Any Operator.
            if (strategyBasedCondition.contains(cn)) {
                var strategyBasedConditions = context.getStrategy().awaitSlotsConditions(cn);
                strategyBasedConditions.forEach(c -> c.initialize(context));
                return strategyBasedConditions;
            }

            var condition = SchedulingCondition.constructorMap.get(conditionName).get();
            condition.initialize(context);

            return Collections.singleton(condition);
        });
    }
}
