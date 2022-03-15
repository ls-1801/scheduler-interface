package de.tuberlin.batchjoboperator.common.crd.scheduling;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

import java.util.Locale;

public enum SlotSchedulingMode {
    STRICT("strict"),
    RELAXED("relaxed");

    @JsonValue
    @Getter
    private final String mode;

    SlotSchedulingMode(String mode) {
        this.mode = mode;
    }

    public static SlotSchedulingMode fromString(String mode) {
        if ("strict" .equals(mode.toLowerCase(Locale.ROOT))) {
            return STRICT;
        }

        if ("relaxed" .equals(mode.toLowerCase(Locale.ROOT))) {
            return RELAXED;
        }

        throw new RuntimeException("Could not parse mode: " + mode);
    }
}
