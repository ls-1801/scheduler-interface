package de.tuberlin.esi.schedulingreconciler;

import de.tuberlin.esi.common.crd.NamespacedName;
import de.tuberlin.esi.schedulingreconciler.strategy.QueueBasedSlotSelectionStrategy;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public class TestbedSelectionStrategyTest {


    @Test
    void longQueueOddNumber() {
        var job1 = new NamespacedName("job1", "default");
        var job2 = new NamespacedName("job2", "default");
        var job3 = new NamespacedName("job3", "default");
        var queue2 = List.of(
                job1, job2, job3,
                job1, job2, job3,
                job1, job2, job3
        );

        var freeSlots = Set.of(0, 1, 2, 3, 4, 5, 6, 7);
        var sss = new QueueBasedSlotSelectionStrategy(queue2, freeSlots, Collections.emptySet());
        assertThat(sss.getSlotsForJob(job1))
                .contains(0, 3, 6);

        sss = new QueueBasedSlotSelectionStrategy(queue2, Set.of(1, 2, 4, 5, 7), Set.of(job1));
        assertThat(sss.getSlotsForJob(job2))
                .contains(1, 4, 7);

        sss = new QueueBasedSlotSelectionStrategy(queue2, Set.of(2, 5), Set.of(job1, job2));
        assertThat(sss.getSlotsForJob(job3))
                .isEmpty();

        sss = new QueueBasedSlotSelectionStrategy(queue2, Set.of(0, 2, 3, 5, 6), Set.of(job1, job2));
        assertThat(sss.getSlotsForJob(job3))
                .contains(0, 2, 3);
    }

    @Test
    void longQueue() {
        var job1 = new NamespacedName("job1", "default");
        var job2 = new NamespacedName("job2", "default");
        var job3 = new NamespacedName("job3", "default");
        var queue2 = List.of(
                job1, job2, job1, job2, job1, job2, job1, job2,
                job3, job3, job3, job3
        );
        var freeSlots = Set.of(0, 1, 2, 3, 4, 5, 6, 7);

        var sss = new QueueBasedSlotSelectionStrategy(queue2, freeSlots, Collections.emptySet());
        assertThat(sss.getSlotsForJob(job1))
                .contains(0, 2, 4, 6);

        sss = new QueueBasedSlotSelectionStrategy(queue2, Set.of(1, 3, 5, 7), Set.of(job1));
        assertThat(sss.getSlotsForJob(job2))
                .contains(1, 3, 5, 7);

        sss = new QueueBasedSlotSelectionStrategy(queue2, Set.of(), Set.of(job1, job2));
        assertThat(sss.getSlotsForJob(job3))
                .isEmpty();

        sss = new QueueBasedSlotSelectionStrategy(queue2, Set.of(1, 3, 5, 7), Set.of(job1, job2));
        assertThat(sss.getSlotsForJob(job3))
                .contains(1, 3, 5, 7);
    }

    @Test
    void impossible() {
        var job1 = new NamespacedName("job1", "default");
        var job2 = new NamespacedName("job2", "default");
        var queue2 = List.of(job2, job1, job1, job2, job1, job2, job1, job2, job2);
        var freeSlots = Set.of(0, 1, 2, 3, 4, 5, 6, 7);

        var sss = new QueueBasedSlotSelectionStrategy(queue2, freeSlots, Collections.emptySet());

        assertThat(sss.getSlotsForJob(job2))
                .isEmpty();

        sss = new QueueBasedSlotSelectionStrategy(queue2, freeSlots, Set.of());

        assertThat(sss.getSlotsForJob(job1))
                .contains(1, 2, 4, 6);
    }


    @Test
    void simpleTest() {
        var job1 = new NamespacedName("job1", "default");
        var job2 = new NamespacedName("job2", "default");
        var queue2 = List.of(job2, job1, job1, job2, job1, job2, job1, job2);
        var freeSlots = Set.of(0, 1, 2, 3, 4, 5, 6, 7);

        var sss = new QueueBasedSlotSelectionStrategy(queue2, freeSlots, Collections.emptySet());

        assertThat(sss.getSlotsForJob(job2))
                .contains(0, 3, 5, 7);

        sss = new QueueBasedSlotSelectionStrategy(queue2, Set.of(1, 2, 4, 6), Set.of(job2));

        assertThat(sss.getSlotsForJob(job1))
                .contains(1, 2, 4, 6);
    }
}
