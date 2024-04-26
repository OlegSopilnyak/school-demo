package oleg.sopilnyak.test.service.command.executable.student;

import lombok.extern.slf4j.Slf4j;
import oleg.sopilnyak.test.school.common.exception.NotExistStudentException;
import oleg.sopilnyak.test.school.common.model.Student;
import oleg.sopilnyak.test.school.common.persistence.students.courses.StudentsPersistenceFacade;
import oleg.sopilnyak.test.school.common.persistence.utility.PersistenceFacadeUtilities;
import oleg.sopilnyak.test.service.command.executable.sys.CommandResult;
import oleg.sopilnyak.test.service.command.type.StudentCommand;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.command.type.base.command.SchoolCommandCache;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.LongFunction;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;


/**
 * Command-Implementation: command to update the student
 *
 * @see SchoolCommandCache
 * @see Student
 * @see StudentsPersistenceFacade
 */
@Slf4j
@Component
public class CreateOrUpdateStudentCommand
        extends SchoolCommandCache<Student>
        implements StudentCommand<Optional<Student>> {
    private final StudentsPersistenceFacade persistence;

    public CreateOrUpdateStudentCommand(StudentsPersistenceFacade persistence) {
        super(Student.class);
        this.persistence = persistence;
    }

    /**
     * To update student entity
     *
     * @param parameter student instance to update
     * @return execution's result
     * @deprecated commands are going to work through redo/undo
     */
    @Deprecated(forRemoval = true)
    @Override
    public CommandResult<Optional<Student>> execute(Object parameter) {
        try {
            log.debug("Trying to update student:{}", parameter);
            Student student = commandParameter(parameter);
            Optional<Student> resultStudent = persistence.save(student);
            log.debug("Got student {}", resultStudent);
            return CommandResult.<Optional<Student>>builder()
                    .result(Optional.ofNullable(resultStudent))
                    .success(true)
                    .build();
        } catch (Exception e) {
            log.error("Cannot update the student:{}", parameter, e);
            return CommandResult.<Optional<Student>>builder()
                    .result(Optional.of(Optional.empty()))
                    .exception(e).success(false).build();
        }
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
     * @see StudentsPersistenceFacade#toEntity(Student)
     * @see StudentsPersistenceFacade#save(Student)
     * @see NotExistStudentException
     */
    @Override
    public void executeDo(Context<?> context) {
        final Object parameter = context.getRedoParameter();
        try {
            check(parameter);
            log.debug("Trying to change student using: {}", parameter.toString());
            final Long inputId = ((Student) parameter).getId();
            final boolean isCreateStudent = PersistenceFacadeUtilities.isInvalidId(inputId);
            if (!isCreateStudent) {
                // cached student is storing to context for further rollback (undo)
                context.setUndoParameter(
                        retrieveEntity(inputId, persistence::findStudentById, persistence::toEntity,
                                () -> new NotExistStudentException(STUDENT_WITH_ID_PREFIX + inputId + " is not exists.")
                        )
                );
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
     * @see SchoolCommandCache#rollbackCachedEntity(Context, Function)
     * @see StudentsPersistenceFacade#save(Student)
     * @see StudentsPersistenceFacade#deleteStudent(Long)
     * @see NotExistStudentException
     */
    @Override
    public void executeUndo(Context<?> context) {
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
        return CREATE_OR_UPDATE_COMMAND_ID;
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
