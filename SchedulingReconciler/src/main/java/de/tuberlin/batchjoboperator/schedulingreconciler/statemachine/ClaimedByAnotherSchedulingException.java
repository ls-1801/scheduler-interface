package de.tuberlin.batchjoboperator.schedulingreconciler.statemachine;

import de.tuberlin.batchjoboperator.common.crd.NamespacedName;

import java.text.MessageFormat;

public abstract class ClaimedByAnotherSchedulingException extends RuntimeException {
    public ClaimedByAnotherSchedulingException(String resourceKind, NamespacedName name, NamespacedName scheduling) {
        super(MessageFormat.format("{0} {1} is currently claimed by a different scheduling. Active Scheduling: {2}",
                resourceKind, name, scheduling
        ));
    }
}
