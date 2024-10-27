package oleg.sopilnyak.test.school.common.exception;

/**
 * Exception: throws when you want to delete or update entity which is not exists
 */
public abstract class EntityNotFoundException extends RuntimeException {
    protected EntityNotFoundException(String message) {
        super(message);
    }
    protected EntityNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
