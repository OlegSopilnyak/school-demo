package oleg.sopilnyak.test.school.common.exception.education;

import oleg.sopilnyak.test.school.common.exception.core.GeneralCannotDeleteException;

/**
 * Exception: throws when you want to delete course with registered students
 */
public class CourseWithStudentsException extends GeneralCannotDeleteException {
    public CourseWithStudentsException(String message) {
        super(message);
    }

    public CourseWithStudentsException(String message, Throwable cause) {
        super(message, cause);
    }
}
