package oleg.sopilnyak.test.service.command.executable.course;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import oleg.sopilnyak.test.school.common.exception.NotExistCourseException;
import oleg.sopilnyak.test.school.common.exception.NotExistStudentException;
import oleg.sopilnyak.test.school.common.persistence.StudentCourseLinkPersistenceFacade;
import oleg.sopilnyak.test.school.common.model.Course;
import oleg.sopilnyak.test.school.common.model.Student;
import oleg.sopilnyak.test.service.command.executable.sys.CommandResult;
import oleg.sopilnyak.test.service.command.type.CourseCommand;
import oleg.sopilnyak.test.service.command.type.base.Context;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import java.util.Optional;

import static java.util.Objects.isNull;

/**
 * Command-Implementation: command to un-link the student from the course
 */
@Slf4j
@AllArgsConstructor
@Component
public class UnRegisterStudentFromCourseCommand implements CourseCommand<Boolean> {
    public static final String IS_NOT_EXISTS_SUFFIX = " is not exists.";
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
                        .exception(new NotExistStudentException("Student with ID:" + studentId + IS_NOT_EXISTS_SUFFIX))
                        .result(Optional.of(false)).build();
            }
            final Optional<Course> course = persistenceFacade.findCourseById(courseId);
            if (course.isEmpty()) {
                log.debug("No such course with id:{}", courseId);
                return CommandResult.<Boolean>builder().success(false)
                        .exception(new NotExistCourseException("Course with ID:" + courseId + IS_NOT_EXISTS_SUFFIX))
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
     * DO: To unlink the student from the course<BR/>
     * To execute command redo with correct context state
     *
     * @param context context of redo execution
     * @see StudentCourseLinkPersistenceFacade#findStudentById(Long)
     * @see StudentCourseLinkPersistenceFacade#findCourseById(Long)
     * @see StudentCourseLinkPersistenceFacade#toEntity(Student)
     * @see StudentCourseLinkPersistenceFacade#toEntity(Course)
     * @see StudentCourseLinkPersistenceFacade#unLink(Student, Course)
     * @see Context
     * @see Context#setUndoParameter(Object)
     * @see Context#setResult(Object)
     * @see Context.State#WORK
     */
    @Override
    public void executeDo(Context<?> context) {
        final Object parameter = context.getRedoParameter();
        try {
            log.debug("Trying to un-link student from course: {}", parameter);
            final Long[] ids = commandParameter(parameter);
            final Long studentId = ids[0];
            final Long courseId = ids[1];
            final Optional<Student> student = persistenceFacade.findStudentById(studentId);
            if (student.isEmpty()) {
                log.debug("No such student with id:{}", studentId);
                throw new NotExistStudentException("Student with ID:" + studentId + IS_NOT_EXISTS_SUFFIX);
            }
            final Optional<Course> course = persistenceFacade.findCourseById(courseId);
            if (course.isEmpty()) {
                log.debug("No such course with id:{}", courseId);
                throw new NotExistCourseException("Course with ID:" + courseId + IS_NOT_EXISTS_SUFFIX);
            }

            final Student existingStudent = student.get();
            final Course existingCourse = course.get();

            log.debug("Un-linking student-id:{} from course-id:{}", studentId, courseId);

            final Object[] forUndo = new Object[]{
                    persistenceFacade.toEntity(existingStudent),
                    persistenceFacade.toEntity(existingCourse)
            };
            final boolean unLinked = persistenceFacade.unLink(existingStudent, existingCourse);
            if (unLinked) {
                context.setUndoParameter(forUndo);
                context.setResult(true);
            } else {
                context.setResult(false);
            }

            log.debug("Un-linked student-id:{} from course-id:{} {}", studentId, courseId, unLinked);
        } catch (Exception e) {
            log.error("Cannot link student to course {}", parameter, e);
            context.failed(e);
        }
    }

    /**
     * UNDO: To unlink the student from the course<BR/>
     * To rollback command's execution with correct context state
     *
     * @param context context of redo execution
     * @see StudentCourseLinkPersistenceFacade#link(Student, Course)
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
                log.debug("Trying to undo student to course un-linking using: {}", parameter);

                final Object[] forUndo = commandParameter(parameter);
                final boolean success = persistenceFacade.link((Student) forUndo[0], (Course) forUndo[1]);
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
