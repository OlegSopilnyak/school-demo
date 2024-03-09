package oleg.sopilnyak.test.school.common.exception;

/**
 * Exception: throws when you want to delete faculty which is not created before
 * @see oleg.sopilnyak.test.school.common.model.Faculty
 */
public class FacultyNotExistsException extends Exception {
    /**
     * Constructs a new exception with the specified detail message.  The
     * cause is not initialized, and may subsequently be initialized by
     * a call to {@link #initCause}.
     *
     * @param message the detail message. The detail message is saved for
     *                later retrieval by the {@link #getMessage()} method.
     */
    public FacultyNotExistsException(String message) {
        super(message);
    }
}
