package de.tuberlin.batchjoboperator.web.external;

import com.google.common.base.Splitter;
import com.google.common.primitives.Ints;
import de.tuberlin.batchjoboperator.crd.slots.SlotOccupationStatus;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

public class SlotIDsAnnotationString {
    @Nonnull
    private final TreeSet<Integer> slotIds;

    private SlotIDsAnnotationString(@Nonnull TreeSet<Integer> slotIds) {
        this.slotIds = slotIds;
    }

    @Nonnull
    public static SlotIDsAnnotationString of(@Nonnull Collection<SlotOccupationStatus> slots) {
        var set = slots.stream().map(SlotOccupationStatus::getPosition).collect(Collectors.toCollection(TreeSet::new));
        return new SlotIDsAnnotationString(set);
    }

    @Nonnull
    public static SlotIDsAnnotationString parse(@Nonnull String annotationValue) {
        var set = Splitter.on("_")
                          .trimResults()
                          .splitToStream(annotationValue)
                          .map(Ints::tryParse)
                          .filter(Objects::nonNull)
                          .collect(Collectors.toCollection(TreeSet::new));

        return new SlotIDsAnnotationString(set);
    }

    @Nonnull
    public Set<Integer> getSlotIds() {
        return slotIds;
    }

    @Nonnull
    public String toString() {
        return slotIds.stream().map(String::valueOf).collect(Collectors.joining("_"));
    }
}
