package de.tuberlin.esi.schedulingreconciler.strategy;

import de.tuberlin.esi.common.crd.NamespacedName;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static de.tuberlin.esi.common.util.General.enumerate;

@Value
@Slf4j
public class QueueBasedSlotSelectionStrategy {
    List<NamespacedName> queue;
    List<Integer> freeSlots;

    public QueueBasedSlotSelectionStrategy(List<NamespacedName> queue,
                                           Set<Integer> freeSlots,
                                           Set<NamespacedName> submittedOrCompleted) {
        this.queue = queue.stream().filter(Predicate.not(submittedOrCompleted::contains)).collect(Collectors.toList());
        this.freeSlots = freeSlots.stream().sorted().collect(Collectors.toList());
    }

    public Set<Integer> getSlotsForJob(NamespacedName name) {
        log.debug("Finding Slots for Job {} given a Queue of {} and FreeSlots {}", name, queue, freeSlots);

        var slotIds = new HashSet<Integer>();

        for (var pair : enumerate(queue).collect(Collectors.toList())) {
            if (!pair.getKey().equals(name)) {
                if (pair.getValue() >= freeSlots.size()) {
                    log.debug("NOT Job {} does not fit", pair.getKey());
                }
                else {
                    log.debug("NOT Slot #{} is for {}", freeSlots.get(pair.getValue()), pair.getKey());
                }
                continue;
            }

            if (pair.getValue() >= freeSlots.size()) {
                log.debug("Job {} cannot be placed into free slots", name);
                return Collections.emptySet();
            }

            log.debug("YES Slot #{} is for {}", pair.getValue(), pair.getKey());
            slotIds.add(freeSlots.get(pair.getValue()));
        }

        return slotIds;
    }
}
