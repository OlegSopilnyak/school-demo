package oleg.sopilnyak.test.service.command.executable.course;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import oleg.sopilnyak.test.school.common.exception.education.CourseNotFoundException;
import oleg.sopilnyak.test.school.common.exception.education.StudentNotFoundException;
import oleg.sopilnyak.test.school.common.model.Course;
import oleg.sopilnyak.test.school.common.model.Student;
import oleg.sopilnyak.test.school.common.persistence.education.joint.EducationPersistenceFacade;
import oleg.sopilnyak.test.service.command.executable.sys.CommandContext;
import oleg.sopilnyak.test.service.command.io.Input;
import oleg.sopilnyak.test.service.command.type.CourseCommand;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.mapper.BusinessMessagePayloadMapper;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import static java.util.Objects.isNull;

/**
 * Command-Implementation: command to un-link the student from the course
 */
@Slf4j
@AllArgsConstructor
@Component
public class UnRegisterStudentFromCourseCommand implements CourseCommand<Boolean>, EducationLinkCommand {
    private final EducationPersistenceFacade persistenceFacade;
    @Getter
    private final BusinessMessagePayloadMapper payloadMapper;

    /**
     * DO: To unlink the student from the course<BR/>
     * To execute command redo with correct context state
     *
     * @param context context of redo execution
     * @see EducationLinkCommand#detached(Course)
     * @see EducationLinkCommand#detached(Student) 
     * @see EducationPersistenceFacade#findStudentById(Long)
     * @see EducationPersistenceFacade#findCourseById(Long)
     * @see EducationPersistenceFacade#unLink(Student, Course)
     * @see Context
     * @see Context#setUndoParameter(Object)
     * @see Context#setResult(Object)
     * @see Context.State#WORK
     */
    @Override
    public void executeDo(Context<Boolean> context) {
        final Object parameter = context.getRedoParameter();
        try {
            log.debug("Trying to un-link student from course: {}", parameter);
            final Long[] ids = commandParameter(parameter);
            final Long studentId = ids[0];
            final Long courseId = ids[1];
            final Student student = persistenceFacade.findStudentById(studentId)
                    .orElseThrow(() -> new StudentNotFoundException("Student with ID:" + studentId + " is not exists."));
            final Course course = persistenceFacade.findCourseById(courseId)
                    .orElseThrow(() -> new CourseNotFoundException("Course with ID:" + courseId + " is not exists."));

            final var undoLink = new StudentToCourseLink(detached(student), detached(course));
            log.debug("Un-linking student-id:{} from course-id:{}", studentId, courseId);

            final boolean successful = persistenceFacade.unLink(student, course);

            if (context instanceof CommandContext commandContext) {
                commandContext.setUndoParameter(Input.of(undoLink));
                commandContext.setResult(successful);
                log.debug("Un-linked student-id:{} from course-id:{} successful: {}", studentId, courseId, successful);
            }
//            if (successful) {
//                context.setUndoParameter(undoLink);
//            }
//            context.setResult(successful);
//            log.debug("Un-linked student-id:{} from course-id:{} successful: {}", studentId, courseId, successful);
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
     * @see EducationPersistenceFacade#link(Student, Course)
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
        log.debug("Trying to undo student to course un-linking using: {}", parameter);
        try {
            final StudentToCourseLink undoLink = commandParameter(parameter);

            final boolean successful = persistenceFacade.link(undoLink.getStudent(), undoLink.getCourse());

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
        return UN_REGISTER;
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
