package de.tuberlin.batchjoboperator.batchjobreconciler.reconciler;

import de.tuberlin.batchjoboperator.common.OnCondition;
import de.tuberlin.batchjoboperator.common.State;
import de.tuberlin.batchjoboperator.common.StateMachine;
import de.tuberlin.batchjoboperator.common.crd.batchjob.BatchJobState;
import lombok.extern.slf4j.Slf4j;

import static de.tuberlin.batchjoboperator.batchjobreconciler.reconciler.conditions.BatchJobCondition.AWAIT_COMPLETION_CONDITION;
import static de.tuberlin.batchjoboperator.batchjobreconciler.reconciler.conditions.BatchJobCondition.AWAIT_CREATION_REQUEST_CONDITION;
import static de.tuberlin.batchjoboperator.batchjobreconciler.reconciler.conditions.BatchJobCondition.AWAIT_DELETION_CONDITION;
import static de.tuberlin.batchjoboperator.batchjobreconciler.reconciler.conditions.BatchJobCondition.AWAIT_ENQUEUE_REQUEST_CONDITION;
import static de.tuberlin.batchjoboperator.batchjobreconciler.reconciler.conditions.BatchJobCondition.AWAIT_POD_SCHEDULED_CONDITION;
import static de.tuberlin.batchjoboperator.batchjobreconciler.reconciler.conditions.BatchJobCondition.AWAIT_RELEASE_CONDITION;
import static de.tuberlin.batchjoboperator.batchjobreconciler.reconciler.conditions.BatchJobCondition.AWAIT_RUNNING_CONDITION;
import static de.tuberlin.batchjoboperator.common.crd.batchjob.BatchJobState.CompletedState;
import static de.tuberlin.batchjoboperator.common.crd.batchjob.BatchJobState.InQueueState;
import static de.tuberlin.batchjoboperator.common.crd.batchjob.BatchJobState.ReadyState;
import static de.tuberlin.batchjoboperator.common.crd.batchjob.BatchJobState.RunningState;
import static de.tuberlin.batchjoboperator.common.crd.batchjob.BatchJobState.ScheduledState;

@Slf4j
public class BatchJobStateMachine {
    public static final StateMachine<BatchJobContext> STATE_MACHINE = StateMachine.of(

            /*
             * ReadyState:
             * - Wait for the Enqueue Request to go into the InQueueState
             */
            State.<BatchJobContext>withName(ReadyState.name())
                 .condition(OnCondition.any(
                         (conditions, context) -> log.info("Enqueue " +
                                 "Requested: Moved to InQueueState"),
                         InQueueState.name(),
                         AWAIT_ENQUEUE_REQUEST_CONDITION
                 )).build(),

            /*
             * This Represents the "NotInQueue" State:
             * - If the Current user of the BatchJob Releases the Job it will go back into the ReadyState
             */
            State.<BatchJobContext>anonymous()
                 .condition(OnCondition.any(
                         BatchJobReconciler::releaseRequest,
                         ReadyState.name(),
                         AWAIT_RELEASE_CONDITION
                 ))
                 /*
                  * InQueueState:
                  * - Wait for the CreationRequest then move to the SubmittedState
                  */
                 .subState(State.<BatchJobContext>withName(InQueueState.name())
                                .condition(OnCondition.any(
                                        BatchJobReconciler::creationRequest,
                                        BatchJobState.SubmittedState.name(),
                                        AWAIT_CREATION_REQUEST_CONDITION
                                ))
                                .build())
                 .subState(
                         /*
                          * This Represents the "ApplicationExisting" State:
                          * - If the Application is deleted we go back to the ReadyState
                          */
                         State.<BatchJobContext>anonymous()
                              /*
                               * This Represents the "NotCompleted" State:
                               * - If the Application is Completed we may skip states and go directly to the completed
                               * State
                               * - This might happen if the Application is short running and the controller skips
                               * some events
                               */
                              .subState(State.<BatchJobContext>anonymous()
                                             .condition(OnCondition.any(
                                                     BatchJobReconciler.stopRunningEvent(true),
                                                     CompletedState.name(),
                                                     AWAIT_COMPLETION_CONDITION
                                             ))
                                             /*
                                              * SubmittedState:
                                              * - Wait for the Application Pods to be scheduled to go to the
                                              * ScheduledState
                                              *
                                              * - SubmittedState is important for the user to know when all the pods
                                              * are scheduled and the Status of the Slot can be 'trusted' again to
                                              * contain the current SlotOccupation
                                              */
                                             .subState(
                                                     State.<BatchJobContext>withName(BatchJobState.SubmittedState.name())
                                                          .condition(OnCondition.any(
                                                                  (conditions, context) -> log.info("All Pods Are " +
                                                                          "scheduled"),
                                                                  ScheduledState.name(),
                                                                  AWAIT_POD_SCHEDULED_CONDITION
                                                          ))
                                                          .build()
                                             )/*
                                              * ScheduledState:
                                              * - Wait for the Application to start Running
                                              */
                                             .subState(State.<BatchJobContext>withName(ScheduledState.name())
                                                            .condition(OnCondition.any(
                                                                    BatchJobReconciler::startRunningEvent,
                                                                    RunningState.name(),
                                                                    AWAIT_RUNNING_CONDITION
                                                            ))
                                                            .build())
                                             /*
                                              * RunningState:
                                              * - Remain in Running State until the application has completed
                                              */
                                             .subState(State.<BatchJobContext>withName(RunningState.name())
                                                            .build())
                                             .build())
                              //TODO: I don't think the errorState is implemented correctly nor is it used at the
                              // moment. Ideally one could define different ErrorStates for different States.
                              // There might be different ErrorStates for an Application that cannot be scheduled, than
                              // for an Application, that fails during Runtime. Some of the errors might be resolved by
                              // waiting, some by restarting, others may be permanent and require more work to be
                              // resolved.
                              // Defining the Error state as a 'Catch' State, rather than defining it as an actual state
                              // with conditions that can cause transition into the ErrorState, brings the benefit of
                              // not explicitly creating Conditions for all kinds of Errors. Conditions can return an
                              // error which will be picked up by the State machine and cause transition to the closest
                              // parent ErrorState or the defaultErrorState defined by the StateMachine
                              .errorState(State.<BatchJobContext>withName(BatchJobState.FailedState.name()).build())
                              .condition(OnCondition.any(
                                      BatchJobReconciler.stopRunningEvent(false),
                                      ReadyState.name(),
                                      AWAIT_DELETION_CONDITION
                              ))
                              .subState(State.<BatchJobContext>withName(CompletedState.name()).build())
                              .build())
                 .build()
    );

}
