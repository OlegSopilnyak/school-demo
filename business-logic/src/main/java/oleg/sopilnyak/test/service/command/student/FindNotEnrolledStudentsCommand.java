package oleg.sopilnyak.test.service.command.student;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import oleg.sopilnyak.test.school.common.facade.peristence.RegisterPersistenceFacade;
import oleg.sopilnyak.test.school.common.model.Student;
import oleg.sopilnyak.test.service.command.CommandResult;
import oleg.sopilnyak.test.service.command.SchoolCommand;
import oleg.sopilnyak.test.service.facade.student.StudentCommandFacade;

import java.util.Optional;
import java.util.Set;

/**
 * Command-Implementation: command to get not enrolled to any course students
 */
@Slf4j
@AllArgsConstructor
public class FindNotEnrolledStudentsCommand implements SchoolCommand<Set<Student>> {
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
            log.debug("Trying to find not enrolled students");
            Set<Student> students = persistenceFacade.findNotEnrolledStudents();
            log.debug("Got students {}", students);
            return CommandResult.<Set<Student>>builder()
                    .result(Optional.ofNullable(students))
                    .success(true)
                    .build();
        } catch (Exception e) {
            log.error("Cannot find the student by ID:{}", parameter, e);
            return CommandResult.<Set<Student>>builder().result(Optional.empty()).exception(e).success(false).build();
        }
    }

    /**
     * To get unique command-id for the command
     *
     * @return value of command-id
     */
    @Override
    public String getId() {
        return StudentCommandFacade.FIND_NOT_ENROLLED;
    }
}
