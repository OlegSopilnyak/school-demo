package oleg.sopilnyak.test.endpoint.exception;

/**
 * Exception: throws when there is no way to execute REST request
 */
public class CannotDoRestCallException extends RuntimeException {
    public CannotDoRestCallException(String message) {
        super(message);
    }

    public CannotDoRestCallException(String message, Throwable cause) {
        super(message, cause);
    }
}
