package oleg.sopilnyak.test.service.command.executable.student;

import lombok.extern.slf4j.Slf4j;
import oleg.sopilnyak.test.school.common.exception.NotExistStudentException;
import oleg.sopilnyak.test.school.common.model.Student;
import oleg.sopilnyak.test.school.common.persistence.students.courses.StudentsPersistenceFacade;
import oleg.sopilnyak.test.school.common.persistence.utility.PersistenceFacadeUtilities;
import oleg.sopilnyak.test.service.command.executable.cache.SchoolCommandCache;
import oleg.sopilnyak.test.service.command.type.StudentCommand;
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
@Component
public class CreateOrUpdateStudentCommand
        extends SchoolCommandCache<Student>
        implements StudentCommand {
    private final StudentsPersistenceFacade persistence;
    private final BusinessMessagePayloadMapper payloadMapper;

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
     * @see StudentsPersistenceFacade#findStudentById(Long)
     * @see StudentsPersistenceFacade#save(Student)
     * @see NotExistStudentException
     */
    @Override
    public <T> void executeDo(Context<T> context) {
        final Object parameter = context.getRedoParameter();
        try {
            check(parameter);
            log.debug("Trying to change student using: {}", parameter.toString());
            final Long id = ((Student) parameter).getId();
            final boolean isCreateStudent = PersistenceFacadeUtilities.isInvalidId(id);
            if (!isCreateStudent) {
                // previous version of student is storing to context for further rollback (undo)
                final var entity = retrieveEntity(id, persistence::findStudentById, payloadMapper::toPayload,
                        () -> new NotExistStudentException(STUDENT_WITH_ID_PREFIX + id + " is not exists.")
                );
                context.setUndoParameter(entity);
            }
            final Optional<Student> student = persistRedoEntity(context, persistence::save);
            // checking execution context state
            afterPersistCheck(context, () -> rollbackCachedEntity(context, persistence::save), student, isCreateStudent);
        } catch (Exception e) {
            log.error("Cannot save the student '{}'", parameter, e);
            context.failed(e);
            rollbackCachedEntity(context, persistence::save);
        }
    }

    /**
     * To rollback update student entity<BR/>
     * To rollback command's execution with correct context state
     *
     * @param context context of redo execution
     * @see Context
     * @see Context#getUndoParameter()
     * @see SchoolCommandCache#rollbackCachedEntity(Context, Function, LongConsumer, Supplier)
     * @see StudentsPersistenceFacade#save(Student)
     * @see StudentsPersistenceFacade#deleteStudent(Long)
     * @see NotExistStudentException
     */
    @Override
    public <T> void executeUndo(Context<T> context) {
        final Object parameter = context.getUndoParameter();
        try {
            log.debug("Trying to undo student changes using: {}", parameter.toString());
            rollbackCachedEntity(context, persistence::save, persistence::deleteStudent,
                    () -> new NotExistStudentException("Wrong undo parameter :" + parameter));
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
