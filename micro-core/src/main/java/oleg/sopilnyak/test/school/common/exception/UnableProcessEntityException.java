package oleg.sopilnyak.test.school.common.exception;

/**
 * Exception: throws when you want to update or delete entity which is not applicable for the operation
 */
public abstract class UnableProcessEntityException extends RuntimeException {
    protected UnableProcessEntityException(String message) {
        super(message);
    }
    protected UnableProcessEntityException(String message, Throwable cause) {
        super(message, cause);
    }
}
