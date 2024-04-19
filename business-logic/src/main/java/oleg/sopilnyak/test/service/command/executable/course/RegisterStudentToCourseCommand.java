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
import oleg.sopilnyak.test.service.command.type.base.Context;
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
    public static final String STUDENT_WITH_ID_PREFIX = "Student with ID:";
    public static final String COURSE_WITH_ID_PREFIX = "Course with ID:";
    public static final String IS_NOT_EXISTS_SUFFIX = " is not exists.";
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
                        .exception(new StudentNotExistsException(STUDENT_WITH_ID_PREFIX + studentId + IS_NOT_EXISTS_SUFFIX))
                        .build();
            }
            final Optional<Course> course = persistenceFacade.findCourseById(courseId);
            if (course.isEmpty()) {
                log.debug("No such course with id:{}", courseId);
                return CommandResult.<Boolean>builder().success(false).result(Optional.of(false))
                        .exception(new CourseNotExistsException(COURSE_WITH_ID_PREFIX + courseId + IS_NOT_EXISTS_SUFFIX))
                        .build();
            }
            if (isLinked(student.get(), course.get())) {
                log.debug("student: {} with course {} are already linked", studentId, courseId);
                return CommandResult.<Boolean>builder().success(true).result(Optional.of(true)).build();
            }
            if (course.get().getStudents().size() >= maximumRooms) {
                log.debug("Course with id:{} has students more than {}", courseId, maximumRooms);
                return CommandResult.<Boolean>builder().success(false).result(Optional.of(false))
                        .exception(new NoRoomInTheCourseException(COURSE_WITH_ID_PREFIX + courseId + " does not have enough rooms."))
                        .build();
            }
            if (student.get().getCourses().size() >= coursesExceed) {
                log.debug("Student with id:{} has more than {} courses", studentId, coursesExceed);
                return CommandResult.<Boolean>builder().success(false).result(Optional.of(false))
                        .exception(new StudentCoursesExceedException(STUDENT_WITH_ID_PREFIX + studentId + " exceeds maximum courses."))
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
     * To link the student to the course<BR/>
     * To execute command redo with correct context state
     *
     * @param context context of redo execution
     * @see StudentCourseLinkPersistenceFacade#findStudentById(Long)
     * @see StudentCourseLinkPersistenceFacade#findCourseById(Long)
     * @see StudentCourseLinkPersistenceFacade#toEntity(Student)
     * @see StudentCourseLinkPersistenceFacade#toEntity(Course)
     * @see StudentCourseLinkPersistenceFacade#link(Student, Course)
     * @see Context
     * @see Context#setUndoParameter(Object)
     * @see Context#setResult(Object)
     * @see Context.State#WORK
     */
    @Override
    public void executeDo(Context<?> context) {
        final Object parameter = context.getRedoParameter();
        try {
            log.debug("Trying to register student to course: {}", parameter);
            final Long[] ids = commandParameter(parameter);
            final Long studentId = ids[0];
            final Long courseId = ids[1];
            final Optional<Student> student = persistenceFacade.findStudentById(studentId);
            if (student.isEmpty()) {
                log.debug("No such student with id:{}", studentId);
                throw new StudentNotExistsException(STUDENT_WITH_ID_PREFIX + studentId + IS_NOT_EXISTS_SUFFIX);
            }
            final Optional<Course> course = persistenceFacade.findCourseById(courseId);
            if (course.isEmpty()) {
                log.debug("No such course with id:{}", courseId);
                throw new CourseNotExistsException(COURSE_WITH_ID_PREFIX + courseId + IS_NOT_EXISTS_SUFFIX);
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

            final Object[] forUndo = new Object[]{
                    persistenceFacade.toEntity(existingStudent),
                    persistenceFacade.toEntity(existingCourse)
            };
            final boolean linked = persistenceFacade.link(existingStudent, existingCourse);
            if (linked) {
                context.setUndoParameter(forUndo);
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
    public void executeUndo(Context<?> context) {
        final Object parameter = context.getUndoParameter();
        if (isNull(parameter)) {
            log.debug("Undo parameter is null");
            context.setState(Context.State.UNDONE);
        } else {
            try {
                log.debug("Trying to undo student to course linking using: {}", parameter);

                final Object[] forUndo = commandParameter(parameter);
                final boolean success = persistenceFacade.unLink((Student) forUndo[0], (Course) forUndo[1]);
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
