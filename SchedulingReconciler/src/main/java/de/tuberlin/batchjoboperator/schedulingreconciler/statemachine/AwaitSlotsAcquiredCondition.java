package de.tuberlin.batchjoboperator.schedulingreconciler.statemachine;

import de.tuberlin.batchjoboperator.common.NamespacedName;
import lombok.Getter;
import lombok.Setter;

import static de.tuberlin.batchjoboperator.common.constants.SchedulingConstants.ACTIVE_SCHEDULING_LABEL_NAME;
import static de.tuberlin.batchjoboperator.common.constants.SchedulingConstants.ACTIVE_SCHEDULING_LABEL_NAMESPACE;
import static de.tuberlin.batchjoboperator.common.util.General.getNullSafe;

public class AwaitSlotsAcquiredCondition extends SchedulingCondition {

    public static final String condition = AWAIT_SLOTS_ACQUIRED_CONDITION;

    @Getter
    @Setter
    private NamespacedName slotsName;

    @Override
    public String getCondition() {
        return AWAIT_SLOTS_ACQUIRED_CONDITION;
    }

    @Override
    protected boolean updateInternal(SchedulingContext context) {
        var activeSchedulingName = getNullSafe(() -> {
            var slots = context.getSlots();
            var name = slots.getMetadata().getLabels().get(ACTIVE_SCHEDULING_LABEL_NAME);
            var namespace =
                    slots.getMetadata().getLabels().get(ACTIVE_SCHEDULING_LABEL_NAMESPACE);

            return new NamespacedName(name, namespace);
        });

        return activeSchedulingName.map(actSched -> actSched.equals(NamespacedName.of(context.getResource())))
                                   .orElse(false);

    }


    @Override
    public void initialize(SchedulingContext context) {
        super.initialize(context);
        this.slotsName = NamespacedName.of(context.getSlots());
    }
}
