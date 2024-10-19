package oleg.sopilnyak.test.service.command.executable.course;

import lombok.extern.slf4j.Slf4j;
import oleg.sopilnyak.test.school.common.exception.education.CourseIsNotFoundException;
import oleg.sopilnyak.test.school.common.model.Course;
import oleg.sopilnyak.test.school.common.persistence.students.courses.CoursesPersistenceFacade;
import oleg.sopilnyak.test.school.common.persistence.utility.PersistenceFacadeUtilities;
import oleg.sopilnyak.test.service.command.executable.cache.SchoolCommandCache;
import oleg.sopilnyak.test.service.command.type.CourseCommand;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.mapper.BusinessMessagePayloadMapper;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.function.*;

/**
 * Command-Implementation: command to create or update the course
 *
 * @see Course
 * @see CourseCommand
 * @see CoursesPersistenceFacade
 * @see SchoolCommandCache
 */
@Slf4j
@Component
public class CreateOrUpdateCourseCommand
        extends SchoolCommandCache<Course>
        implements CourseCommand {
    private final CoursesPersistenceFacade persistence;
    private final BusinessMessagePayloadMapper payloadMapper;

    public CreateOrUpdateCourseCommand(final CoursesPersistenceFacade persistenceFacade,
                                       final BusinessMessagePayloadMapper payloadMapper) {
        super(Course.class);
        this.persistence = persistenceFacade;
        this.payloadMapper = payloadMapper;
    }

    /**
     * DO: To create or update the course<BR/>
     * To execute command redo with correct context state
     *
     * @param context context of redo execution
     * @see Context
     * @see Context.State#WORK
     * @see SchoolCommandCache#retrieveEntity(Long, LongFunction, UnaryOperator, Supplier)
     * @see SchoolCommandCache#persistRedoEntity(Context, Function)
     * @see SchoolCommandCache#rollbackCachedEntity(Context, Function)
     * @see CoursesPersistenceFacade#findCourseById(Long)
     * @see CoursesPersistenceFacade#save(Course)
     * @see CourseIsNotFoundException
     */
    @Override
    public <T> void executeDo(Context<T> context) {
        final Object parameter = context.getRedoParameter();
        try {
            check(parameter);
            log.debug("Trying to create or update course {}", parameter);
            final Long id = ((Course) parameter).getId();
            final boolean isCreateEntityMode = PersistenceFacadeUtilities.isInvalidId(id);
            if (!isCreateEntityMode) {
                // previous version of course is storing to context for further rollback (undo)
                final Course entity = retrieveEntity(
                        id, persistence::findCourseById, payloadMapper::toPayload,
                        () -> new CourseIsNotFoundException(COURSE_WITH_ID_PREFIX + id + " is not exists.")
                );
                log.debug("Previous value of the entity stored for possible command's undo: {}", entity);
                context.setUndoParameter(entity);
            } else {
                log.debug("Trying to create course using: {}", parameter);
            }
            // persisting entity trough persistence layer
            final Optional<Course> persisted = persistRedoEntity(context, persistence::save);
            // checking command context state after entity persistence
            afterEntityPersistenceCheck(
                    context, () -> rollbackCachedEntity(context, persistence::save),
                    persisted.orElse(null), isCreateEntityMode
            );
        } catch (Exception e) {
            log.error("Cannot create or update course by ID:{}", parameter, e);
            context.failed(e);
            restoreInitialCommandState(context, persistence::save);
        }
    }

    /**
     * UNDO: To create or update the course<BR/>
     * To rollback command's execution with correct context state
     *
     * @param context context of redo execution
     * @see Context
     * @see Context#getUndoParameter()
     * @see CoursesPersistenceFacade#save(Course)
     * @see CoursesPersistenceFacade#deleteCourse(Long)
     * @see SchoolCommandCache#rollbackCachedEntity(Context, Function, LongConsumer)
     * @see oleg.sopilnyak.test.service.exception.InvalidParameterTypeException
     */
    @Override
    public <T> void executeUndo(Context<T> context) {
        final Object parameter = context.getUndoParameter();
        try {
            check(parameter);
            log.debug("Trying to undo course changes using: {}", parameter);

            rollbackCachedEntity(context, persistence::save, persistence::deleteCourse);

            context.setState(Context.State.UNDONE);
        } catch (Exception e) {
            log.error("Cannot undo course change {}", parameter, e);
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
        return CREATE_OR_UPDATE;
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
