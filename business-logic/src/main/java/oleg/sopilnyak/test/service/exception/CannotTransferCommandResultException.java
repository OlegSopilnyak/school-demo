package oleg.sopilnyak.test.service.exception;

/**
 * Exception throws when command-executor cannot transfer result to next command input
 */
public class CannotTransferCommandResultException extends RuntimeException {
    /**
     * Constructs a new runtime exception with the specified detail message.
     * The cause is not initialized, and may subsequently be initialized by a
     * call to {@link #initCause}.
     *
     * @param commandId the id of the school-command
     */
    public CannotTransferCommandResultException(String commandId) {
        super("Cannot transfer result from command: '" + commandId + "'");
    }

    /**
     * Constructs a new runtime exception with the specified detail message and
     * cause.  <p>Note that the detail message associated with
     * {@code cause} is <i>not</i> automatically incorporated in
     * this runtime exception's detail message.
     *
     * @param commandId the id of the school-command
     *                by the {@link #getMessage()} method).
     * @param cause   the cause (which is saved for later retrieval by the
     *                {@link #getCause()} method).  (A {@code null} value is
     *                permitted, and indicates that the cause is nonexistent or
     *                unknown.)
     * @since 1.4
     */
    public CannotTransferCommandResultException(String commandId, Throwable cause) {
        super("Cannot transfer result from command: '" + commandId + "'", cause);
    }
}
