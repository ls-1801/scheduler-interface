package de.tuberlin.batchjoboperator.common;

import java.lang.reflect.Field;
import java.util.Optional;
import java.util.Set;

public interface Action<T extends StateMachineContext> {


    /**
     * Retrieve a specific Condition by Class from a Set of Conditions
     * The methods expects the Class clazz to have a public static string constant 'condition'
     */
    static <R extends StateMachineContext, T extends Condition<R>> Optional<T> getCondition(Set<Condition<R>> conditions,
                                                                                            Class<T> clazz) {
        try {
            Field field = clazz.getDeclaredField("condition");
            String conditionName = (String) field.get(null);

            var condition =
                    conditions.stream().filter(c -> conditionName.equals(c.getCondition())).findFirst()
                              .map(o -> (T) o);

            return condition;

        } catch (NoSuchFieldException | IllegalAccessException e) {
            return Optional.empty();
        }
    }

    void doTheThing(Set<Condition<T>> conditions, T context);
}
