package oleg.sopilnyak.test.school.common.exception.profile;

import oleg.sopilnyak.test.school.common.exception.EntityIsNotFoundException;

/**
 * Exception: throws when you want to delete or update profile which is not created before
 *
 * @see EntityIsNotFoundException
 * @see oleg.sopilnyak.test.school.common.model.base.PersonProfile
 */
public class ProfileIsNotFoundException extends EntityIsNotFoundException {
    /**
     * Constructs a new exception with the specified detail message.  The
     * cause is not initialized, and may subsequently be initialized by
     * a call to {@link #initCause}.
     *
     * @param message the detail message. The detail message is saved for
     *                later retrieval by the {@link #getMessage()} method.
     */
    public ProfileIsNotFoundException(String message) {
        super(message);
    }
}
