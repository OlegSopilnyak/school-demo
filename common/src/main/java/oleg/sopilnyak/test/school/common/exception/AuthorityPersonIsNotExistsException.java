package oleg.sopilnyak.test.school.common.exception;

/**
 * Exception: throws when you want to delete authority person who is not created before
 */
public class AuthorityPersonIsNotExistsException extends Exception {
    /**
     * Constructs a new exception with the specified detail message.  The
     * cause is not initialized, and may subsequently be initialized by
     * a call to {@link #initCause}.
     *
     * @param message the detail message. The detail message is saved for
     *                later retrieval by the {@link #getMessage()} method.
     */
    public AuthorityPersonIsNotExistsException(String message) {
        super(message);
    }
}
