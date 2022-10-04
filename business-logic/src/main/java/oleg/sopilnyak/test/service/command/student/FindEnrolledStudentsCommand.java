package oleg.sopilnyak.test.service.command.student;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import oleg.sopilnyak.test.school.common.facade.PersistenceFacade;
import oleg.sopilnyak.test.school.common.model.Student;
import oleg.sopilnyak.test.service.command.CommandResult;
import oleg.sopilnyak.test.service.command.SchoolCommand;

import java.util.Optional;
import java.util.Set;

/**
 * Command-Implementation: command to get enrolled students by course-id
 */
@Slf4j
@AllArgsConstructor
public class FindEnrolledStudentsCommand implements SchoolCommand<Set<Student>> {
    private final PersistenceFacade persistenceFacade;

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
            Long id = (Long) parameter;
            Set<Student> students = persistenceFacade.findEnrolledStudentsByCourseId(id);
            log.debug("Got students {} by ID:{}", students, id);
            return CommandResult.<Set<Student>>builder()
                    .result(Optional.ofNullable(students))
                    .success(true)
                    .build();
        } catch (Exception e) {
            log.error("Cannot find the student by ID:{}", parameter, e);
            return CommandResult.<Set<Student>>builder().result(Optional.empty()).exception(e).success(false).build();
        }
    }
}
