package oleg.sopilnyak.test.service.command.executable.course;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import oleg.sopilnyak.test.school.common.exception.CourseNotExistsException;
import oleg.sopilnyak.test.school.common.exception.StudentNotExistsException;
import oleg.sopilnyak.test.school.common.facade.peristence.StudentCourseLinkPersistenceFacade;
import oleg.sopilnyak.test.school.common.model.Course;
import oleg.sopilnyak.test.school.common.model.Student;
import oleg.sopilnyak.test.service.command.executable.sys.CommandResult;
import oleg.sopilnyak.test.service.command.type.CourseCommand;
import org.slf4j.Logger;

import java.util.Optional;

/**
 * Command-Implementation: command to un-link the student from the course
 */
@Slf4j
@AllArgsConstructor
public class UnRegisterStudentFromCourseCommand implements CourseCommand<Boolean> {
    private final StudentCourseLinkPersistenceFacade persistenceFacade;


    /**
     * To unlink the student from the course
     *
     * @param parameter the array of [student-id, course-id]
     * @return execution's result
     * @deprecated commands are going to work through redo/undo
     */
    @Deprecated(forRemoval = true)
    @Override
    public CommandResult<Boolean> execute(Object parameter) {
        try {
            log.debug("Trying to un-link student from course: {}", parameter);
            final Long[] ids = commandParameter(parameter);
            final Long studentId = ids[0];
            final Long courseId = ids[1];
            final Optional<Student> student = persistenceFacade.findStudentById(studentId);
            if (student.isEmpty()) {
                log.debug("No such student with id:{}", studentId);
                return CommandResult.<Boolean>builder().success(false)
                        .exception(new StudentNotExistsException("Student with ID:" + studentId + " is not exists."))
                        .result(Optional.of(false)).build();
            }
            final Optional<Course> course = persistenceFacade.findCourseById(courseId);
            if (course.isEmpty()) {
                log.debug("No such course with id:{}", courseId);
                return CommandResult.<Boolean>builder().success(false)
                        .exception(new CourseNotExistsException("Course with ID:" + courseId + " is not exists."))
                        .result(Optional.of(false)).build();
            }

            log.debug("Un-linking student-id:{} from course-id:{}", studentId, courseId);
            final boolean unLinked = persistenceFacade.unLink(student.get(), course.get());
            log.debug("Un-linked student:{} from course-id:{} {}", studentId, courseId, unLinked);

            return CommandResult.<Boolean>builder().success(true).result(Optional.of(unLinked)).build();
        } catch (Exception e) {
            log.error("Cannot link student to course {}", parameter, e);
            return CommandResult.<Boolean>builder().success(false).result(Optional.of(false)).exception(e).build();
        }
    }

    /**
     * To get unique command-id for the command
     *
     * @return value of command-id
     */
    @Override
    public String getId() {
        return UN_REGISTER_COMMAND_ID;
    }

    /**
     * To get reference to command's logger
     *
     * @return reference to the logger
     */
    @Override
    public Logger getLog() {
        return log;
    }
}
