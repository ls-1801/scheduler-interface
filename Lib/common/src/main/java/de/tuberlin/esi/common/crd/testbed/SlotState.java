package de.tuberlin.esi.common.crd.testbed;

import com.fasterxml.jackson.annotation.JsonValue;

public enum SlotState {
    FREE("FREE"),
    OCCUPIED("OCCUPIED"),
    ERROR("ERROR"),
    RESERVED("RESERVED");

    private final String statusString;


    SlotState(String statusString) {
        this.statusString = statusString;
    }

    @JsonValue
    public String getStatusString() {
        return statusString;
    }
}
