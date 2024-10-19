package oleg.sopilnyak.test.school.common.exception.education;

/**
 * Exception: throws when you want to register student to the course where there is no free slots for student
 */
public class CourseHasNoRoomException extends Exception {
    /**
     * Constructs a new exception with the specified detail message.  The
     * cause is not initialized, and may subsequently be initialized by
     * a call to {@link #initCause}.
     *
     * @param message the detail message. The detail message is saved for
     *                later retrieval by the {@link #getMessage()} method.
     */
    public CourseHasNoRoomException(String message) {
        super(message);
    }
}
