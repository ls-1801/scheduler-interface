package de.tuberlin.esi.schedulingreconciler.statemachine;

import de.tuberlin.esi.common.crd.NamespacedName;
import lombok.Getter;
import lombok.Setter;

import static de.tuberlin.esi.common.constants.SchedulingConstants.ACTIVE_SCHEDULING_LABEL_NAME;
import static de.tuberlin.esi.common.constants.SchedulingConstants.ACTIVE_SCHEDULING_LABEL_NAMESPACE;
import static de.tuberlin.esi.common.util.General.getNullSafe;

public class AwaitTestbedReleasedCondition extends SchedulingCondition {

    public static final String condition = AWAIT_TESTBED_RELEASED_CONDITION;

    @Getter
    @Setter
    private NamespacedName testbedName;

    @Override
    public String getCondition() {
        return AWAIT_TESTBED_RELEASED_CONDITION;
    }

    @Override
    protected boolean updateInternal(SchedulingContext context) {
        var activeSchedulingName = getNullSafe(() -> {
            var testbed = context.getTestbed();
            var name = testbed.getMetadata().getLabels().get(ACTIVE_SCHEDULING_LABEL_NAME);
            var namespace =
                    testbed.getMetadata().getLabels().get(ACTIVE_SCHEDULING_LABEL_NAMESPACE);

            return new NamespacedName(name, namespace);
        });

        return activeSchedulingName.map(actSched -> !actSched.equals(NamespacedName.of(context.getResource())))
                                   .orElse(true);

    }


    @Override
    public void initialize(SchedulingContext context) {
        super.initialize(context);
        // Testbed might not exist, however the spec requires that it is at least specified
        this.testbedName = getNullSafe(() -> context.getResource().getSpec().getTestbed()).orElse(null);
    }
}
