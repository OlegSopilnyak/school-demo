package oleg.sopilnyak.test.service.command.executable.student;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import oleg.sopilnyak.test.school.common.exception.StudentNotExistsException;
import oleg.sopilnyak.test.school.common.facade.peristence.students.courses.StudentsPersistenceFacade;
import oleg.sopilnyak.test.school.common.model.Student;
import oleg.sopilnyak.test.service.command.executable.sys.CommandResult;
import oleg.sopilnyak.test.service.command.type.StudentCommand;
import oleg.sopilnyak.test.service.command.type.base.ChangeStudentCommand;
import oleg.sopilnyak.test.service.command.type.base.Context;
import org.slf4j.Logger;

import java.util.Optional;

import static oleg.sopilnyak.test.school.common.facade.peristence.students.courses.StudentsPersistenceFacade.isInvalidId;


/**
 * Command-Implementation: command to update the student
 */
@Slf4j
@AllArgsConstructor
public class CreateOrUpdateStudentCommand implements
        ChangeStudentCommand,
        StudentCommand<Optional<Student>> {
    @Getter
    private final StudentsPersistenceFacade persistenceFacade;

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
            Optional<Student> resultStudent = persistenceFacade.save(student);
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
     */
    @Override
    public void executeDo(Context<?> context) {
        final Object parameter = context.getRedoParameter();
        try {
            log.debug("Trying to change student using: {}", parameter.toString());
            final Long inputId = ((Student) parameter).getId();
            final boolean isCreateStudent = isInvalidId(inputId);
            if (!isCreateStudent) {
                context.setUndoParameter(cacheEntityForRollback(inputId));
            }
            final Optional<Student> student = persistRedoEntity(context);
            // checking execution context state
            if (context.isFailed()) {
                // there was a fail during save student
                log.error("Cannot save student {}", parameter);
                rollbackCachedEntity(context);
            } else {
                // save student operation is done successfully
                log.debug("Got saved \nstudent {}\n for input {}", student, parameter);
                context.setResult(student);

                if (student.isPresent() && isCreateStudent) {
                    // saving created student.id for undo operation
                    context.setUndoParameter(student.get().getId());
                }
            }
        } catch (Exception e) {
            log.error("Cannot save the student {}", parameter, e);
            context.failed(e);
            rollbackCachedEntity(context);
        }
    }

    /**
     * To rollback update student entity<BR/>
     * To rollback command's execution with correct context state
     *
     * @param context context of redo execution
     * @see Context
     * @see Context#getUndoParameter()
     */
    @Override
    public void executeUndo(Context<?> context) {
        final Object parameter = context.getUndoParameter();
        try {
            log.debug("Trying to undo student changes using: {}", parameter.toString());
            if (parameter instanceof Long id) {
                persistenceFacade.deleteStudent(id);
                log.debug("Got deleted \nstudent ID:{}\n success: {}", id, true);
            } else if (parameter instanceof Student student) {
                persistenceFacade.save(student);
                log.debug("Got restored \nstudent {}\n success: {}", student, true);
            } else {
                throw new StudentNotExistsException("Wrong undo parameter :" + parameter);
            }
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
