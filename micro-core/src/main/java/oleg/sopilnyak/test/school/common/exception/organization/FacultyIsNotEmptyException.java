package oleg.sopilnyak.test.school.common.exception.organization;

import oleg.sopilnyak.test.school.common.exception.core.GeneralCannotDeleteException;

/**
 * Exception: throws when you want to delete faculty which has courses
 */
public class FacultyIsNotEmptyException extends GeneralCannotDeleteException {
    public FacultyIsNotEmptyException(String message) {
        super(message);
    }

    public FacultyIsNotEmptyException(String message, Throwable cause) {
        super(message, cause);
    }
}
