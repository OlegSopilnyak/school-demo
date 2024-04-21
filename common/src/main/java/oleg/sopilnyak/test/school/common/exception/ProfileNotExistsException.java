package oleg.sopilnyak.test.school.common.exception;

import oleg.sopilnyak.test.school.common.model.base.PersonProfile;

/**
 * Exception: throws when you want to delete profile which is not created before
 * @see PersonProfile
 */
public class ProfileNotExistsException extends Exception {
    /**
     * Constructs a new exception with the specified detail message.  The
     * cause is not initialized, and may subsequently be initialized by
     * a call to {@link #initCause}.
     *
     * @param message the detail message. The detail message is saved for
     *                later retrieval by the {@link #getMessage()} method.
     */
    public ProfileNotExistsException(String message) {
        super(message);
    }
}
