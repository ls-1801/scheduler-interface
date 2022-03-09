package de.tuberlin.batchjoboperator.schedulingreconciler.statemachine;

import de.tuberlin.batchjoboperator.common.NamespacedName;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;


@EqualsAndHashCode(callSuper = true)
public class AwaitNumberOfSlotsAvailableCondition extends SchedulingCondition {

    public static final String condition = AWAIT_NUMBER_OF_SLOTS_CONDITION;

    @Getter
    @Setter
    protected Integer numberOfSlotsRequired;

    @Getter
    @Setter
    protected NamespacedName jobName;

    public AwaitNumberOfSlotsAvailableCondition() {
        super();
    }

    public AwaitNumberOfSlotsAvailableCondition(NamespacedName jobName, Integer numberOfSlotsRequired) {
        super();
        this.jobName = jobName;
        this.numberOfSlotsRequired = numberOfSlotsRequired;
    }

    @Override
    public String getCondition() {
        return AWAIT_NUMBER_OF_SLOTS_CONDITION;
    }


    @Override
    protected boolean updateInternal(SchedulingContext context) {
        return context.getFreeSlots().size() >= numberOfSlotsRequired;
    }

}
