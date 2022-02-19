package de.tuberlin.batchjoboperator.reconciler.batchjob.common.statemachine;

import de.tuberlin.batchjoboperator.crd.batchjob.BatchJob;

public class StateProvider {
    public static AbstractState of(BatchJob batchJob) {
        switch (batchJob.getStatus().getState()) {
            case NewState:
                return new InitialState();
            case SubmittedState:
                return new SubmittedState();
            case InQueueState:
                return new InQueueState();
            case RunningState:
                return new RunningState();
            case CompletedState:
                return new CompletedState();
            case FailedState:
                return new FailedState();
            case FailedSubmissionState:
                return new FailedSubmissionState();
            default:
                return new UnknownState();
        }
    }
}
