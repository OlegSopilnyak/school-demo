package oleg.sopilnyak.test.service.command.student;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import oleg.sopilnyak.test.school.common.exception.StudentNotExistsException;
import oleg.sopilnyak.test.school.common.exception.StudentWithCoursesException;
import oleg.sopilnyak.test.school.common.facade.peristence.StudentsPersistenceFacade;
import oleg.sopilnyak.test.school.common.model.Student;
import oleg.sopilnyak.test.service.command.CommandResult;
import oleg.sopilnyak.test.service.command.SchoolCommand;
import oleg.sopilnyak.test.service.facade.student.StudentCommandFacade;
import org.springframework.util.ObjectUtils;

import java.util.Optional;

/**
 * Command-Implementation: command to delete the student
 */
@Slf4j
@AllArgsConstructor
public class DeleteStudentCommand implements SchoolCommand<Boolean> {
    private final StudentsPersistenceFacade persistenceFacade;

    /**
     * To find enrolled students by course-id
     *
     * @param parameter system course-id
     * @return execution's result
     */
    @Override
    public CommandResult<Boolean> execute(Object parameter) {
        try {
            log.debug("Trying to delete the student ID:{}", parameter);
            Long id = (Long) parameter;
            Optional<Student> student = persistenceFacade.findStudentById(id);
            if (student.isEmpty()) {
                return CommandResult.<Boolean>builder().result(Optional.empty())
                        .exception(new StudentNotExistsException("Student with ID:" + id + " is not exists."))
                        .success(false).build();
            }
            if (!ObjectUtils.isEmpty(student.get().getCourses())) {
                return CommandResult.<Boolean>builder().result(Optional.empty())
                        .exception(new StudentWithCoursesException("Student with ID:" + id + " has registered courses."))
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
            return CommandResult.<Boolean>builder().result(Optional.empty()).exception(e).success(false).build();
        }
    }

    /**
     * To get unique command-id for the command
     *
     * @return value of command-id
     */
    @Override
    public String getId() {
        return StudentCommandFacade.DELETE;
    }
}
