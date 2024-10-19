package oleg.sopilnyak.test.school.common.exception.organization;

/**
 * Exception: throws when you want to delete authority person who is the dean of a faculty now
 */
public class AuthorityPersonManagesFacultyException extends RuntimeException {
    /**
     * Constructs a new exception with the specified detail message.  The
     * cause is not initialized, and may subsequently be initialized by
     * a call to {@link #initCause}.
     *
     * @param message the detail message. The detail message is saved for
     *                later retrieval by the {@link #getMessage()} method.
     */
    public AuthorityPersonManagesFacultyException(String message) {
        super(message);
    }
}
