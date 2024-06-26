package oleg.sopilnyak.test.school.common.exception;

/**
 * Exception: throws when you want to delete or update profile which is not created before
 *
 * @see EntityNotExistException
 * @see oleg.sopilnyak.test.school.common.model.base.PersonProfile
 */
public class NotExistProfileException extends EntityNotExistException {
    /**
     * Constructs a new exception with the specified detail message.  The
     * cause is not initialized, and may subsequently be initialized by
     * a call to {@link #initCause}.
     *
     * @param message the detail message. The detail message is saved for
     *                later retrieval by the {@link #getMessage()} method.
     */
    public NotExistProfileException(String message) {
        super(message);
    }
}
