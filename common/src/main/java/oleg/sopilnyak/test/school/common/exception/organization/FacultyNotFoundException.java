package oleg.sopilnyak.test.school.common.exception.organization;

import oleg.sopilnyak.test.school.common.exception.EntityNotFoundException;

/**
 * Exception: throws when you want to delete or update faculty which is not created before
 *
 * @see EntityNotFoundException
 * @see oleg.sopilnyak.test.school.common.model.Faculty
 */
public class FacultyNotFoundException extends EntityNotFoundException {
    public FacultyNotFoundException(String message) {
        super(message);
    }

    public FacultyNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
