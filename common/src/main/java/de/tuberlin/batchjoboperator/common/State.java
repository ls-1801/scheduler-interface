package de.tuberlin.batchjoboperator.common;

import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.Singular;
import lombok.Value;

import javax.annotation.Nullable;
import java.util.List;

@RequiredArgsConstructor
@Value
@Builder
public class State<T extends StateMachineContext> {

    @Nullable
    String stateName;
    boolean anonymous;
    @Singular
    List<OnCondition<T>> conditions;
    @Singular
    List<State<T>> subStates;
    @Nullable
    State<T> errorState;


    public static <T extends StateMachineContext> StateBuilder<T> withName(String state3) {
        return new StateBuilder<T>()
                .stateName(state3)
                .anonymous(false);
    }

    public static <T extends StateMachineContext> StateBuilder<T> anonymous() {
        return new StateBuilder<T>()
                .stateName(null)
                .anonymous(true);
    }
}
