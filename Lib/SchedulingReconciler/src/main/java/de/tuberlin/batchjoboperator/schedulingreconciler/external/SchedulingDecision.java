package de.tuberlin.batchjoboperator.schedulingreconciler.external;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import de.tuberlin.batchjoboperator.common.crd.NamespacedName;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class SchedulingDecision {

    @JsonValue
    private final Map<String, List<NamespacedName>> decision;

    @JsonCreator
    public SchedulingDecision(Map<String, List<NamespacedName>> decision) {
        this.decision = decision;
    }
}
