package de.tuberlin.batchjoboperator.common.crd.scheduling;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

public enum SlotSchedulingMode {
    STRICT("strict"),
    RELAXED("relaxed");

    @JsonValue
    @Getter
    private final String mode;

    SlotSchedulingMode(String mode) {
        this.mode = mode;
    }
}
