package oleg.sopilnyak.test.school.common.exception.education;

import oleg.sopilnyak.test.school.common.exception.EntityNotFoundException;

/**
 * Exception: throws when you want to delete or update course which is not created before
 *
 * @see EntityNotFoundException
 * @see oleg.sopilnyak.test.school.common.model.Course
 */
public class CourseNotFoundException extends EntityNotFoundException {
    public CourseNotFoundException(String message) {
        super(message);
    }

    public CourseNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
