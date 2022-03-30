package de.tuberlin.esi.schedulingreconciler.statemachine;

import org.junit.jupiter.api.Test;

import java.util.Objects;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

class AwaitNumberOfSlotsAvailableConditionTest {

    @Test
    void testEmptyStreamAllMatchBehaivor() {
        assertTrue(Stream.empty().allMatch(Objects::nonNull));
    }
}