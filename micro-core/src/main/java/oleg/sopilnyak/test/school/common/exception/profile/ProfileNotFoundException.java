package oleg.sopilnyak.test.school.common.exception.profile;

import oleg.sopilnyak.test.school.common.exception.EntityNotFoundException;
import oleg.sopilnyak.test.school.common.model.person.profile.PersonProfile;

/**
 * Exception: throws when you want to delete or update profile which is not created before
 *
 * @see EntityNotFoundException
 * @see PersonProfile
 */
public class ProfileNotFoundException extends EntityNotFoundException {
    /**
     * Constructs a new exception with the specified detail message.  The
     * cause is not initialized, and may subsequently be initialized by
     * a call to {@link #initCause}.
     *
     * @param message the detail message. The detail message is saved for
     *                later retrieval by the {@link #getMessage()} method.
     */
    public ProfileNotFoundException(String message) {
        super(message);
    }
}
