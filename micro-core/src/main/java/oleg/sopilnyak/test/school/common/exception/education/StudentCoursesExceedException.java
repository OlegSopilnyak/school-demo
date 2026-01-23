package oleg.sopilnyak.test.school.common.exception.education;

import oleg.sopilnyak.test.school.common.exception.EntityUnableProcessException;

/**
 * Exception: throws when you want to register student who already registered to a lot of courses
 */
public class StudentCoursesExceedException extends EntityUnableProcessException {
    public StudentCoursesExceedException(String message) {
        super(message);
    }

    public StudentCoursesExceedException(String message, Throwable cause) {
        super(message, cause);
    }
}
