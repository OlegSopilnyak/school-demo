package oleg.sopilnyak.test.service.command.executable.student;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import oleg.sopilnyak.test.school.common.exception.StudentNotExistsException;
import oleg.sopilnyak.test.school.common.exception.StudentWithCoursesException;
import oleg.sopilnyak.test.school.common.facade.peristence.students.courses.StudentsPersistenceFacade;
import oleg.sopilnyak.test.school.common.model.Student;
import oleg.sopilnyak.test.service.command.executable.sys.CommandResult;
import oleg.sopilnyak.test.service.command.type.StudentCommand;
import org.springframework.util.ObjectUtils;

import java.util.Optional;

import static oleg.sopilnyak.test.service.command.id.set.StudentCommands.DELETE;

/**
 * Command-Implementation: command to delete the student
 */
@Slf4j
@AllArgsConstructor
public class DeleteStudentCommand implements StudentCommand<Boolean> {
    private final StudentsPersistenceFacade persistenceFacade;

    /**
     * To delete the student by student-id
     *
     * @param parameter system course-id
     * @return execution's result
     */
    @Override
    public CommandResult<Boolean> execute(Object parameter) {
        try {
            log.debug("Trying to delete the student ID:{}", parameter);
            Long id = commandParameter(parameter);
            Optional<Student> student = persistenceFacade.findStudentById(id);
            if (student.isEmpty()) {
                return CommandResult.<Boolean>builder()
                        .result(Optional.of(false))
                        .exception(new StudentNotExistsException("Student with ID:" + id + " is not exists."))
                        .success(false).build();
            }
            if (!ObjectUtils.isEmpty(student.get().getCourses())) {
                return CommandResult.<Boolean>builder()
                        .result(Optional.of(false))
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
            return CommandResult.<Boolean>builder()
                    .result(Optional.of(false))
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
        return DELETE.id();
    }
}
