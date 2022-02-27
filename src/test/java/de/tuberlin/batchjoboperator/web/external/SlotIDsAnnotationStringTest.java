package de.tuberlin.batchjoboperator.web.external;

import de.tuberlin.batchjoboperator.crd.slots.SlotOccupationStatus;
import de.tuberlin.batchjoboperator.crd.slots.SlotState;
import org.assertj.core.util.Sets;
import org.junit.jupiter.api.Test;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;

class SlotIDsAnnotationStringTest {

    private static SlotOccupationStatus slotWithId(int id) {
        var slot =
                new SlotOccupationStatus(SlotState.FREE, "podName" + id, "nodeName", 0, id, "pod" + id);
        slot.setPosition(id);

        return slot;
    }

    @Test
    void edgeCases() {
        assertThat(SlotIDsAnnotationString.parse("NOT_A_VALID_STRING").getSlotIds())
                .isEmpty();
        assertThat(SlotIDsAnnotationString.parse("").getSlotIds())
                .isEmpty();
        assertThat(SlotIDsAnnotationString.parse("1").getSlotIds())
                .isEqualTo(Sets.set(1));


        assertThat(SlotIDsAnnotationString.of(emptyList()).toString())
                .isEqualTo("");
    }

    @Test
    void actualFunctionality() {
        var slots = Stream.of(2, 7, 3)
                          .map(SlotIDsAnnotationStringTest::slotWithId)
                          .collect(Collectors.toSet());

        assertThat(SlotIDsAnnotationString.of(slots).toString())
                .isEqualTo("2_3_7");

        assertThat(SlotIDsAnnotationString.parse("2_3_7").getSlotIds())
                .isEqualTo(Sets.set(2, 3, 7));

        assertThat(SlotIDsAnnotationString.parse("3_7_2").getSlotIds())
                .isEqualTo(Sets.set(2, 3, 7));
    }
}