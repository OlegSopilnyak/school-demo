package oleg.sopilnyak.test.endpoint.exception;

/**
 * Exception occurred if system cannot delete resource
 */
public class CannotRegisterToCourseException extends RuntimeException {

    public CannotRegisterToCourseException(String message, Throwable cause) {
        super(message, cause);
    }
}
