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
            log.debug("Trying to delete course by ID: {}", parameter.toString());
            final Long inputId = commandParameter(parameter);
            final EntityNotExistException notFoundException =
                    new NotExistCourseException(COURSE_WITH_ID_PREFIX + inputId + " is not exists.");
            if (PersistenceFacadeUtilities.isInvalidId(inputId)) {
                throw notFoundException;
            }

            final var entity = retrieveEntity(inputId,
                    persistenceFacade::findCourseById, payloadMapper::toPayload, () -> notFoundException
            );

            if (!ObjectUtils.isEmpty(entity.getStudents())) {
                log.warn(COURSE_WITH_ID_PREFIX + "{} has enrolled students.", inputId);
                throw new CourseWithStudentsException(COURSE_WITH_ID_PREFIX + inputId + " has enrolled students.");
            }

            // cached course is storing to context for further rollback (undo)
            context.setUndoParameter(entity);
            persistenceFacade.deleteCourse(inputId);
            context.setResult(Boolean.TRUE);
        } catch (Exception e) {
            log.error("Cannot delete the course by Id: {}", parameter, e);
            context.failed(e);
            rollbackCachedEntity(context, persistenceFacade::save);
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
            log.debug("Trying to undo course deletion using: {}", parameter.toString());
            final Course course = rollbackCachedEntity(context, persistenceFacade::save)
                    .orElseThrow(() -> new NotExistCourseException("Wrong undo parameter :" + parameter));
            log.debug("Updated in database: '{}'", course);

            context.setState(Context.State.UNDONE);
        } catch (Exception e) {
            log.error("Cannot undo student deletion {}", parameter, e);
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
