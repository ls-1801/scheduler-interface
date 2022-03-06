package de.tuberlin.batchjoboperator.common.crd.slots;

import com.fasterxml.jackson.annotation.JsonValue;

public enum SlotsStatusState {
    IN_PROGRESS("IN_PROGRESS"),
    SUCCESS("SUCCESS"),
    ERROR("ERROR"),
    RUNNING("RUNNING"),
    INITIAL("INITIAL");

    private final String statusString;


    SlotsStatusState(String statusString) {
        this.statusString = statusString;
    }

    @JsonValue
    public String getStatusString() {
        return statusString;
    }
}

