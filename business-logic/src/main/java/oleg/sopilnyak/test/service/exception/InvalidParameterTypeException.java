package oleg.sopilnyak.test.service.exception;

/**
 * Exception throws when command-executor cannot execute the command
 */
public class InvalidParameterTypeException extends RuntimeException {
    /**
     * Constructs a new instance of th exception with the specified detail command-id and
     * cause.  <p>Note that the detail message associated with
     * {@code cause} is <i>not</i> automatically incorporated in
     * this runtime exception's detail message.
     *
     * @param waitingForType correct type of the parameter
     * @param parameterValue actual value of the parameter
     * @param cause          the cause (which is saved for later retrieval by the
     *                       {@link #getCause()} method).  (A {@code null} value is
     *                       permitted, and indicates that the cause is nonexistent or
     *                       unknown.)
     */
    public InvalidParameterTypeException(String waitingForType, Object parameterValue, Throwable cause) {
        super("Parameter not a  '" + waitingForType + "' value:[" + parameterValue + "]", cause);
    }

    /**
     * Constructs a new instance of th exception with the specified detail command-id.
     *
     * @param waitingForType correct type of the parameter
     * @param parameterValue actual value of the parameter
     */
    public InvalidParameterTypeException(String waitingForType, Object parameterValue) {
        super("Parameter not a  '" + waitingForType + "' value:[" + parameterValue + "]");
    }
}
