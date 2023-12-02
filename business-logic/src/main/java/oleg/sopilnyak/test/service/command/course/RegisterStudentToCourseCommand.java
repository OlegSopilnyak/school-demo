package oleg.sopilnyak.test.service.command.course;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import oleg.sopilnyak.test.school.common.exception.CourseNotExistsException;
import oleg.sopilnyak.test.school.common.exception.NoRoomInTheCourseException;
import oleg.sopilnyak.test.school.common.exception.StudentCoursesExceedException;
import oleg.sopilnyak.test.school.common.exception.StudentNotExistsException;
import oleg.sopilnyak.test.school.common.facade.PersistenceFacade;
import oleg.sopilnyak.test.school.common.model.Course;
import oleg.sopilnyak.test.school.common.model.Student;
import oleg.sopilnyak.test.service.command.CommandResult;
import oleg.sopilnyak.test.service.command.SchoolCommand;
import oleg.sopilnyak.test.service.facade.course.CourseCommandsFacade;

import java.util.Collection;
import java.util.Optional;

import static java.util.Objects.isNull;
import static org.springframework.util.ObjectUtils.isEmpty;


/**
 * Command-Implementation: command to link the student to the course
 */
@SuppressWarnings("DuplicatedCode")
@Slf4j
@AllArgsConstructor
public class RegisterStudentToCourseCommand implements SchoolCommand<Boolean> {
    private final PersistenceFacade persistenceFacade;
    private final int maximumRooms;
    private final int coursesExceed;

    /**
     * To find student by id
     *
     * @param parameter system student-id
     * @return execution's result
     */
    @Override
    public CommandResult<Boolean> execute(Object parameter) {
        try {
            log.debug("Trying to register student to course: {}", parameter);
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
            if (isLinked(student.get(), course.get())) {
                log.debug("student: {} with course {} are already linked", studentId, courseId);
                return CommandResult.<Boolean>builder()
                        .result(Optional.of(true))
                        .success(true).build();
            }
            if (course.get().getStudents().size() >= maximumRooms) {
                log.debug("Course with id:{} has students more than {}", courseId, maximumRooms);
                return CommandResult.<Boolean>builder()
                        .result(Optional.of(false))
                        .exception(new NoRoomInTheCourseException("Course with ID:" + courseId + " does not have enough rooms."))
                        .success(false).build();
            }
            if (student.get().getCourses().size() >= coursesExceed) {
                log.debug("Student with id:{} has more than {} courses", studentId, coursesExceed);
                return CommandResult.<Boolean>builder()
                        .result(Optional.of(false))
                        .exception(new StudentCoursesExceedException("Student with ID:" + studentId + " exceeds maximum courses."))
                        .success(false).build();
            }

            log.debug("Linking student:{} to course {}", studentId, courseId);
            boolean linked = persistenceFacade.link(student.get(), course.get());
            log.debug("Linked student:{} to course {} {}", studentId, courseId, linked);

            return CommandResult.<Boolean>builder().result(Optional.of(linked)).success(true).build();
        } catch (Exception e) {
            log.error("Cannot link student to course {}", parameter, e);
            return CommandResult.<Boolean>builder()
                    .result(Optional.of(false)).exception(e).success(false).build();
        }
    }

    /**
     * To get unique command-id for the command
     *
     * @return value of command-id
     */
    @Override
    public String getId() {
        return CourseCommandsFacade.REGISTER;
    }

    private static boolean isLinked(Student studentInstance, Course courseInstance) {
        final Long studentId = studentInstance.getId(), courseId = courseInstance.getId();
        return
                studentsHaveCourse(courseInstance.getStudents(), courseId) &&
                        coursesHaveStudent(studentInstance.getCourses(), studentId)
                ;
    }

    private static boolean studentsHaveCourse(final Collection<Student> students, final Long courseId) {
        if (isEmpty(students) || isNull(courseId) || courseId <= 0) {
            return false;
        }
        for (final Student student : students) {
            if (studentHasCourse(student, courseId)) {
                return true;
            }
        }
        return false;
    }

    private static boolean studentHasCourse(final Student student, final Long courseId) {
        for (final Course course : student.getCourses()) {
            if (courseId.equals(course.getId())) {
                return true;
            }
        }
        return false;
    }

    private static boolean coursesHaveStudent(final Collection<Course> courses, final Long studentId) {
        if (isEmpty(courses) || isNull(studentId) || studentId <= 0) {
            return false;
        }
        for (final Course course : courses) {
            if (courseHasStudent(course, studentId)) {
                return true;
            }
        }
        return false;
    }

    private static boolean courseHasStudent(final Course course, final Long studentId) {
        for (final Student student : course.getStudents()) {
            if (studentId.equals(student.getId())) {
                return true;
            }
        }
        return false;
    }
}
