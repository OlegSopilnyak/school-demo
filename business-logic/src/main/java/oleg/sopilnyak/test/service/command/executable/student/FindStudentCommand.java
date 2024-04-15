package oleg.sopilnyak.test.service.command.executable.student;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import oleg.sopilnyak.test.school.common.facade.peristence.students.courses.StudentsPersistenceFacade;
import oleg.sopilnyak.test.school.common.model.Student;
import oleg.sopilnyak.test.service.command.executable.sys.CommandResult;
import oleg.sopilnyak.test.service.command.type.StudentCommand;
import oleg.sopilnyak.test.service.command.type.base.Context;
import org.slf4j.Logger;

import java.util.Optional;

/**
 * Command-Implementation: command to get student by id
 */
@Slf4j
@AllArgsConstructor
public class FindStudentCommand implements StudentCommand<Optional<Student>> {
    private final StudentsPersistenceFacade persistenceFacade;

    /**
     * To find student by id
     *
     * @param parameter system student-id
     * @return execution's result
     * @deprecated commands are going to work through redo/undo
     */
    @Deprecated(forRemoval = true)
    @Override
    public CommandResult<Optional<Student>> execute(Object parameter) {
        try {
            log.debug("Trying to find student by ID:{}", parameter);
            Long id = commandParameter(parameter);
            Optional<Student> student = persistenceFacade.findStudentById(id);
            log.debug("Got student {} by ID:{}", student, id);
            return CommandResult.<Optional<Student>>builder()
                    .result(Optional.ofNullable(student))
                    .success(true)
                    .build();
        } catch (Exception e) {
            log.error("Cannot find the student by ID:{}", parameter, e);
            return CommandResult.<Optional<Student>>builder()
                    .result(Optional.of(Optional.empty())).exception(e).success(false).build();
        }
    }

    /**
     * To find student by id<BR/>
     * To execute command redo with correct context state
     *
     * @param context context of redo execution
     * @see Context
     * @see Context#setResult(Object)
     * @see Context.State#WORK
     * @see StudentsPersistenceFacade#findStudentById(Long)
     */
    @Override
    public void executeDo(Context<?> context) {
        final Object parameter = context.getRedoParameter();
        try {
            log.debug("Trying to find person profile by ID:{}", parameter.toString());

            final Long id = commandParameter(parameter);
            final Optional<Student> student = persistenceFacade.findStudentById(id);

            log.debug("Got student {} by ID:{}", student, id);
            context.setResult(student);
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
    public final String getId() {
        return FIND_BY_ID_COMMAND_ID;
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
