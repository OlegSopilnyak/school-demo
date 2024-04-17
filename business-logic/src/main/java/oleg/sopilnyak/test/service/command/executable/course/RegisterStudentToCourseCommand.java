package oleg.sopilnyak.test.service.command.executable.course;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import oleg.sopilnyak.test.school.common.exception.CourseNotExistsException;
import oleg.sopilnyak.test.school.common.exception.NoRoomInTheCourseException;
import oleg.sopilnyak.test.school.common.exception.StudentCoursesExceedException;
import oleg.sopilnyak.test.school.common.exception.StudentNotExistsException;
import oleg.sopilnyak.test.school.common.facade.peristence.StudentCourseLinkPersistenceFacade;
import oleg.sopilnyak.test.school.common.model.Course;
import oleg.sopilnyak.test.school.common.model.Student;
import oleg.sopilnyak.test.service.command.executable.sys.CommandResult;
import oleg.sopilnyak.test.service.command.type.CourseCommand;
import org.slf4j.Logger;

import java.util.Collection;
import java.util.Optional;

import static java.util.Objects.isNull;
import static org.springframework.util.ObjectUtils.isEmpty;


/**
 * Command-Implementation: command to link the student to the course
 */
@Slf4j
@AllArgsConstructor
public class RegisterStudentToCourseCommand implements CourseCommand<Boolean> {
    private final StudentCourseLinkPersistenceFacade persistenceFacade;
    @Getter
    private final int maximumRooms;
    @Getter
    private final int coursesExceed;

    /**
     * To link the student to the course
     *
     * @param parameter the array of [student-id, course-id]
     * @return execution's result
     * @deprecated commands are going to work through redo/undo
     */
    @Deprecated(forRemoval = true)
    @Override
    public CommandResult<Boolean> execute(Object parameter) {
        try {
            log.debug("Trying to register student to course: {}", parameter);
            final Long[] ids = commandParameter(parameter);
            final Long studentId = ids[0];
            final Long courseId = ids[1];
            final Optional<Student> student = persistenceFacade.findStudentById(studentId);
            if (student.isEmpty()) {
                log.debug("No such student with id:{}", studentId);
                return CommandResult.<Boolean>builder().success(false).result(Optional.of(false))
                        .exception(new StudentNotExistsException("Student with ID:" + studentId + " is not exists."))
                        .build();
            }
            final Optional<Course> course = persistenceFacade.findCourseById(courseId);
            if (course.isEmpty()) {
                log.debug("No such course with id:{}", courseId);
                return CommandResult.<Boolean>builder().success(false).result(Optional.of(false))
                        .exception(new CourseNotExistsException("Course with ID:" + courseId + " is not exists."))
                        .build();
            }
            if (isLinked(student.get(), course.get())) {
                log.debug("student: {} with course {} are already linked", studentId, courseId);
                return CommandResult.<Boolean>builder().success(true).result(Optional.of(true)).build();
            }
            if (course.get().getStudents().size() >= maximumRooms) {
                log.debug("Course with id:{} has students more than {}", courseId, maximumRooms);
                return CommandResult.<Boolean>builder().success(false).result(Optional.of(false))
                        .exception(new NoRoomInTheCourseException("Course with ID:" + courseId + " does not have enough rooms."))
                        .build();
            }
            if (student.get().getCourses().size() >= coursesExceed) {
                log.debug("Student with id:{} has more than {} courses", studentId, coursesExceed);
                return CommandResult.<Boolean>builder().success(false).result(Optional.of(false))
                        .exception(new StudentCoursesExceedException("Student with ID:" + studentId + " exceeds maximum courses."))
                        .build();
            }

            log.debug("Linking student:{} to course {}", studentId, courseId);
            final boolean linked = persistenceFacade.link(student.get(), course.get());
            log.debug("Linked student:{} to course {} {}", studentId, courseId, linked);

            return CommandResult.<Boolean>builder().success(true).result(Optional.of(linked)).build();
        } catch (Exception e) {
            log.error("Cannot link student to course {}", parameter, e);
            return CommandResult.<Boolean>builder().success(false).exception(e).result(Optional.of(false)).build();
        }
    }

    /**
     * To get unique command-id for the command
     *
     * @return value of command-id
     */
    @Override
    public String getId() {
        return REGISTER_COMMAND_ID;
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

    private static boolean isLinked(final Student student, final Course course) {
        return studentsHaveCourse(course.getStudents(), course.getId())
                &&
                coursesHaveStudent(student.getCourses(), student.getId())
                ;
    }

    private static boolean studentsHaveCourse(final Collection<Student> students, final Long courseId) {
        return !isEmpty(students) && isValid(courseId)
                && students.stream().anyMatch(student -> studentHasCourse(student, courseId));
    }

    private static boolean studentHasCourse(final Student student, final Long courseId) {
        return student.getCourses().stream().anyMatch(course -> courseId.equals(course.getId()));
    }

    private static boolean coursesHaveStudent(final Collection<Course> courses, final Long studentId) {
        return !isEmpty(courses) && isValid(studentId)
                && courses.stream().anyMatch(course -> courseHasStudent(course, studentId));
    }

    private static boolean courseHasStudent(final Course course, final Long studentId) {
        return course.getStudents().stream().anyMatch(student -> studentId.equals(student.getId()));
    }

    private static boolean isValid(final Long id) {
        return !isNull(id) && id > 0L;
    }
}
