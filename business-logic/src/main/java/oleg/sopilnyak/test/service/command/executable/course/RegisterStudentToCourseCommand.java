package oleg.sopilnyak.test.service.command.executable.course;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import oleg.sopilnyak.test.school.common.exception.NoRoomInTheCourseException;
import oleg.sopilnyak.test.school.common.exception.NotExistCourseException;
import oleg.sopilnyak.test.school.common.exception.NotExistStudentException;
import oleg.sopilnyak.test.school.common.exception.StudentCoursesExceedException;
import oleg.sopilnyak.test.school.common.model.Course;
import oleg.sopilnyak.test.school.common.model.Student;
import oleg.sopilnyak.test.school.common.persistence.StudentCourseLinkPersistenceFacade;
import oleg.sopilnyak.test.service.command.type.CourseCommand;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.mapper.BusinessMessagePayloadMapper;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Optional;

import static java.util.Objects.isNull;
import static org.springframework.util.ObjectUtils.isEmpty;


/**
 * Command-Implementation: command to link the student to the course
 */
@Slf4j
@Getter
@Component
public class RegisterStudentToCourseCommand implements CourseCommand {
    public static final String STUDENT_WITH_ID_PREFIX = "Student with ID:";
    public static final String COURSE_WITH_ID_PREFIX = "Course with ID:";
    public static final String IS_NOT_EXISTS_SUFFIX = " is not exists.";
    @Getter(AccessLevel.NONE)
    private final StudentCourseLinkPersistenceFacade persistenceFacade;
    @Getter(AccessLevel.NONE)
    private final BusinessMessagePayloadMapper payloadMapper;
    private final int maximumRooms;
    private final int coursesExceed;

    public RegisterStudentToCourseCommand(final StudentCourseLinkPersistenceFacade persistenceFacade,
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
     * @see StudentCourseLinkPersistenceFacade#findStudentById(Long)
     * @see StudentCourseLinkPersistenceFacade#findCourseById(Long)
     * @see BusinessMessagePayloadMapper#toPayload(Student)
     * @see BusinessMessagePayloadMapper#toPayload(Course)
     * @see StudentCourseLinkPersistenceFacade#link(Student, Course)
     * @see Context
     * @see Context#setUndoParameter(Object)
     * @see Context#setResult(Object)
     * @see Context.State#WORK
     */
    @Override
    public <T> void executeDo(Context<T> context) {
        final Object parameter = context.getRedoParameter();
        try {
            log.debug("Trying to register student to course: {}", parameter);
            final Long[] ids = commandParameter(parameter);
            final Long studentId = ids[0];
            final Long courseId = ids[1];
            final Optional<Student> student = persistenceFacade.findStudentById(studentId);
            if (student.isEmpty()) {
                log.debug("No such student with id:{}", studentId);
                throw new NotExistStudentException(STUDENT_WITH_ID_PREFIX + studentId + IS_NOT_EXISTS_SUFFIX);
            }
            final Optional<Course> course = persistenceFacade.findCourseById(courseId);
            if (course.isEmpty()) {
                log.debug("No such course with id:{}", courseId);
                throw new NotExistCourseException(COURSE_WITH_ID_PREFIX + courseId + IS_NOT_EXISTS_SUFFIX);
            }

            final Student existingStudent = student.get();
            final Course existingCourse = course.get();

            if (isLinked(existingStudent, existingCourse)) {
                log.debug("student: {} with course {} are already linked", studentId, courseId);
                context.setResult(true);
                return;
            }
            if (existingCourse.getStudents().size() >= maximumRooms) {
                log.debug("Course with id:{} has students more than {}", courseId, maximumRooms);
                throw new NoRoomInTheCourseException(COURSE_WITH_ID_PREFIX + courseId + " does not have enough rooms.");
            }
            if (existingStudent.getCourses().size() >= coursesExceed) {
                log.debug("Student with id:{} has more than {} courses", studentId, coursesExceed);
                throw new StudentCoursesExceedException(STUDENT_WITH_ID_PREFIX + studentId + " exceeds maximum courses.");
            }

            log.debug("Linking student:{} to course:{}", studentId, courseId);

            final StudentToCourseLink undoLink = StudentToCourseLink.builder()
                    .student(payloadMapper.toPayload(existingStudent))
                    .course(payloadMapper.toPayload(existingCourse))
                    .build();
            final boolean linked = persistenceFacade.link(existingStudent, existingCourse);
            if (linked) {
                context.setUndoParameter(undoLink);
                context.setResult(true);
            } else {
                context.setResult(false);
            }

            log.debug("Linked student:{} to course {} {}", studentId, courseId, linked);
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
     * @see StudentCourseLinkPersistenceFacade#unLink(Student, Course)
     * @see Context
     * @see Context#getUndoParameter()
     */
    @Override
    public <T> void executeUndo(Context<T> context) {
        final Object parameter = context.getUndoParameter();
        if (isNull(parameter)) {
            log.debug("Undo parameter is null");
            context.setState(Context.State.UNDONE);
        } else {
            try {
                log.debug("Trying to undo student to course linking using: {}", parameter);

                final StudentToCourseLink undoLink = commandParameter(parameter);
                final boolean success = persistenceFacade.unLink(undoLink.getStudent(), undoLink.getCourse());
                context.setState(Context.State.UNDONE);

                log.debug("Undone student to course linking {}", success);
            } catch (Exception e) {
                log.error("Cannot undo student to course linking for {}", parameter, e);
                context.failed(e);
            }
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
