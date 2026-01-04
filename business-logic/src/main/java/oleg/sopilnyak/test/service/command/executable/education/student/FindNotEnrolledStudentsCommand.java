package oleg.sopilnyak.test.service.command.executable.education.student;

import oleg.sopilnyak.test.school.common.model.Student;
import oleg.sopilnyak.test.school.common.persistence.education.RegisterPersistenceFacade;
import oleg.sopilnyak.test.service.command.executable.core.BasicCommand;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.command.type.education.StudentCommand;
import oleg.sopilnyak.test.service.mapper.BusinessMessagePayloadMapper;

import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Command-Implementation: command to get not enrolled to any course students
 */
@Slf4j
@AllArgsConstructor
@Component(StudentCommand.Component.FIND_NOT_ENROLLED)
public class FindNotEnrolledStudentsCommand extends BasicCommand<Set<Student>> implements StudentCommand<Set<Student>> {
    private final transient RegisterPersistenceFacade persistenceFacade;
    @Getter
    private final transient BusinessMessagePayloadMapper payloadMapper;

    /**
     * The name of command bean in spring beans factory
     *
     * @return spring name of the command
     */
    @Override
    public String springName() {
        return Component.FIND_NOT_ENROLLED;
    }

    /**
     * To get unique command-id for the command
     *
     * @return value of command-id
     */
    @Override
    public String getId() {
        return CommandId.FIND_NOT_ENROLLED;
    }

    /**
     * To find not enrolled students<BR/>
     * To execute command redo with correct context state
     *
     * @param context context of redo execution
     * @see Context
     * @see Context#setResult(Object)
     * @see Context.State#WORK
     * @see RegisterPersistenceFacade#findNotEnrolledStudents()
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public void executeDo(Context<Set<Student>> context) {
        log.debug("Trying to find not enrolled students");
        try {

            final Set<Student> students = persistenceFacade.findNotEnrolledStudents().stream()
                    .map(this::adoptEntity).collect(Collectors.toSet());

            log.debug("Got {} students", students.size());
            context.setResult(students);
        } catch (Exception e) {
            log.error("Cannot find not enrolled students", e);
            context.failed(e);
        }
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
