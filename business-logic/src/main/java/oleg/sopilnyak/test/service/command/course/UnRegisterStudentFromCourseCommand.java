package oleg.sopilnyak.test.service.command.course;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import oleg.sopilnyak.test.school.common.exception.CourseNotExistsException;
import oleg.sopilnyak.test.school.common.exception.StudentNotExistsException;
import oleg.sopilnyak.test.school.common.facade.PersistenceFacade;
import oleg.sopilnyak.test.school.common.model.Course;
import oleg.sopilnyak.test.school.common.model.Student;
import oleg.sopilnyak.test.service.command.CommandResult;
import oleg.sopilnyak.test.service.command.SchoolCommand;
import oleg.sopilnyak.test.service.facade.course.CourseCommandsFacade;

import java.util.Optional;

/**
 * Command-Implementation: command to un-link the student from the course
 */
@Slf4j
@AllArgsConstructor
public class UnRegisterStudentFromCourseCommand implements SchoolCommand<Boolean> {
    private final PersistenceFacade persistenceFacade;

    /**
     * To find student by id
     *
     * @param parameter system student-id
     * @return execution's result
     */
    @Override
    public CommandResult<Boolean> execute(Object parameter) {
        try {
            log.debug("Trying to un-link student from course: {}", parameter);
            Long[] ids = (Long[]) parameter;
            Long studentId = ids[0];
            Long courseId = ids[1];
            final Optional<Student> student = persistenceFacade.findStudentById(studentId);
            if (student.isEmpty()) {
                log.debug("No such student with id:{}", studentId);
                return CommandResult.<Boolean>builder()
                        .result(Optional.of(false))
                        .exception(new StudentNotExistsException("Student with ID:" + studentId + " is not exists."))
                        .success(false).build();
            }
            final Optional<Course> course = persistenceFacade.findCourseById(courseId);
            if (course.isEmpty()) {
                log.debug("No such course with id:{}", courseId);
                return CommandResult.<Boolean>builder()
                        .result(Optional.of(false))
                        .exception(new CourseNotExistsException("Course with ID:" + courseId + " is not exists."))
                        .success(false).build();
            }

            log.debug("Un-linking student-id:{} from course-id:{}", studentId, courseId);
            boolean unLinked = persistenceFacade.unLink(student.get(), course.get());
            log.debug("Un-linked student:{} from course-id:{} {}", studentId, courseId, unLinked);

            return CommandResult.<Boolean>builder().result(Optional.of(unLinked)).success(true).build();
        } catch (Exception e) {
            log.error("Cannot link student to course {}", parameter, e);
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
        return CourseCommandsFacade.UN_REGISTER;
    }
}
