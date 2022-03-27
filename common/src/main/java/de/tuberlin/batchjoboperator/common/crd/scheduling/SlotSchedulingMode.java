package de.tuberlin.batchjoboperator.common.crd.scheduling;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum SlotSchedulingMode {
    @JsonProperty("strict")
    STRICT,
    @JsonProperty("relaxed")
    RELAXED
}
