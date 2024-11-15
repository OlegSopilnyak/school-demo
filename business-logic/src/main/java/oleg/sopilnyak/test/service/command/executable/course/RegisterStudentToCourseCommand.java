package oleg.sopilnyak.test.service.command.executable.course;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import oleg.sopilnyak.test.school.common.exception.education.CourseHasNoRoomException;
import oleg.sopilnyak.test.school.common.exception.education.CourseNotFoundException;
import oleg.sopilnyak.test.school.common.exception.education.StudentCoursesExceedException;
import oleg.sopilnyak.test.school.common.exception.education.StudentNotFoundException;
import oleg.sopilnyak.test.school.common.model.Course;
import oleg.sopilnyak.test.school.common.model.Student;
import oleg.sopilnyak.test.school.common.persistence.education.joint.EducationPersistenceFacade;
import oleg.sopilnyak.test.service.command.type.CourseCommand;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.mapper.BusinessMessagePayloadMapper;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Objects;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.springframework.util.ObjectUtils.isEmpty;


/**
 * Command-Implementation: command to link the student to the course
 */
@Slf4j
@Getter
@Component
public class RegisterStudentToCourseCommand implements CourseCommand<Boolean>, EducationLinkCommand {
    public static final String STUDENT_WITH_ID_PREFIX = "Student with ID:";
    public static final String COURSE_WITH_ID_PREFIX = "Course with ID:";
    public static final String IS_NOT_EXISTS_SUFFIX = " is not exists.";
    @Getter(AccessLevel.NONE)
    private final EducationPersistenceFacade persistenceFacade;
    private final BusinessMessagePayloadMapper payloadMapper;
    private final int maximumRooms;
    private final int coursesExceed;

    public RegisterStudentToCourseCommand(final EducationPersistenceFacade persistenceFacade,
                                          final BusinessMessagePayloadMapper payloadMapper,
                                          @Value("${school.courses.maximum.rooms:50}") int maximumRooms,
                                          @Value("${school.students.maximum.courses:5}") int coursesExceed) {
        this.persistenceFacade = persistenceFacade;
        this.payloadMapper = payloadMapper;
        this.maximumRooms = maximumRooms;
        this.coursesExceed = coursesExceed;
    }

    /**
     * To link the student to the course<BR/>
     * To execute command redo with correct context state
     *
     * @param context context of redo execution
     * @see EducationLinkCommand#detached(Course)
     * @see EducationLinkCommand#detached(Student)
     * @see EducationPersistenceFacade#findStudentById(Long)
     * @see EducationPersistenceFacade#findCourseById(Long)
     * @see EducationPersistenceFacade#link(Student, Course)
     * @see Context
     * @see Context#setUndoParameter(Object)
     * @see Context#setResult(Object)
     * @see Context.State#WORK
     */
    @Override
    public void executeDo(Context<Boolean> context) {
        final Object parameter = context.getRedoParameter();
        try {
            checkNullParameter(parameter);
            log.debug("Trying to register student to course: {}", parameter);
            final Long[] ids = commandParameter(parameter);
            final Long studentId = ids[0];
            final Long courseId = ids[1];
            final Student student = persistenceFacade.findStudentById(studentId)
                    .orElseThrow(() -> new StudentNotFoundException(STUDENT_WITH_ID_PREFIX + studentId + IS_NOT_EXISTS_SUFFIX));
            final Course course = persistenceFacade.findCourseById(courseId)
                    .orElseThrow(() -> new CourseNotFoundException(COURSE_WITH_ID_PREFIX + courseId + IS_NOT_EXISTS_SUFFIX));

            if (isLinked(student, course)) {
                log.debug("student: {} with course {} are already linked", studentId, courseId);
                context.setResult(true);
            } else if (course.getStudents().size() >= maximumRooms) {
                log.error("Course with id:{} has students more than {}", courseId, maximumRooms);
                throw new CourseHasNoRoomException(COURSE_WITH_ID_PREFIX + courseId + " does not have enough rooms.");
            } else if (student.getCourses().size() >= coursesExceed) {
                log.error("Student with id:{} has more than {} courses", studentId, coursesExceed);
                throw new StudentCoursesExceedException(STUDENT_WITH_ID_PREFIX + studentId + " exceeds maximum courses.");
            } else {
                log.debug("Linking student:{} to course:{}", studentId, courseId);
                final var undoLink = new StudentToCourseLink(detached(student), detached(course));

                final boolean successful = persistenceFacade.link(student, course);

                if (successful) {
                    context.setUndoParameter(undoLink);
                }
                context.setResult(successful);
                log.debug("Linked student:{} to course {} successful: {}", studentId, courseId, successful);
            }
        } catch (Exception e) {
            log.error("Cannot link student to course {}", parameter, e);
            context.failed(e);
        }
    }

    /**
     * To link the student to the course<BR/>
     * To rollback command's execution with correct context state
     *
     * @param context context of redo execution
     * @see EducationPersistenceFacade#unLink(Student, Course)
     * @see Context
     * @see Context#getUndoParameter()
     */
    @Override
    public void executeUndo(Context<?> context) {
        final Object parameter = context.getUndoParameter();
        if (isNull(parameter)) {
            log.debug("Undo parameter is null");
            context.setState(Context.State.UNDONE);
            return;
        }
        log.debug("Trying to undo student to course linking using: {}", parameter);
        try {
            final StudentToCourseLink undoLink = commandParameter(parameter);

            final boolean successful = persistenceFacade.unLink(undoLink.getStudent(), undoLink.getCourse());

            context.setState(Context.State.UNDONE);
            log.debug("Undone student to course linking {}", successful);
        } catch (Exception e) {
            log.error("Cannot undo student to course linking for {}", parameter, e);
            context.failed(e);
        }
    }

    /**
     * To get unique command-id for the command
     *
     * @return value of command-id
     */
    @Override
    public String getId() {
        return REGISTER;
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

    // private methods
    private static boolean isLinked(final Student student, final Course course) {
        return studentsHaveCourse(course.getStudents(), course.getId()) &&
                coursesHaveStudent(student.getCourses(), student.getId());
    }

    private static boolean studentsHaveCourse(final Collection<Student> students, final Long courseId) {
        return !isEmpty(students) && isValid(courseId)
                && students.stream().anyMatch(student -> studentHasCourse(student, courseId));
    }

    private static boolean studentHasCourse(final Student student, final Long courseId) {
        return nonNull(student) && !isEmpty(student.getCourses())
                && student.getCourses().stream().anyMatch(course -> Objects.equals(courseId, course.getId()));
    }

    private static boolean coursesHaveStudent(final Collection<Course> courses, final Long studentId) {
        return !isEmpty(courses) && isValid(studentId)
                && courses.stream().anyMatch(course -> courseHasStudent(course, studentId));
    }

    private static boolean courseHasStudent(final Course course, final Long studentId) {
        return nonNull(course) && !isEmpty(course.getStudents())
                && course.getStudents().stream().anyMatch(student -> Objects.equals(studentId, student.getId()));
    }

    private static boolean isValid(final Long id) {
        return nonNull(id) && id > 0L;
    }
}
