package oleg.sopilnyak.test.service.command.executable.student;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import oleg.sopilnyak.test.school.common.model.Student;
import oleg.sopilnyak.test.school.common.persistence.education.RegisterPersistenceFacade;
import oleg.sopilnyak.test.service.command.type.StudentCommand;
import oleg.sopilnyak.test.service.command.type.base.Context;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Command-Implementation: command to get not enrolled to any course students
 */
@Slf4j
@AllArgsConstructor
@Component
public class FindNotEnrolledStudentsCommand implements StudentCommand<Set<Student>> {
    private final RegisterPersistenceFacade persistenceFacade;

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
    public void executeDo(Context<Set<Student>> context) {
        log.debug("Trying to find not enrolled students");
        try {

            Set<Student> students = persistenceFacade.findNotEnrolledStudents();
            log.debug("Got students {}", students);

            context.setResult(students);
        } catch (Exception e) {
            log.error("Cannot find not enrolled students", e);
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
        return FIND_NOT_ENROLLED;
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
