package oleg.sopilnyak.test.service.command.executable.student;

import lombok.extern.slf4j.Slf4j;
import oleg.sopilnyak.test.school.common.exception.EntityNotFoundException;
import oleg.sopilnyak.test.school.common.exception.education.StudentNotFoundException;
import oleg.sopilnyak.test.school.common.exception.education.StudentWithCoursesException;
import oleg.sopilnyak.test.school.common.model.Student;
import oleg.sopilnyak.test.school.common.persistence.education.StudentsPersistenceFacade;
import oleg.sopilnyak.test.school.common.persistence.utility.PersistenceFacadeUtilities;
import oleg.sopilnyak.test.service.command.executable.cache.SchoolCommandCache;
import oleg.sopilnyak.test.service.command.executable.sys.CommandContext;
import oleg.sopilnyak.test.service.command.io.Input;
import oleg.sopilnyak.test.service.command.type.education.StudentCommand;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.mapper.BusinessMessagePayloadMapper;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import java.util.function.*;

/**
 * Command-Implementation: command to delete the student
 *
 * @see SchoolCommandCache
 * @see Student
 * @see StudentsPersistenceFacade
 */
@Slf4j
@Component
public class DeleteStudentCommand extends SchoolCommandCache<Student> implements StudentCommand<Boolean> {
    private final transient StudentsPersistenceFacade persistence;
    private final transient BusinessMessagePayloadMapper payloadMapper;

    public DeleteStudentCommand(final StudentsPersistenceFacade persistence,
                                final BusinessMessagePayloadMapper payloadMapper) {
        super(Student.class);
        this.persistence = persistence;
        this.payloadMapper = payloadMapper;
    }

    /**
     * DO: To delete the student by id<BR/>
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
    public void executeDo(Context<Boolean> context) {
        final Input<Long> parameter = context.getRedoParameter();
        try {
            checkNullParameter(parameter);
            log.debug("Trying to delete student by ID: {}", parameter.toString());
            final Long id = parameter.value();
            if (PersistenceFacadeUtilities.isInvalidId(id)) {
                log.warn("Invalid id {}", id);
                throw exceptionFor(id);
            }

            // previous student is storing to context for further rollback (undo)
            final Student entity = retrieveEntity(
                    id, persistence::findStudentById, payloadMapper::toPayload, () -> exceptionFor(id)
            );
            if (!ObjectUtils.isEmpty(entity.getCourses())) {
                log.warn(STUDENT_WITH_ID_PREFIX + "{} has registered courses.", id);
                throw new StudentWithCoursesException(STUDENT_WITH_ID_PREFIX + id + " has registered courses.");
            }
            // removing student instance by ID from the database
            persistence.deleteStudent(id);
            // setup undo parameter for deleted entity
            prepareDeleteEntityUndo(context, entity, () -> exceptionFor(id));
            // successful delete entity operation
            context.setResult(true);
            log.debug("Deleted student with ID: {} successfully.", id);
        } catch (Exception e) {
            log.error("Cannot delete the student by Id: {}", parameter, e);
            context.failed(e);
        }
    }

    /**
     * UNDO: To delete the student by id<BR/>
     * To rollback command's execution with correct context state
     *
     * @param context context of redo execution
     * @see Context
     * @see Context#getUndoParameter()
     * @see SchoolCommandCache#rollbackCachedEntity(Context, Function, LongConsumer)
     */
    @Override
    public void executeUndo(Context<?> context) {
        final Input<Student> parameter = context.getUndoParameter();
        try {
            checkNullParameter(parameter);
            log.debug("Trying to undo student deletion using: {}", parameter.value());
            final var entity = rollbackCachedEntity(context, persistence::save).orElseThrow();

            log.debug("Updated in database: '{}'", entity);
            // change student-id value for further do command action
            if (context instanceof CommandContext<?> commandContext) {
                commandContext.setRedoParameter(Input.of(entity.getId()));
            }
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
     * To get mapper for business-message-payload
     *
     * @return mapper instance
     * @see BusinessMessagePayloadMapper
     */
    @Override
    public BusinessMessagePayloadMapper getPayloadMapper() {
        return payloadMapper;
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
        return new StudentNotFoundException(STUDENT_WITH_ID_PREFIX + id + " is not exists.");
    }
}
