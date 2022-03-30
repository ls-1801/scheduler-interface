package de.tuberlin.esi.schedulingreconciler.statemachine;

import de.tuberlin.esi.common.crd.NamespacedName;

public class TestbedClaimedByAnotherSchedulingException extends ClaimedByAnotherSchedulingException {
    public TestbedClaimedByAnotherSchedulingException(NamespacedName testbedName, NamespacedName activeScheduling) {
        super("Testbed", testbedName, activeScheduling);
    }
}
