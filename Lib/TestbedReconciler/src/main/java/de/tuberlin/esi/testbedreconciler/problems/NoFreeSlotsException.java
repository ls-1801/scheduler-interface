package de.tuberlin.esi.testbedreconciler.problems;

import de.tuberlin.esi.common.crd.testbed.Testbed;
import de.tuberlin.esi.testbedreconciler.reconciler.ApplicationPodView;

import java.text.MessageFormat;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class NoFreeSlotsException extends RuntimeException {

    private NoFreeSlotsException(String message) {
        super(message);
    }

    public static NoFreeSlotsException create(ApplicationPodView requestPod, Set<Integer> slotIds, Testbed slots) {
        var invalidIds = slotIds.stream()
                                .filter(id -> Objects.requireNonNull(slots.getStatus().getSlots()).stream()
                                                     .noneMatch(slot -> slot.getPosition() == id))
                                .collect(Collectors.toList());

        var validSlots = slotIds.stream()
                                .map(id -> Objects.requireNonNull(slots.getStatus().getSlots()).stream()
                                                  .filter(slot -> slot.getPosition() == id).findAny())
                                .filter(Optional::isPresent)
                                .collect(Collectors.toList());

        var message = MessageFormat.format(
                "Preemption failed! All desired slots are either reserved or occupied. Invalid Slot Ids:" +
                        " {0} Valid Slots: {1}", invalidIds, validSlots);

        return new NoFreeSlotsException(message);
    }
}
