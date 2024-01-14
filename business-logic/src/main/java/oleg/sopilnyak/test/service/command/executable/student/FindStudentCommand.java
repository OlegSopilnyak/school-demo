package oleg.sopilnyak.test.service.command.executable.student;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import oleg.sopilnyak.test.school.common.facade.peristence.StudentsPersistenceFacade;
import oleg.sopilnyak.test.school.common.model.Student;
import oleg.sopilnyak.test.service.command.executable.CommandResult;
import oleg.sopilnyak.test.service.command.type.StudentCommand;
import oleg.sopilnyak.test.service.facade.student.StudentCommandFacade;

import java.util.Optional;

/**
 * Command-Implementation: command to get student by id
 */
@Slf4j
@AllArgsConstructor
public class FindStudentCommand implements StudentCommand<Optional<Student>> {
    private final StudentsPersistenceFacade persistenceFacade;

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
            return CommandResult.<Optional<Student>>builder()
                    .result(Optional.of(Optional.empty())).exception(e).success(false).build();
        }
    }

    /**
     * To get unique command-id for the command
     *
     * @return value of command-id
     */
    @Override
    public final String getId() {
        return StudentCommandFacade.FIND_BY_ID;
    }
}
