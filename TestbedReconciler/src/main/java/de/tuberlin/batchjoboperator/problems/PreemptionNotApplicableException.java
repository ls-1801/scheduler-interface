package de.tuberlin.batchjoboperator.problems;

import io.fabric8.kubernetes.api.model.Pod;

import java.text.MessageFormat;

public class PreemptionNotApplicableException extends RuntimeException {
    public PreemptionNotApplicableException(Pod pod) {
        super(MessageFormat.format("Preemption is not applicable to the pod: {}", pod));
    }
}
