package oleg.sopilnyak.test.school.common.exception.core;

/**
 * Exception throws when command-executor cannot execute the command
 */
public class InvalidParameterTypeException extends RuntimeException {
    private static final String MESSAGE_TEMPLATE = "Parameter not a '%s' value:[%s]";

    /**
     * Constructs a new instance of th exception with the specified detail command-id and
     * cause.  <p>Note that the detail message associated with
     * {@code cause} is <i>not</i> automatically incorporated in
     * this runtime exception's detail message.
     *
     * @param expectedParameterType expected type of the parameter
     * @param parameterValue        actual value of the parameter
     * @param cause                 the cause (which is saved for later retrieval by the
     *                              {@link #getCause()} method).  (A {@code null} value is
     *                              permitted, and indicates that the cause is nonexistent or
     *                              unknown.)
     */
    public InvalidParameterTypeException(String expectedParameterType, Object parameterValue, Throwable cause) {
        super(makeMessageFor(expectedParameterType, parameterValue), cause);
    }

    /**
     * Constructs a new instance of th exception with the specified detail command-id.
     *
     * @param expectedParameterType expected type of the parameter
     * @param parameterValue        actual value of the parameter
     */
    public InvalidParameterTypeException(String expectedParameterType, Object parameterValue) {
        super(makeMessageFor(expectedParameterType, parameterValue));
    }

    private static String makeMessageFor(String expectedParameterType, Object parameterValue) {
        assert expectedParameterType != null && !expectedParameterType.isBlank() : "expected parameter type is invalid";
        return String.format(MESSAGE_TEMPLATE, expectedParameterType, parameterValue);
    }
}
