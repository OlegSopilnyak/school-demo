package oleg.sopilnyak.test.service.command.executable.student;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import oleg.sopilnyak.test.school.common.facade.peristence.RegisterPersistenceFacade;
import oleg.sopilnyak.test.school.common.model.Student;
import oleg.sopilnyak.test.service.command.executable.CommandResult;
import oleg.sopilnyak.test.service.command.type.StudentCommand;

import java.util.Optional;
import java.util.Set;

import static oleg.sopilnyak.test.service.command.id.set.StudentCommands.FIND_ENROLLED;

/**
 * Command-Implementation: command to get enrolled students by course-id
 */
@Slf4j
@AllArgsConstructor
public class FindEnrolledStudentsCommand implements StudentCommand<Set<Student>> {
    private final RegisterPersistenceFacade persistenceFacade;

    /**
     * To find enrolled students by course-id
     *
     * @param parameter system course-id
     * @return execution's result
     */
    @Override
    public CommandResult<Set<Student>> execute(Object parameter) {
        try {
            log.debug("Trying to find enrolled students by the course ID:{}", parameter);
            Long id = commandParameter(parameter);
            Set<Student> students = persistenceFacade.findEnrolledStudentsByCourseId(id);
            log.debug("Got students {} by ID:{}", students, id);
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
     * To get unique command-id for the command
     *
     * @return value of command-id
     */
    @Override
    public String getId() {
        return FIND_ENROLLED.id();
    }
}
