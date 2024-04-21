package oleg.sopilnyak.test.service.command.executable.student;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import oleg.sopilnyak.test.school.common.persistence.students.courses.RegisterPersistenceFacade;
import oleg.sopilnyak.test.school.common.model.Student;
import oleg.sopilnyak.test.service.command.executable.sys.CommandResult;
import oleg.sopilnyak.test.service.command.type.StudentCommand;
import oleg.sopilnyak.test.service.command.type.base.Context;
import org.slf4j.Logger;

import java.util.Optional;
import java.util.Set;

/**
 * Command-Implementation: command to get enrolled students by course-id
 */
@Slf4j
@AllArgsConstructor
public class FindEnrolledStudentsCommand implements StudentCommand<Set<Student>> {
    private final RegisterPersistenceFacade persistenceFacade;

    /**
     * To find enrolled students by course-id
     *
     * @param parameter system course-id
     * @return execution's result
     * @deprecated commands are going to work through redo/undo
     */
    @Deprecated(forRemoval = true)
    @Override
    public CommandResult<Set<Student>> execute(Object parameter) {
        try {
            log.debug("Trying to find enrolled students by the course ID:{}", parameter);
            Long id = commandParameter(parameter);
            Set<Student> students = persistenceFacade.findEnrolledStudentsByCourseId(id);
            log.debug("Got students {} by ID:{}", students, id);
            return CommandResult.<Set<Student>>builder()
                    .result(Optional.ofNullable(students))
                    .success(true)
                    .build();
        } catch (Exception e) {
            log.error("Cannot find the student by ID:{}", parameter, e);
            return CommandResult.<Set<Student>>builder()
                    .result(Optional.of(Set.of()))
                    .exception(e).success(false).build();
        }
    }

    /**
     * To find enrolled students by course-id<BR/>
     * To execute command redo with correct context state
     *
     * @param context context of redo execution
     * @see Context
     * @see Context#setResult(Object)
     * @see Context.State#WORK
     * @see RegisterPersistenceFacade#findEnrolledStudentsByCourseId(Long)
     */
    @Override
    public void executeDo(Context<?> context) {
        final Object parameter = context.getRedoParameter();
        try {
            log.debug("Trying to find enrolled students by the course ID:{}", parameter);

            final Long id = commandParameter(parameter);
            final Set<Student> students = persistenceFacade.findEnrolledStudentsByCourseId(id);

            log.debug("Got students {} by ID:{}", students, id);
            context.setResult(students);
        } catch (Exception e) {
            log.error("Cannot find the student by ID:{}", parameter, e);
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
        return FIND_ENROLLED_COMMAND_ID;
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
