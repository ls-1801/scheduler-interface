package de.tuberlin.batchjoboperator.common.crd.scheduling;

public enum SchedulingState {
    FailedState,
    InitialState,
    AcquireState,
    CompletedState,
    ConfirmationState,
    FinishedState,
    SubmissionState,
    AwaitingCompletionState,
    Error
}
