package oleg.sopilnyak.test.school.common.exception;

/**
 * Exception: throws when you want to update or delete entity which is not applicable for the operation
 */
public abstract class EntityUnableProcessException extends RuntimeException {
    protected EntityUnableProcessException(String message) {
        super(message);
    }
    protected EntityUnableProcessException(String message, Throwable cause) {
        super(message, cause);
    }
}
