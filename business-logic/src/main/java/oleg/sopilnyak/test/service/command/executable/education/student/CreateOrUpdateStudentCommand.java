package oleg.sopilnyak.test.service.command.executable.education.student;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import oleg.sopilnyak.test.school.common.exception.education.StudentNotFoundException;
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

import java.util.Optional;
import java.util.function.*;


/**
 * Command-Implementation: command to create or update the student instance
 *
 * @see SchoolCommandCache
 * @see Student
 * @see StudentsPersistenceFacade
 */
@Slf4j
@Getter
@Component("studentUpdate")
public class CreateOrUpdateStudentCommand extends SchoolCommandCache<Student>
        implements StudentCommand<Optional<Student>> {
    private final transient StudentsPersistenceFacade persistence;
    private final transient BusinessMessagePayloadMapper payloadMapper;

    public CreateOrUpdateStudentCommand(final StudentsPersistenceFacade persistence,
                                        final BusinessMessagePayloadMapper payloadMapper) {
        super(Student.class);
        this.persistence = persistence;
        this.payloadMapper = payloadMapper;
    }

    /**
     * To update student entity<BR/>
     * To execute command redo with correct context state
     *
     * @param context context of redo execution
     * @see Context
     * @see Context.State#WORK
     * @see SchoolCommandCache#retrieveEntity(Long, LongFunction, UnaryOperator, Supplier)
     * @see SchoolCommandCache#persistRedoEntity(Context, Function)
     * @see SchoolCommandCache#rollbackCachedEntity(Context, Function)
     * @see SchoolCommandCache#restoreInitialCommandState(Context, Function)
     * @see StudentsPersistenceFacade#findStudentById(Long)
     * @see StudentsPersistenceFacade#save(Student)
     * @see StudentNotFoundException
     */
    @Override
    public void executeDo(Context<Optional<Student>> context) {
        final Input<Student> parameter = context.getRedoParameter();
        try {
            checkNullParameter(parameter);
            log.debug("Trying to change student using: {}", parameter);
            final Long id = parameter.value().getId();
            final boolean isCreateEntityMode = PersistenceFacadeUtilities.isInvalidId(id);
            if (!isCreateEntityMode) {
                // previous version of student is getting and storing to context for further rollback (undo)
                final Student entity = retrieveEntity(
                        id, persistence::findStudentById, payloadMapper::toPayload,
                        () -> new StudentNotFoundException(STUDENT_WITH_ID_PREFIX + id + " is not exists.")
                );
                if (context instanceof CommandContext<?> commandContext) {
                    log.debug("Previous value of the entity stored for possible command's undo: {}", entity);
                    commandContext.setUndoParameter(Input.of(entity));
                }
            } else {
                log.debug("Trying to create student using: {}", parameter);
            }
            // persisting entity trough persistence layer
            final Optional<Student> persisted = persistRedoEntity(context, persistence::save);
            // checking command context state after entity persistence
            afterEntityPersistenceCheck(
                    context, () -> rollbackCachedEntity(context, persistence::save),
                    persisted.orElse(null), isCreateEntityMode
            );
        } catch (Exception e) {
            log.error("Cannot save the student '{}'", parameter, e);
            context.failed(e);
            restoreInitialCommandState(context, persistence::save);
        }
    }

    /**
     * To rollback update student entity<BR/>
     * To rollback command's execution with correct context state
     *
     * @param context context of redo execution
     * @see Context
     * @see Context#getUndoParameter()
     * @see SchoolCommandCache#rollbackCachedEntity(Context, Function, LongConsumer)
     * @see StudentsPersistenceFacade#save(Student)
     * @see StudentsPersistenceFacade#deleteStudent(Long)
     */
    @Override
    public void executeUndo(Context<?> context) {
        final Input<?> parameter = context.getUndoParameter();
        try {
            checkNullParameter(parameter);
            log.debug("Trying to undo student changes using: {}", parameter);

            rollbackCachedEntity(context, persistence::save, persistence::deleteStudent);

            context.setState(Context.State.UNDONE);
        } catch (Exception e) {
            log.error("Cannot undo student change {}", parameter, e);
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
