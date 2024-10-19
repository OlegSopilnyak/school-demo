package oleg.sopilnyak.test.school.common.exception;

/**
 * Exception: throws when you want to delete or update entity which is not exists
 * @see oleg.sopilnyak.test.school.common.model.base.BaseType
 */
public class EntityIsNotFoundException extends RuntimeException {
    /**
     * Constructs a new runtime exception with the specified detail message.
     * The cause is not initialized, and may subsequently be initialized by a
     * call to {@link #initCause}.
     *
     * @param message the detail message. The detail message is saved for
     *                later retrieval by the {@link #getMessage()} method.
     */
    public EntityIsNotFoundException(String message) {
        super(message);
    }
}
