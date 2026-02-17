package oleg.sopilnyak.test.service.command.executable.education.course;

import oleg.sopilnyak.test.school.common.business.facade.education.CoursesFacade;
import oleg.sopilnyak.test.school.common.exception.EntityNotFoundException;
import oleg.sopilnyak.test.school.common.exception.education.CourseNotFoundException;
import oleg.sopilnyak.test.school.common.exception.education.CourseWithStudentsException;
import oleg.sopilnyak.test.school.common.model.education.Course;
import oleg.sopilnyak.test.school.common.persistence.education.CoursesPersistenceFacade;
import oleg.sopilnyak.test.school.common.persistence.utility.PersistenceFacadeUtilities;
import oleg.sopilnyak.test.service.command.executable.core.cache.SchoolCommandCache;
import oleg.sopilnyak.test.service.command.executable.core.context.CommandContext;
import oleg.sopilnyak.test.service.command.io.Input;
import oleg.sopilnyak.test.service.command.type.core.Context;
import oleg.sopilnyak.test.service.command.type.education.CourseCommand;
import oleg.sopilnyak.test.service.mapper.BusinessMessagePayloadMapper;

import java.util.function.Function;
import java.util.function.LongFunction;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Command-Implementation: command to delete the course by id
 *
 * @see SchoolCommandCache
 * @see Course
 * @see CoursesPersistenceFacade
 */
@Slf4j
@Component(CourseCommand.Component.DELETE)
public class DeleteCourseCommand extends SchoolCommandCache<Course, Boolean> implements CourseCommand<Boolean> {
    private final transient CoursesPersistenceFacade persistenceFacade;
    @Getter
    private final transient BusinessMessagePayloadMapper payloadMapper;

    /**
     * The name of command bean in spring beans factory
     *
     * @return spring name of the command
     */
    @Override
    public String springName() {
        return Component.DELETE;
    }

    /**
     * To get unique command-id for the command
     *
     * @return value of command-id
     */
    @Override
    public String getId() {
        return CoursesFacade.DELETE;
    }

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
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void executeDo(Context<Boolean> context) {
        final Input<Long> parameter = context.getRedoParameter();
        try {
            checkNullParameter(parameter);
            final Long id = parameter.value();
            log.debug("Trying to delete course by ID: {}", id);
            if (PersistenceFacadeUtilities.isInvalidId(id)) {
                log.warn("Invalid id {}", id);
                throw exceptionFor(id);
            }
            // getting from the database current version of the course
            final Course entity = retrieveEntity(
                    id, persistenceFacade::findCourseById, payloadMapper::toPayload, () -> exceptionFor(id)
            );
            if (!ObjectUtils.isEmpty(entity.getStudents())) {
                log.warn(COURSE_WITH_ID_PREFIX + "{} has enrolled students.", id);
                throw new CourseWithStudentsException(COURSE_WITH_ID_PREFIX + id + " has enrolled students.");
            }
            // removing course instance by ID from the database
            persistenceFacade.deleteCourse(id);
            // setup undo parameter for deleted entity
            prepareDeleteEntityUndo(context, entity, () -> exceptionFor(id));
            // successful delete entity operation
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
     * @see SchoolCommandCache#rollbackCachedEntity(Context, Function)
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void executeUndo(Context<?> context) {
        final Input<Course> parameter = context.getUndoParameter();
        try {
            checkNullParameter(parameter);
            log.debug("Trying to undo course deletion using: {}", parameter.value());

            final Course entity = rollbackCachedEntity(context, persistenceFacade::save).orElseThrow();

            log.debug("Updated in database: '{}'", entity);
            // change course-id value for further do command processing
            if (context instanceof CommandContext<?> commandContext) {
                commandContext.setRedoParameter(Input.of(entity.getId()));
            }
            context.setState(Context.State.UNDONE);
        } catch (Exception e) {
            log.error("Cannot undo course deletion {}", parameter, e);
            context.failed(e);
        }
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

    // private methods
    private EntityNotFoundException exceptionFor(final Long id) {
        return new CourseNotFoundException(COURSE_WITH_ID_PREFIX + id + " is not exists.");
    }
}
