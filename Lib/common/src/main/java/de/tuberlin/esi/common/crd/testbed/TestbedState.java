package de.tuberlin.esi.common.crd.testbed;

import com.fasterxml.jackson.annotation.JsonValue;

public enum TestbedState {
    IN_PROGRESS("IN_PROGRESS"),
    SUCCESS("SUCCESS"),
    ERROR("ERROR"),
    RUNNING("RUNNING"),
    INITIAL("INITIAL");

    private final String statusString;


    TestbedState(String statusString) {
        this.statusString = statusString;
    }

    @JsonValue
    public String getStatusString() {
        return statusString;
    }
}

