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
            log.debug("Trying to un-register student from course: {}", parameter);
            Long[] ids = (Long[]) parameter;
            Long studentId = ids[0];
            Long courseId = ids[1];
            Optional<Student> student = persistenceFacade.findStudentById(studentId);
            Optional<Course> course = persistenceFacade.findCourseById(courseId);
            if (student.isEmpty()) {
                log.debug("No such student with id:{}", studentId);
                return CommandResult.<Boolean>builder().result(Optional.empty())
                        .exception(new StudentNotExistsException("Student with ID:" + studentId + " is not exists."))
                        .success(false).build();
            }
            if (course.isEmpty()) {
                log.debug("No such course with id:{}", courseId);
                return CommandResult.<Boolean>builder().result(Optional.empty())
                        .exception(new CourseNotExistsException("Course with ID:" + courseId + " is not exists."))
                        .success(false).build();
            }

            log.debug("Un-linking student:{} from course {}", studentId, courseId);
            boolean success = persistenceFacade.unLink(student.get(), course.get());
            log.debug("Un-linked student:{} from course {} {}", studentId, courseId, success);

            return CommandResult.<Boolean>builder()
                    .result(Optional.of(success))
                    .success(true)
                    .build();
        } catch (Exception e) {
            log.error("Cannot link student to course {}", parameter, e);
            return CommandResult.<Boolean>builder().result(Optional.empty()).exception(e).success(false).build();
        }
    }
}
