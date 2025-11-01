package oleg.sopilnyak.test.service.command.executable.education.course;

import static java.util.Objects.isNull;

import oleg.sopilnyak.test.school.common.exception.EntityNotFoundException;
import oleg.sopilnyak.test.school.common.exception.education.CourseNotFoundException;
import oleg.sopilnyak.test.school.common.exception.education.CourseWithStudentsException;
import oleg.sopilnyak.test.school.common.model.Course;
import oleg.sopilnyak.test.school.common.persistence.education.CoursesPersistenceFacade;
import oleg.sopilnyak.test.school.common.persistence.utility.PersistenceFacadeUtilities;
import oleg.sopilnyak.test.service.command.executable.cache.SchoolCommandCache;
import oleg.sopilnyak.test.service.command.executable.sys.CommandContext;
import oleg.sopilnyak.test.service.command.io.Input;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.command.type.base.RootCommand;
import oleg.sopilnyak.test.service.command.type.education.CourseCommand;
import oleg.sopilnyak.test.service.mapper.BusinessMessagePayloadMapper;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.LongFunction;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
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
@Getter
@Component("courseDelete")
public class DeleteCourseCommand extends SchoolCommandCache<Course> implements CourseCommand<Boolean> {
    private final transient CoursesPersistenceFacade persistenceFacade;
    private final transient BusinessMessagePayloadMapper payloadMapper;
    @Autowired
    // beans factory to prepare the current command for transactional operations
    private transient ApplicationContext applicationContext;
    // reference to current command for transactional operations
    private final AtomicReference<CourseCommand<Boolean>> self = new AtomicReference<>(null);

    /**
     * Reference to the current command for transactional operations
     *
     * @return reference to the current command
     * @see RootCommand#self()
     * @see RootCommand#doCommand(Context)
     * @see RootCommand#undoCommand(Context)
     */
    @Override
    @SuppressWarnings("unchecked")
    public CourseCommand<Boolean> self() {
        synchronized (CourseCommand.class) {
            if (isNull(self.get())) {
                // getting command reference which can be used for transactional operations
                // actually it's proxy of the command with transactional executeDo method
                self.getAndSet(applicationContext.getBean("courseDelete", CourseCommand.class));
            }
        }
        return self.get();
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
     * @see this#rollbackCachedEntity(Context, Function)
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
            // change course-id value for further do command action
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

    // private methods
    private EntityNotFoundException exceptionFor(final Long id) {
        return new CourseNotFoundException(COURSE_WITH_ID_PREFIX + id + " is not exists.");
    }
}
