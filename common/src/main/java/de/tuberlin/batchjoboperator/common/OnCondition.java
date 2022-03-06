package de.tuberlin.batchjoboperator.common;

import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.assertj.core.util.Lists;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Value
public class OnCondition<T extends StateMachineContext> {

    boolean any;
    Action<T> action;
    String state;
    List<String> conditionIdentifiers;

    public static <T extends StateMachineContext> OnCondition<T> any(Action<T> action, String state,
                                                                     String... conditions) {
        return new OnCondition<>(true, action, state, Lists.newArrayList(conditions));
    }

    public static <T extends StateMachineContext> OnCondition<T> all(Action<T> action, String state,
                                                                     String... conditions) {
        return new OnCondition<>(false, action, state, Lists.newArrayList(conditions));
    }

    public void update(ConditionProvider<T> provider, T context) {
        conditionIdentifiers.forEach(c -> provider.getCondition(c).update(context));
    }

    public ErrorsOrResult shouldDoAction(ConditionProvider<T> provider) {
        var results = conditionIdentifiers.stream().map(c -> provider.getCondition(c)).collect(Collectors.toList());
        var errors = results.stream()
                            .map(Condition::getError)
                            .filter(Objects::nonNull)
                            .collect(Collectors.toList());


        if (!errors.isEmpty())
            return ErrorsOrResult.error(errors);

        if (any)
            return ErrorsOrResult.result(results.stream()
                                                .map(Condition::getValue)
                                                .filter(Objects::nonNull)
                                                .anyMatch(Boolean::booleanValue));

        return ErrorsOrResult.result(results.stream()
                                            .map(Condition::getValue)
                                            .filter(Objects::nonNull)
                                            .allMatch(Boolean::booleanValue));
    }


    public void callAction(ConditionProvider<T> provider, T context) {
        var conditions =
                conditionIdentifiers.stream().map(c -> provider.getCondition(c))
                                    .collect(Collectors.toSet());
        this.action.doTheThing(conditions, context);
    }
}
