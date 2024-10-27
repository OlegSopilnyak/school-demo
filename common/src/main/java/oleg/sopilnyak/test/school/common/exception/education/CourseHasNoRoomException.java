package oleg.sopilnyak.test.school.common.exception.education;

import oleg.sopilnyak.test.school.common.exception.EntityUnableProcessException;

/**
 * Exception: throws when you want to register student to the course where there is no free slots for student
 */
public class CourseHasNoRoomException extends EntityUnableProcessException {
    public CourseHasNoRoomException(String message) {
        super(message);
    }

    public CourseHasNoRoomException(String message, Throwable cause) {
        super(message, cause);
    }
}
