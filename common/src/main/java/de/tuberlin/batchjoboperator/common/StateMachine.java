package de.tuberlin.batchjoboperator.common;

import com.google.common.collect.Streams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RequiredArgsConstructor
@Slf4j
public class StateMachine<T extends StateMachineContext> {
    private final List<State<T>> states;
    private final State<T> defaultErrorState = State.<T>withName("FailedState").build();

    @SafeVarargs
    public static <T extends StateMachineContext> StateMachine<T> of(State<T>... states) {
        var list = List.of(states);
        if (list.isEmpty())
            throw new RuntimeException("List of States cannot be empty");

        if (list.get(0).isAnonymous()) {
            throw new RuntimeException("First State cannot be Anonymous, because this will be the initial state");
        }


        return new StateMachine<>(List.of(states));
    }

    protected State<T> getDefaultErrorState() {
        return defaultErrorState;
    }

    public Optional<UpdateResult<T>> update(@Nullable String stateName, ConditionProvider<T> provider, T context) {
        if (stateName == null) {
            //Initial State

            return Optional.of(new UpdateResult<>(
                    null,
                    states.get(0).getStateName(),
                    conditionsForState(states.get(0).getStateName(), provider)
            ));

        }


        var trace = traceState(stateName);
        if (stateName.equals(getDefaultErrorState().getStateName())) {
            trace = Collections.singletonList(getDefaultErrorState());
        }

        if (trace.isEmpty()) {
            throw new RuntimeException("State not found");
        }


        for (State<T> tState : trace) {
            for (OnCondition<T> condition : tState.getConditions()) {
                condition.update(provider, context);
            }
        }

        for (State<T> tState : trace) {
            for (OnCondition<T> condition : tState.getConditions()) {
                var result = condition.shouldDoAction(provider);
                if (result.isError()) {
                    var errorState = findErrorState(stateName);
                    return Optional.of(new UpdateResult<T>(
                            result.getErrors().stream().collect(Collectors.joining(",")),
                            errorState.getStateName(),
                            conditionsForState(errorState.getStateName(), provider)
                    ));
                }
                if (result.isResult() && result.getResult()) {
                    try {
                        condition.callAction(provider, context);
                        return Optional.of(new UpdateResult<>(
                                null,
                                condition.getState(),
                                conditionsForState(condition.getState(), provider)
                        ));
                    } catch (Exception e) {
                        log.error("Conditions {} Action caused an Exception:", e);
                        var errorState = findErrorState(stateName);
                        return Optional.of(new UpdateResult<T>(
                                e.getMessage(),
                                errorState.getStateName(),
                                conditionsForState(errorState.getStateName(), provider)
                        ));
                    }
                }
            }
        }

        return Optional.empty();
    }

    private State<T> findErrorState(String statename) {
        var trace = traceState(statename);

        State<T> nearestErrorState = getDefaultErrorState();

        for (var traceState : trace) {
            if (traceState.getErrorState() != null) {
                nearestErrorState = traceState.getErrorState();
            }

            if (statename.equals(traceState.getStateName())) {
                return nearestErrorState;
            }
        }

        return getDefaultErrorState();
    }

    private Set<Condition<T>> conditionsForState(String statename, ConditionProvider<T> conditionProvider) {
        var trace = traceState(statename, true);
        return trace.stream().flatMap(tState -> tState.getConditions().stream()
                                                      .flatMap(c -> c.getConditionIdentifiers().stream()))
                    .map(conditionProvider::getCondition)
                    .collect(Collectors.toSet());
    }


    private List<State<T>> traceState(String stateName) {
        return traceState(stateName, false);
    }

    private List<State<T>> traceState(String stateName, boolean includeError) {
        var stream = states.stream();
        if (includeError && getDefaultErrorState() != null) {
            stream = Streams.concat(Stream.of(getDefaultErrorState()), stream);
        }

        return stream.flatMap(state -> traceStatesRecursive(state, stateName, includeError))
                     .collect(Collectors.toList());
    }

    private Stream<State<T>> traceStatesRecursive(State<T> state, String statename, boolean includeError) {
        if (statename.equals(state.getStateName())) {
            return Stream.of(state);
        }

        var stream = state.getSubStates().stream();
        if (includeError && state.getErrorState() != null) {
            stream = Streams.concat(Stream.of(state.getErrorState()), stream);
        }

        var subTrace = stream.flatMap(ss -> traceStatesRecursive(ss, statename, includeError))
                             .collect(Collectors.toList());

        if (subTrace.isEmpty())
            return Stream.empty();

        return Streams.concat(Stream.of(state), subTrace.stream());
    }

    public List<UpdateResult<T>> runMachineUntilItStops(@Nullable String initialState,
                                                        @Nonnull ConditionProvider<T> provider,
                                                        @Nonnull T context) {
        int sanityCheck = 100;
        var state = initialState;
        var lastUpdate = Optional.<UpdateResult<T>>empty();
        var updates = new ArrayList<UpdateResult<T>>();
        while (sanityCheck > 0) {
            lastUpdate = this.update(state, provider, context);

            if (lastUpdate.isEmpty())
                return updates;
            log.debug("Advancing to {}", lastUpdate.get().getNewState());
            state = lastUpdate.get().getNewState();
            updates.add(lastUpdate.get());
            sanityCheck--;
        }

        Assertions.fail("The Machine does not appear to stop");
        return Collections.emptyList();
    }

}
