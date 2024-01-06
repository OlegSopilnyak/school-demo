package oleg.sopilnyak.test.service.exception;

import oleg.sopilnyak.test.service.command.type.base.SchoolCommand;

/**
 * Exception throws when command-executor cannot execute the command
 */
public class UnableExecuteCommandException extends RuntimeException {
    /**
     * Constructs a new instance of th exception with the specified detail command-id and
     * cause.  <p>Note that the detail message associated with
     * {@code cause} is <i>not</i> automatically incorporated in
     * this runtime exception's detail message.
     *
     * @param commandId the id of the school-command
     * @param cause     the cause (which is saved for later retrieval by the
     *                  {@link #getCause()} method).  (A {@code null} value is
     *                  permitted, and indicates that the cause is nonexistent or
     *                  unknown.)
     * @see SchoolCommand#getId()
     */
    public UnableExecuteCommandException(String commandId, Throwable cause) {
        super("Cannot execute command '" + commandId + "'", cause);
    }

    /**
     * Constructs a new instance of th exception with the specified detail command-id.
     *
     * @param commandId the id of the school-command
     * @see SchoolCommand#getId()
     */
    public UnableExecuteCommandException(String commandId) {
        super("Cannot execute command '" + commandId + "'");
    }
}
