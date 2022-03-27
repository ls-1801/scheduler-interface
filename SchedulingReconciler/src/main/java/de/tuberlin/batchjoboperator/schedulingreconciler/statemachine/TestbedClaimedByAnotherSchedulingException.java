package de.tuberlin.batchjoboperator.schedulingreconciler.statemachine;

import de.tuberlin.batchjoboperator.common.crd.NamespacedName;

public class TestbedClaimedByAnotherSchedulingException extends ClaimedByAnotherSchedulingException {
    public TestbedClaimedByAnotherSchedulingException(NamespacedName testbedName, NamespacedName activeScheduling) {
        super("Testbed", testbedName, activeScheduling);
    }
}
