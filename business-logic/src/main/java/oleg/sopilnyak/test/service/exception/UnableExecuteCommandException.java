package oleg.sopilnyak.test.service.exception;

import static java.util.Objects.nonNull;

import oleg.sopilnyak.test.service.command.type.base.RootCommand;

/**
 * Exception throws when command-executor cannot execute the command
 */
public class UnableExecuteCommandException extends RuntimeException {
    private static final String PREFIX = "Cannot execute command '";
    private static final String SUFFIX = "'";

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
     * @see RootCommand#getId()
     */
    public UnableExecuteCommandException(String commandId, Throwable cause) {
        super(properMessage(commandId), cause);
    }

    /**
     * Constructs a new instance of th exception with the specified detail command-id.
     *
     * @param commandId the id of the school-command
     * @see RootCommand#getId()
     */
    public UnableExecuteCommandException(String commandId) {
        super(properMessage(commandId));
    }

    // private methods
    private static String properMessage(String message) {
        return nonNull(message) && message.startsWith(PREFIX) ? message : PREFIX + message + SUFFIX;
    }
}
