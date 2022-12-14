package oleg.sopilnyak.test.service.command.student;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import oleg.sopilnyak.test.school.common.facade.PersistenceFacade;
import oleg.sopilnyak.test.school.common.model.Student;
import oleg.sopilnyak.test.service.command.CommandResult;
import oleg.sopilnyak.test.service.command.SchoolCommand;

import java.util.Optional;

/**
 * Command-Implementation: command to get student by id
 */
@Slf4j
@AllArgsConstructor
public class FindStudentCommand implements SchoolCommand<Optional<Student>> {
    private final PersistenceFacade persistenceFacade;

    /**
     * To find student by id
     *
     * @param parameter system student-id
     * @return execution's result
     */
    @Override
    public CommandResult<Optional<Student>> execute(Object parameter) {
        try {
            log.debug("Trying to find student by ID:{}", parameter);
            Long id = (Long) parameter;
            Optional<Student> student = persistenceFacade.findStudentById(id);
            log.debug("Got student {} by ID:{}", student, id);
            return CommandResult.<Optional<Student>>builder()
                    .result(Optional.ofNullable(student))
                    .success(true)
                    .build();
        } catch (Exception e) {
            log.error("Cannot find the student by ID:{}", parameter, e);
            return CommandResult.<Optional<Student>>builder().result(Optional.empty()).exception(e).success(false).build();
        }
    }
}
