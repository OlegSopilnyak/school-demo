package oleg.sopilnyak.test.service.command.executable.course;

import lombok.extern.slf4j.Slf4j;
import oleg.sopilnyak.test.school.common.exception.CourseWithStudentsException;
import oleg.sopilnyak.test.school.common.exception.EntityNotExistException;
import oleg.sopilnyak.test.school.common.exception.NotExistCourseException;
import oleg.sopilnyak.test.school.common.model.Course;
import oleg.sopilnyak.test.school.common.persistence.students.courses.CoursesPersistenceFacade;
import oleg.sopilnyak.test.school.common.persistence.utility.PersistenceFacadeUtilities;
import oleg.sopilnyak.test.service.command.executable.cache.SchoolCommandCache;
import oleg.sopilnyak.test.service.command.type.CourseCommand;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.mapper.BusinessMessagePayloadMapper;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import java.util.function.Function;
import java.util.function.LongFunction;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

/**
 * Command-Implementation: command to delete the course by id
 *
 * @see SchoolCommandCache
 * @see Course
 * @see CoursesPersistenceFacade
 */
@Slf4j
@Component
public class DeleteCourseCommand extends SchoolCommandCache<Course> implements CourseCommand {
    private final CoursesPersistenceFacade persistenceFacade;
    private final BusinessMessagePayloadMapper payloadMapper;

    public DeleteCourseCommand(final CoursesPersistenceFacade persistenceFacade,
                               final BusinessMessagePayloadMapper payloadMapper) {
        super(Course.class);
        this.persistenceFacade = persistenceFacade;
        this.payloadMapper = payloadMapper;
    }

    /**
     * DO: To delete the course by id<BR/>
     * To execute command redo with correct context state
     *
     * @param context context of redo execution
     * @see Context
     * @see Context#getRedoParameter()
     * @see Context.State#WORK
     * @see SchoolCommandCache#retrieveEntity(Long, LongFunction, UnaryOperator, Supplier)
     * @see SchoolCommandCache#rollbackCachedEntity(Context, Function)
     */
    @Override
    public <T> void executeDo(Context<T> context) {
        final Object parameter = context.getRedoParameter();
        try {
            check(parameter);
            log.debug("Trying to delete course by ID: {}", parameter);
            final Long id = commandParameter(parameter);
            final EntityNotExistException notFoundException =
                    new NotExistCourseException(COURSE_WITH_ID_PREFIX + id + " is not exists.");
            if (PersistenceFacadeUtilities.isInvalidId(id)) {
                throw notFoundException;
            }
            // getting from the database current version of the course
            final var entity = retrieveEntity(
                    id, persistenceFacade::findCourseById, payloadMapper::toPayload, () -> notFoundException
            );

            if (!ObjectUtils.isEmpty(entity.getStudents())) {
                log.warn(COURSE_WITH_ID_PREFIX + "{} has enrolled students.", id);
                throw new CourseWithStudentsException(COURSE_WITH_ID_PREFIX + id + " has enrolled students.");
            }
            // removing course instance by ID from the database
            persistenceFacade.deleteCourse(id);
            // cached course is storing to context for further rollback (undo)
            context.setUndoParameter(entity);
            context.setResult(Boolean.TRUE);
            getLog().debug("Deleted course with ID: {}", id);
        } catch (Exception e) {
            log.error("Cannot delete the course by Id: {}", parameter, e);
            context.failed(e);
        }
    }

    /**
     * UNDO: To delete the course by id<BR/>
     * To rollback command's execution with correct context state
     *
     * @param context context of redo execution
     * @see Context
     * @see Context#getUndoParameter()
     * @see this#rollbackCachedEntity(Context, Function)
     */
    @Override
    public <T> void executeUndo(Context<T> context) {
        final Object parameter = context.getUndoParameter();
        try {
            check(parameter);
            log.debug("Trying to undo course deletion using: {}", parameter);

            final Course entity = rollbackCachedEntity(context, persistenceFacade::save).orElseThrow();

            log.debug("Updated in database: '{}'", entity);
            // change course-id value for further do command action
            context.setRedoParameter(entity.getId());
            context.setState(Context.State.UNDONE);
        } catch (Exception e) {
            log.error("Cannot undo course deletion {}", parameter, e);
            context.failed(e);
        }
    }

    /**
     * To get unique command-id for the command
     *
     * @return value of command-id
     */
    @Override
    public String getId() {
        return DELETE;
    }

    /**
     * To get reference to command's logger
     *
     * @return reference to the logger
     */
    @Override
    public Logger getLog() {
        return log;
    }
}
