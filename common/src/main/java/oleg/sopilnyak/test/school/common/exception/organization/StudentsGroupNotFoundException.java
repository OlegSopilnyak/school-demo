package oleg.sopilnyak.test.school.common.exception.organization;

import oleg.sopilnyak.test.school.common.exception.EntityNotFoundException;

/**
 * Exception: throws when you want to delete or update students group which is not created before
 *
 * @see EntityNotFoundException
 * @see oleg.sopilnyak.test.school.common.model.StudentsGroup
 */
public class StudentsGroupNotFoundException extends EntityNotFoundException {
    /**
     * Constructs a new exception with the specified detail message.  The
     * cause is not initialized, and may subsequently be initialized by
     * a call to {@link #initCause}.
     *
     * @param message the detail message. The detail message is saved for
     *                later retrieval by the {@link #getMessage()} method.
     */
    public StudentsGroupNotFoundException(String message) {
        super(message);
    }
}
