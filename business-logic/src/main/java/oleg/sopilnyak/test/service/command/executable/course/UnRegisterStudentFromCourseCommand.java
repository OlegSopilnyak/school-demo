package oleg.sopilnyak.test.service.command.executable.course;

import static java.util.Objects.isNull;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import oleg.sopilnyak.test.school.common.model.Course;
import oleg.sopilnyak.test.school.common.model.Student;
import oleg.sopilnyak.test.school.common.persistence.education.joint.EducationPersistenceFacade;
import oleg.sopilnyak.test.service.command.executable.sys.CommandContext;
import oleg.sopilnyak.test.service.command.io.Input;
import oleg.sopilnyak.test.service.command.io.parameter.PairParameter;
import oleg.sopilnyak.test.service.command.type.CourseCommand;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.mapper.BusinessMessagePayloadMapper;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

/**
 * Command-Implementation: command to un-link the student from the course
 */
@Slf4j
@AllArgsConstructor
@Component
@Getter
public class UnRegisterStudentFromCourseCommand implements CourseCommand<Boolean>, EducationLinkCommand {
    private final EducationPersistenceFacade persistenceFacade;
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
     * @see CommandContext#setUndoParameter(Input)
     * @see Context#setResult(Object)
     * @see Context.State#WORK
     */
    @Override
    public void executeDo(Context<Boolean> context) {
        final Input<?> parameter = context.getRedoParameter();
        try {
            log.debug("Trying to un-link student from course: {}", parameter);
            checkNullParameter(parameter);
            log.debug("Trying to register student to course: {}", parameter);
            final PairParameter<Long> input = PairParameter.class.cast(parameter);
            final Student student = retrieveStudent(input);
            final Course course = retrieveCourse(input);
            final Long studentId = student.getId();
            final Long courseId = course.getId();

            log.debug("Un-linking student-id:{} from course-id:{}", studentId, courseId);

            final boolean successful = persistenceFacade.unLink(student, course);

            if (successful && context instanceof CommandContext<?> commandContext) {
                commandContext.setUndoParameter(Input.of(studentId, courseId));
            }

            context.setResult(successful);
            log.debug("Un-linked student-id:{} from course-id:{} successful: {}", studentId, courseId, successful);
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
        final Input<?> parameter = context.getUndoParameter();
        if (isNull(parameter) || parameter.isEmpty()) {
            log.debug("Undo parameter is null");
            context.setState(Context.State.UNDONE);
            return;
        }
        log.debug("Trying to undo student to course un-linking using: {}", parameter);
        try {
            final PairParameter<Long> input = PairParameter.class.cast(parameter);

            final boolean successful = persistenceFacade.link(retrieveStudent(input), retrieveCourse(input));

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
