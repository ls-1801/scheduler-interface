package de.tuberlin.batchjoboperator.common.statemachine;

import lombok.RequiredArgsConstructor;
import lombok.Value;

import javax.annotation.Nullable;
import java.util.Set;

@Value
@RequiredArgsConstructor
public class UpdateResult<T extends StateMachineContext> {
    @Nullable
    String error;
    String newState;
    Set<Condition<T>> newConditions;

    public boolean isError() {
        return error != null;
    }
}
