package oleg.sopilnyak.test.service.command.executable.course;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import oleg.sopilnyak.test.school.common.exception.core.InvalidParameterTypeException;
import oleg.sopilnyak.test.school.common.exception.education.CourseNotFoundException;
import oleg.sopilnyak.test.school.common.model.Course;
import oleg.sopilnyak.test.school.common.persistence.education.CoursesPersistenceFacade;
import oleg.sopilnyak.test.school.common.persistence.utility.PersistenceFacadeUtilities;
import oleg.sopilnyak.test.service.command.executable.cache.SchoolCommandCache;
import oleg.sopilnyak.test.service.command.executable.sys.CommandContext;
import oleg.sopilnyak.test.service.command.io.Input;
import oleg.sopilnyak.test.service.command.type.education.CourseCommand;
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
@Getter
public class CreateOrUpdateCourseCommand extends SchoolCommandCache<Course>
        implements CourseCommand<Optional<Course>> {
    private final transient CoursesPersistenceFacade persistence;
    private final transient BusinessMessagePayloadMapper payloadMapper;

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
     * @see CourseNotFoundException
     */
    @Override
    public void executeDo(Context<Optional<Course>> context) {
        final Input<Course> parameter = context.getRedoParameter();
        try {
            checkNullParameter(parameter);
            final Course doParameter = parameter.value();
            final Long id = doParameter.getId();
            final boolean isCreateEntityMode = PersistenceFacadeUtilities.isInvalidId(id);
            if (!isCreateEntityMode) {
                log.debug("Trying to update course using: {}", doParameter);
                // previous version of course is storing to context for further rollback (undo)
                final Course entity = retrieveEntity(
                        id, persistence::findCourseById, payloadMapper::toPayload,
                        () -> new CourseNotFoundException(COURSE_WITH_ID_PREFIX + id + " is not exists.")
                );
                log.debug("Previous value of the entity stored for possible command's undo: {}", entity);
                if (context instanceof CommandContext<Optional<Course>> commandContext) {
                    commandContext.setUndoParameter(Input.of(entity));
                }
            } else {
                log.debug("Trying to create course using: {}", doParameter);
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
     * @see Input
     * @see Context#getUndoParameter()
     * @see CoursesPersistenceFacade#save(Course)
     * @see CoursesPersistenceFacade#deleteCourse(Long)
     * @see SchoolCommandCache#rollbackCachedEntity(Context, Function, LongConsumer)
     * @see InvalidParameterTypeException
     */
    @Override
    public void executeUndo(Context<?> context) {
        final Input<?> parameter = context.getUndoParameter();
        try {
            checkNullParameter(parameter);
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
