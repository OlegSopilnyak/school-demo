package oleg.sopilnyak.test.service.command.student;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import oleg.sopilnyak.test.school.common.facade.peristence.StudentsPersistenceFacade;
import oleg.sopilnyak.test.school.common.model.Student;
import oleg.sopilnyak.test.service.command.CommandResult;
import oleg.sopilnyak.test.service.command.SchoolCommand;

import java.util.Optional;

/**
 * Command-Implementation: command to update the student
 */
@Slf4j
@AllArgsConstructor
public class CreateOrUpdateStudentCommand implements SchoolCommand<Optional<Student>> {
    private final StudentsPersistenceFacade persistenceFacade;

    /**
     * To find enrolled students by course-id
     *
     * @param parameter system course-id
     * @return execution's result
     */
    @Override
    public CommandResult<Optional<Student>> execute(Object parameter) {
        try {
            log.debug("Trying to find enrolled students by the course ID:{}", parameter);
            Student student = (Student) parameter;
            Optional<Student> resultStudent = persistenceFacade.save(student);
            log.debug("Got student {}", resultStudent);
            return CommandResult.<Optional<Student>>builder()
                    .result(Optional.ofNullable(resultStudent))
                    .success(true)
                    .build();
        } catch (Exception e) {
            log.error("Cannot find the student by ID:{}", parameter, e);
            return CommandResult.<Optional<Student>>builder().result(Optional.empty()).exception(e).success(false).build();
        }
    }
}
