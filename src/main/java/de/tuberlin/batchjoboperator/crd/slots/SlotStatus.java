package de.tuberlin.batchjoboperator.crd.slots;

import de.tuberlin.batchjoboperator.web.external.NamespacedName;
import io.javaoperatorsdk.operator.api.ObservedGenerationAwareStatus;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.apache.commons.lang3.mutable.MutableInt;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

@Getter
@Setter
@ToString
public class SlotStatus extends ObservedGenerationAwareStatus {
    private SlotsStatusState state = SlotsStatusState.INITIAL;
    @Nullable
    private Map<String, List<String>> nodeProblems;

    @Nullable
    private List<String> problems;

    @Nullable
    private Set<SlotOccupationStatus> slots;

    @Nullable
    private String schedulingInProgressTimestamp;

    @Nullable
    private List<NamespacedName> currentScheduling;


    public void addProblem(String problem) {
        if (problems == null)
            problems = new ArrayList<>();

        problems.add(problem);
    }

    public void setSlots(@Nullable Collection<SlotOccupationStatus> slots) {
        if (slots == null) {
            this.slots = null;
            return;
        }

        var set = new TreeSet<>(Comparator.comparingInt(SlotOccupationStatus::getSlotPositionOnNode)
                                          .thenComparingInt(SlotOccupationStatus::getNodeId));
        set.addAll(slots);
        var mutatingInt = new MutableInt(0);
        set.forEach(slot -> slot.setPosition(mutatingInt.getAndIncrement()));
        this.slots = set;
    }
}
