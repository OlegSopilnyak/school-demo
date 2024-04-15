package oleg.sopilnyak.test.service.command.executable.student;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import oleg.sopilnyak.test.school.common.facade.peristence.students.courses.StudentsPersistenceFacade;
import oleg.sopilnyak.test.school.common.model.Student;
import oleg.sopilnyak.test.service.command.executable.sys.CommandResult;
import oleg.sopilnyak.test.service.command.type.StudentCommand;
import org.slf4j.Logger;

import java.util.Optional;

/**
 * Command-Implementation: command to update the student
 */
@Slf4j
@AllArgsConstructor
public class CreateOrUpdateStudentCommand implements StudentCommand<Optional<Student>> {
    private final StudentsPersistenceFacade persistenceFacade;

    /**
     * To find enrolled students by course-id
     *
     * @param parameter system course-id
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
