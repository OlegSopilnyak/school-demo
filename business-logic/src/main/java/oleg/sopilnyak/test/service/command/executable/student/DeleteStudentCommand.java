package oleg.sopilnyak.test.service.command.executable.student;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import oleg.sopilnyak.test.school.common.exception.StudentNotExistsException;
import oleg.sopilnyak.test.school.common.exception.StudentWithCoursesException;
import oleg.sopilnyak.test.school.common.facade.peristence.students.courses.StudentsPersistenceFacade;
import oleg.sopilnyak.test.school.common.model.Student;
import oleg.sopilnyak.test.service.command.executable.sys.CommandResult;
import oleg.sopilnyak.test.service.command.type.StudentCommand;
import oleg.sopilnyak.test.service.command.type.base.ChangeStudentCommand;
import oleg.sopilnyak.test.service.command.type.base.Context;
import org.slf4j.Logger;
import org.springframework.util.ObjectUtils;

import java.util.Optional;

import static oleg.sopilnyak.test.school.common.facade.peristence.students.courses.StudentsPersistenceFacade.isInvalidId;

/**
 * Command-Implementation: command to delete the student
 */
@Slf4j
@AllArgsConstructor
public class DeleteStudentCommand implements
        ChangeStudentCommand,
        StudentCommand<Boolean> {
    public static final String STUDENT_WITH_ID_PREFIX = "Student with ID:";
    @Getter
    private final StudentsPersistenceFacade persistenceFacade;

    /**
     * To delete the student by student-id
     *
     * @param parameter system course-id
     * @return execution's result
     * @deprecated commands are going to work through redo/undo
     */
    @Deprecated(forRemoval = true)
    @Override
    public CommandResult<Boolean> execute(Object parameter) {
        try {
            log.debug("Trying to delete the student ID:{}", parameter);
            Long id = commandParameter(parameter);
            Optional<Student> student = persistenceFacade.findStudentById(id);
            if (student.isEmpty()) {
                return CommandResult.<Boolean>builder()
                        .result(Optional.of(false))
                        .exception(new StudentNotExistsException(STUDENT_WITH_ID_PREFIX + id + " is not exists."))
                        .success(false).build();
            }
            if (!ObjectUtils.isEmpty(student.get().getCourses())) {
                return CommandResult.<Boolean>builder()
                        .result(Optional.of(false))
                        .exception(new StudentWithCoursesException(STUDENT_WITH_ID_PREFIX + id + " has registered courses."))
                        .success(false).build();
            }
            boolean result = persistenceFacade.deleteStudent(id);
            log.debug("Deleted student: {} success is '{}'", id, result);
            return CommandResult.<Boolean>builder()
                    .result(Optional.of(result))
                    .success(true)
                    .build();
        } catch (Exception e) {
            log.error("Cannot delete the student by ID:{}", parameter, e);
            return CommandResult.<Boolean>builder()
                    .result(Optional.of(false))
                    .exception(e).success(false).build();
        }
    }

    /**
     * DO: To delete the student by student-id<BR/>
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
            log.debug("Trying to delete student by ID: {}", parameter.toString());
            final Long inputId = commandParameter(parameter);
            if (isInvalidId(inputId)) {
                throw new StudentNotExistsException(STUDENT_WITH_ID_PREFIX + inputId + " is not exists.");
            }
            final Student student = (Student) cacheEntityForRollback(inputId);
            if (!ObjectUtils.isEmpty(student.getCourses())) {
                throw new StudentWithCoursesException(STUDENT_WITH_ID_PREFIX + inputId + " has registered courses.");
            }
            // cached student saved to context for further undo
            context.setUndoParameter(student);
            persistenceFacade.deleteStudent(inputId);
            context.setResult(true);
        } catch (Exception e) {
            log.error("Cannot save the student {}", parameter, e);
            context.failed(e);
            rollbackCachedEntity(context);
        }
    }

    /**
     * To rollback delete the student by student-id<BR/>
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
            log.debug("Trying to undo student deletion using: {}", parameter.toString());
            if (parameter instanceof Student student) {
                final Optional<Student> restored = persistenceFacade.save(student);
                log.debug("Got restored student {}", restored.orElse(null));
            } else {
                throw new NullPointerException("Wrong undo parameter :" + parameter);
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
        return DELETE_COMMAND_ID;
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
