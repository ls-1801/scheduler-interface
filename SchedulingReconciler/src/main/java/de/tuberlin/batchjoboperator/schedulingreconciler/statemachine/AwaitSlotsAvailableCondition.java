package de.tuberlin.batchjoboperator.schedulingreconciler.statemachine;

import de.tuberlin.batchjoboperator.common.NamespacedName;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;

public class AwaitSlotsAvailableCondition extends SchedulingCondition {

    public static final String condition = AWAIT_SLOTS_AVAILABLE_CONDITION;

    @Getter
    @Setter
    protected Set<Integer> slotIds;

    @Getter
    @Setter
    protected NamespacedName jobName;


    public AwaitSlotsAvailableCondition(NamespacedName jobName, Set<Integer> slotIds) {
        super();
        this.jobName = jobName;
        this.slotIds = slotIds;
    }

    public AwaitSlotsAvailableCondition() {
        super();
    }

    @Override
    public String getCondition() {
        return AWAIT_SLOTS_AVAILABLE_CONDITION;
    }


    @Override
    protected boolean updateInternal(SchedulingContext context) {
        return context.getFreeSlots().containsAll(slotIds);
    }


    @Override
    public void initialize(SchedulingContext context) {
        super.initialize(context);
    }
}
