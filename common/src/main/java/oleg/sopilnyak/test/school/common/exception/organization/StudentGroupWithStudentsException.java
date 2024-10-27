package oleg.sopilnyak.test.school.common.exception.organization;

import oleg.sopilnyak.test.school.common.exception.core.GeneralCannotDeleteException;

/**
 * Exception: throws when you want to delete students group with students
 */
public class StudentGroupWithStudentsException extends GeneralCannotDeleteException {
    public StudentGroupWithStudentsException(String message) {
        super(message);
    }

    public StudentGroupWithStudentsException(String message, Throwable cause) {
        super(message, cause);
    }
}
