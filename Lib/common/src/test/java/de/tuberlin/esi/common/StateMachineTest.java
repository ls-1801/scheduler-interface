package de.tuberlin.esi.common;

import de.tuberlin.esi.common.statemachine.Condition;
import de.tuberlin.esi.common.statemachine.ConditionProvider;
import de.tuberlin.esi.common.statemachine.OnCondition;
import de.tuberlin.esi.common.statemachine.State;
import de.tuberlin.esi.common.statemachine.StateMachine;
import de.tuberlin.esi.common.statemachine.StateMachineContext;
import lombok.Data;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.assertj.core.groups.Tuple;
import org.assertj.core.util.Sets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class StateMachineTest {

    private static final String AWAIT_JOB_CONDITION = "AWAIT_TEST_CONDITION1";
    private static final String AWAIT_SLOTS_CONDITION = "AWAIT_TEST_CONDITION2";
    private static final String AWAIT_SCHEDULING_CONDITION = "AWAIT_TEST_CONDITION3";

    private static final String STATE_1 = "STATE_1";
    private static final String STATE_2 = "STATE_2";
    private static final String STATE_3 = "STATE_3";
    private static final String INNER_ERROR = "INNER_ERROR";

    MutableBoolean assertActionWasCalled1 = new MutableBoolean(false);
    MutableBoolean assertActionWasCalled2 = new MutableBoolean(false);
    MutableBoolean assertActionWasCalled3 = new MutableBoolean(false);
    private final StateMachine<TestContext> uut =
            StateMachine.of(
                    State.<TestContext>withName(STATE_1)
                         .condition(OnCondition.any(
                                 (conditions, context) -> assertActionWasCalled1.setTrue(),
                                 STATE_2,
                                 AWAIT_SLOTS_CONDITION
                         ))
                         .build(),
                    State.<TestContext>anonymous()
                         .errorState(State.<TestContext>withName(INNER_ERROR).build())
                         .subState(State.<TestContext>withName(STATE_2)
                                        .condition(
                                                OnCondition.any(
                                                        (conditions, context) -> assertActionWasCalled2.setTrue(),
                                                        STATE_3,
                                                        AWAIT_JOB_CONDITION
                                                )).build())
                         .subState(State.<TestContext>withName(STATE_3).build())
                         .condition(
                                 OnCondition.any(
                                         (conditions, context) -> assertActionWasCalled3.setTrue(),
                                         STATE_1,
                                         AWAIT_SCHEDULING_CONDITION
                                 ))
                         .build()
            );

    @BeforeEach
    void setUp() {
        resetAssertActionWasCalled();
    }

    private void resetAssertActionWasCalled() {
        assertActionWasCalled1.setFalse();
        assertActionWasCalled2.setFalse();
        assertActionWasCalled3.setFalse();
    }

    private TestContext createContext(
            Map<String, String> args,
            Map<String, Boolean> jobs,
            Map<String, Boolean> slots,
            Map<String, Boolean> scheduling
    ) {
        var context = new TestContext();

        context.conditionArgs = args;

        context.jobs = jobs;
        context.slots = slots;
        context.scheduling = scheduling;

        return context;
    }

    @Test
    void testInitialState() {

        var context = createContext(
                Map.of(AWAIT_SLOTS_CONDITION, "slots1"),
                Map.of(),
                Map.of("slots1", false),
                Map.of()
        );

        var provider = new TestConditionProvider(context, Collections.emptySet());

        assertThat(uut.update(STATE_1, provider, context))
                .isEmpty();

        assertThat(uut.runMachineUntilItStops(null, provider, context))
                .hasSize(1)
                .extracting("newState", "newConditions")
                .containsExactly(Tuple.tuple(STATE_1, Sets.set(provider.parsedCondition.get(AWAIT_SLOTS_CONDITION))));

        assertThat(provider.parsedCondition.get(AWAIT_SLOTS_CONDITION))
                .extracting("slotName")
                .isEqualTo("slots1");


        assertThat(assertActionWasCalled1.booleanValue()).isFalse();
        assertThat(assertActionWasCalled2.booleanValue()).isFalse();
        assertThat(assertActionWasCalled3.booleanValue()).isFalse();

    }

    @Test
    void testNextState() {

        Set<Condition<TestContext>> conditions = Collections.emptySet();
        String state = null;
        {
            var context = createContext(
                    Map.of(AWAIT_SLOTS_CONDITION, "slots1"),
                    Map.of(),
                    Map.of("slots1", false),
                    Map.of()
            );

            var provider = new TestConditionProvider(context, conditions);

            var updates = uut.runMachineUntilItStops(null, provider, context);
            conditions = updates.get(0).getNewConditions();
            state = updates.get(0).getNewState();

            assertThat(state).isEqualTo(STATE_1);
        }

        {
            var context = createContext(
                    Map.of(
                            AWAIT_SCHEDULING_CONDITION, "scheduling1",
                            AWAIT_JOB_CONDITION, "job1"
                    ),
                    Map.of("job1", false),
                    Map.of("slots1", true),
                    Map.of("scheduling1", false)
            );

            var provider = new TestConditionProvider(context, conditions);

            var updates = uut.runMachineUntilItStops(state, provider, context);
            conditions = updates.get(0).getNewConditions();
            state = updates.get(0).getNewState();

            assertThat(state).isEqualTo(STATE_2);
            assertThat(conditions).hasSize(2);

            assertThat(assertActionWasCalled1.booleanValue()).isTrue();
        }

        {
            var context = createContext(
                    Map.of(
                    ),
                    Map.of("job1", true),
                    Map.of(),
                    Map.of("scheduling1", false)
            );

            var provider = new TestConditionProvider(context, conditions);

            var updates = uut.runMachineUntilItStops(state, provider, context);
            conditions = updates.get(0).getNewConditions();
            state = updates.get(0).getNewState();

            assertThat(state).isEqualTo(STATE_3);
            assertThat(conditions).hasSize(1);

            assertThat(assertActionWasCalled1.booleanValue()).isTrue();
        }

        {
            var context = createContext(
                    Map.of(AWAIT_SLOTS_CONDITION, "slots1"),
                    Map.of(),
                    Map.of("slots1", false),
                    Map.of("scheduling1", true)
            );

            var provider = new TestConditionProvider(context, conditions);

            var updates = uut.runMachineUntilItStops(state, provider, context);
            conditions = updates.get(0).getNewConditions();
            state = updates.get(0).getNewState();

            assertThat(state).isEqualTo(STATE_1);
            assertThat(conditions).hasSize(1);

            assertThat(assertActionWasCalled1.booleanValue()).isTrue();
        }


    }

    @Data
    static class AwaitJobCondition implements Condition<TestContext> {
        private final String condition = AWAIT_JOB_CONDITION;
        private String conditionString;
        private Boolean value;
        private String error;

        @Override
        public void update(TestContext context) {
            this.value = context.jobs.get(conditionString);
            this.error = context.errorMap.get(conditionString);
        }

        @Override
        public void initialize(TestContext context) {
            this.conditionString = context.conditionArgs.get(AWAIT_JOB_CONDITION);
        }
    }

    @Data
    static class AwaitSlotsCondition implements Condition<TestContext> {
        private final String condition = AWAIT_SLOTS_CONDITION;
        private String slotName;
        private Boolean value;
        private String error;

        @Override
        public void update(TestContext context) {
            this.value = context.slots.get(slotName);
            this.error = context.errorMap.get(slotName);
        }

        @Override
        public void initialize(TestContext context) {
            this.slotName = context.conditionArgs.get(AWAIT_SLOTS_CONDITION);
        }
    }

    @Data
    static class AwaitSchedulingCondition implements Condition<TestContext> {
        private final String condition = AWAIT_SCHEDULING_CONDITION;
        private String conditionString;
        private Boolean value;
        private String error;

        @Override
        public void update(TestContext context) {
            this.value = context.scheduling.get(conditionString);
            this.error = context.errorMap.get(conditionString);
        }

        @Override
        public void initialize(TestContext context) {
            this.conditionString = context.conditionArgs.get(AWAIT_SCHEDULING_CONDITION);
        }
    }

    static class ConditionFactory {

        private static final Map<String, Supplier<Condition<TestContext>>> constructorMap =
                Map.of(
                        AWAIT_JOB_CONDITION, AwaitJobCondition::new,
                        AWAIT_SLOTS_CONDITION, AwaitSlotsCondition::new,
                        AWAIT_SCHEDULING_CONDITION, AwaitSchedulingCondition::new
                );

        public static Condition<TestContext> create(String conditionName, TestContext context) {
            var condition = constructorMap.get(conditionName).get();
            condition.initialize(context);
            return condition;
        }
    }

    class TestContext implements StateMachineContext {
        Map<String, Boolean> jobs = new HashMap<>();
        Map<String, Boolean> slots = new HashMap<>();
        Map<String, Boolean> scheduling = new HashMap<>();
        Map<String, String> errorMap = new HashMap<>();


        Map<String, String> conditionArgs = new HashMap<>();
    }

    class TestConditionProvider implements ConditionProvider<TestContext> {

        private final TestContext context;
        private final Map<String, Condition<TestContext>> parsedCondition;

        public TestConditionProvider(TestContext context, Set<Condition<TestContext>> parsedCondition) {
            this.context = context;
            this.parsedCondition = parsedCondition.stream().collect(Collectors.toMap(
                    c -> c.getCondition(),
                    Function.identity()
            ));
        }

        @Override
        public Set<Condition<TestContext>> getCondition(String conditionName) {
            return Collections.singleton(parsedCondition.computeIfAbsent(
                    conditionName,
                    (name) -> ConditionFactory.create(name, context)
            ));
        }
    }


}