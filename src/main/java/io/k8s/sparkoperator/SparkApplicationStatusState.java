package io.k8s.sparkoperator;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum SparkApplicationStatusState {
    @JsonProperty()
    NewState(""),
    @JsonProperty("SUBMITTED")
    SubmittedState("SUBMITTED"),
    @JsonProperty("RUNNING")
    RunningState("RUNNING"),
    @JsonProperty("COMPLETED")
    CompletedState("COMPLETED"),
    @JsonProperty("FAILED")
    FailedState("FAILED"),
    @JsonProperty("SUBMISSION_FAILED")
    FailedSubmissionState("SUBMISSION_FAILED"),
    @JsonProperty("PENDING_RERUN")
    PendingRerunState("PENDING_RERUN"),
    @JsonProperty("INVALIDATING")
    InvalidatingState("INVALIDATING"),
    @JsonProperty("SUCCEEDING")
    SucceedingState("SUCCEEDING"),
    @JsonProperty("FAILING")
    FailingState("FAILING"),
    @JsonProperty("UNKNOWN")
    UnknownState("UNKNOWN");

    @JsonValue
    private final String state;
}
