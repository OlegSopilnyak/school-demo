package oleg.sopilnyak.test.service.command.executable.student;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import oleg.sopilnyak.test.school.common.facade.peristence.students.courses.RegisterPersistenceFacade;
import oleg.sopilnyak.test.school.common.facade.peristence.students.courses.StudentsPersistenceFacade;
import oleg.sopilnyak.test.school.common.model.Student;
import oleg.sopilnyak.test.service.command.executable.sys.CommandResult;
import oleg.sopilnyak.test.service.command.type.StudentCommand;
import oleg.sopilnyak.test.service.command.type.base.Context;
import org.slf4j.Logger;

import java.util.Optional;
import java.util.Set;

/**
 * Command-Implementation: command to get not enrolled to any course students
 */
@Slf4j
@AllArgsConstructor
public class FindNotEnrolledStudentsCommand implements StudentCommand<Set<Student>> {
    private final RegisterPersistenceFacade persistenceFacade;

    /**
     * To find not enrolled students
     *
     * @param parameter not used
     * @return execution's result
     * @deprecated commands are going to work through redo/undo
     */
    @Deprecated(forRemoval = true)
    @Override
    public CommandResult<Set<Student>> execute(Object parameter) {
        try {
            log.debug("Trying to find not enrolled students");
            Set<Student> students = persistenceFacade.findNotEnrolledStudents();
            log.debug("Got students {}", students);
            return CommandResult.<Set<Student>>builder()
                    .result(Optional.ofNullable(students))
                    .success(true)
                    .build();
        } catch (Exception e) {
            log.error("Cannot find the student by ID:{}", parameter, e);
            return CommandResult.<Set<Student>>builder()
                    .result(Optional.of(Set.of()))
                    .exception(e).success(false).build();
        }
    }

    /**
     * To execute command redo with correct context state
     *
     * @param context context of redo execution
     * @see Context
     * @see Context#setResult(Object)
     * @see Context.State#WORK
     * @see StudentsPersistenceFacade#findStudentById(Long)
     */
    @Override
    public void executeDo(Context<?> context) {
        log.debug("Trying to find not enrolled students");
        try {

            Set<Student> students = persistenceFacade.findNotEnrolledStudents();
            log.debug("Got students {}", students);

            context.setResult(students);
        } catch (Exception e) {
            log.error("Cannot find not enrolled students", e);
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
        return FIND_NOT_ENROLLED_COMMAND_ID;
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
