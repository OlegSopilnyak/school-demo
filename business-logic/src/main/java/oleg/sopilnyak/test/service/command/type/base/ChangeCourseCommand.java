package oleg.sopilnyak.test.service.command.type.base;

import oleg.sopilnyak.test.school.common.exception.CourseNotExistsException;
import oleg.sopilnyak.test.school.common.facade.peristence.students.courses.CoursesPersistenceFacade;
import oleg.sopilnyak.test.school.common.model.Course;
import org.slf4j.Logger;

import java.util.Optional;

/**
 * Type for update school-course command
 */
public interface ChangeCourseCommand {
    /**
     * To get reference to command's persistence facade
     *
     * @return reference to the persistence facade
     */
    CoursesPersistenceFacade getPersistenceFacade();

    /**
     * To get reference to command's logger
     *
     * @return reference to the logger
     */
    Logger getLog();

    /**
     * To cache into context old value of the course instance for possible rollback
     *
     * @param inputId system-id of the course
     * @throws CourseNotExistsException if student is not exist
     * @see CoursesPersistenceFacade
     * @see CoursesPersistenceFacade#findCourseById(Long)
     * @see CoursesPersistenceFacade#toEntity(Course)
     * @see Context
     * @see Context#setUndoParameter(Object)
     */
    default Object cacheEntityForRollback(Long inputId) throws CourseNotExistsException {
        final Course existsEntity = getPersistenceFacade().findCourseById(inputId)
                .orElseThrow(() -> new CourseNotExistsException("Course with ID:" + inputId + " is not exists."));
        // return copy of exists entity for undo operation
        return getPersistenceFacade().toEntity(existsEntity);
    }

    /**
     * To restore course entity from cache(context)
     *
     * @param context command execution context
     */
    default void rollbackCachedEntity(Context<?> context) {
        final Object undoParameter = context.getUndoParameter();
        if (undoParameter instanceof Course course) {
            getLog().debug("Restoring changed value of course {}", course);
            getPersistenceFacade().save(course);
        }
    }

    /**
     * To persist entity
     *
     * @param context command's do context
     * @return saved instance or empty
     * @see Course
     * @see Optional#empty()
     */
    default Optional<Course> persistRedoEntity(Context<?> context) {
        final Object input = context.getRedoParameter();
        if (input instanceof Course course) {
            return getPersistenceFacade().save(course);
        } else {
            final String message = "Wrong type of course :" + input.getClass().getName();
            final Exception saveError = new CourseNotExistsException(message);
            saveError.fillInStackTrace();
            getLog().error(message, saveError);
            context.failed(saveError);
            return Optional.empty();
        }
    }
}
