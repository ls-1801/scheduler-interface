package de.tuberlin.esi.common.statemachine;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

public interface Action<T extends StateMachineContext> {


    /**
     * Retrieve a specific Condition by Class from a Set of Conditions
     * The methods expects the Class clazz to have a public static string constant 'condition'
     *
     * @return
     */
    static <R extends StateMachineContext, T extends Condition<R>> Set<T> getConditions(Set<Condition<R>> conditions,
                                                                                        Class<T> clazz) {
        try {
            Field field = clazz.getDeclaredField("condition");
            String conditionName = (String) field.get(null);


            return conditions.stream().filter(c -> conditionName.equals(c.getCondition()))
                             .map(o -> (T) o)
                             .collect(Collectors.toSet());

        } catch (NoSuchFieldException | IllegalAccessException e) {
            return Collections.emptySet();
        }
    }

    void doTheThing(Set<Condition<T>> conditions, T context);
}
