package de.tuberlin.batchjoboperator.common.crd.batchjob;

import com.fasterxml.jackson.annotation.JsonValue;

public enum BatchJobState {
    InitState(null),
    // BatchJob was created
    ReadyState("ReadyState"),
    PendingDeletion("PendingDeletion"),
    // BatchJob was put into the Queue
    InQueueState("InQueueState"),
    // BatchJob was scheduled an SparkApplication is pending
    SubmittedState("SubmittedState"),
    ScheduledState("ScheduledState"),
    // BatchJob was scheduled an SparkApplication is running
    RunningState("RunningState"),
    // BatchJob was scheduled an SparkApplication is completed and no requeue happned
    CompletedState("CompletedState"),
    // BatchJob was scheduled an SparkApplication has failed
    FailedState("FailedState"),
    // Scheduling was not successful
    FailedSubmissionState("FailedSubmissionState"),
    UnknownState("UnknownState");

    private final String state;

    BatchJobState(String state) {
        this.state = state;
    }

    @JsonValue
    String getState() {
        return state;
    }
}
