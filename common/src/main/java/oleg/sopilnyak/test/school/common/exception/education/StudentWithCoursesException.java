package oleg.sopilnyak.test.school.common.exception.education;

import oleg.sopilnyak.test.school.common.exception.core.GeneralCannotDeleteException;

/**
 * Exception: throws when you want to delete course with registered students
 */
public class StudentWithCoursesException extends GeneralCannotDeleteException {
    public StudentWithCoursesException(String message) {
        super(message);
    }

    public StudentWithCoursesException(String message, Throwable cause) {
        super(message, cause);
    }
}
