package oleg.sopilnyak.test.endpoint.exception;

/**
 * Exception occurred if system cannot delete resource
 */
public class CannotDeleteResourceException extends RuntimeException {

    public CannotDeleteResourceException(String message, Throwable cause) {
        super(message, cause);
    }
}
