package de.tuberlin.batchjoboperator.common.statemachine;

public class AbortStateMachineSilentlyException extends RuntimeException {

    public AbortStateMachineSilentlyException(Throwable cause) {
        super(cause);
    }

    public static void abort(Throwable cause) {
        throw new AbortStateMachineSilentlyException(cause);
    }
}
