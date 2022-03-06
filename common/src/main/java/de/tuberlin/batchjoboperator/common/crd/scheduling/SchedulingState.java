package de.tuberlin.batchjoboperator.common.crd.scheduling;

public enum SchedulingState {
    InitialState,
    AcquireState,
    CompletedState,
    FinishedState,
    SubmissionState,
    ConfirmationState,
    AwaitingCompletionState,
    Error
}
