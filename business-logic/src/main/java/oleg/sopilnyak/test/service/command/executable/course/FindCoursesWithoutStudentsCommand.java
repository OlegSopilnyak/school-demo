package oleg.sopilnyak.test.service.command.executable.course;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import oleg.sopilnyak.test.school.common.persistence.students.courses.RegisterPersistenceFacade;
import oleg.sopilnyak.test.school.common.model.Course;
import oleg.sopilnyak.test.service.command.type.CourseCommand;
import oleg.sopilnyak.test.service.command.type.base.Context;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Command-Implementation: command to find courses without students
 */
@Slf4j
@AllArgsConstructor
@Component
public class FindCoursesWithoutStudentsCommand implements CourseCommand {
    private final RegisterPersistenceFacade persistenceFacade;

    /**
     * To find courses without students<BR/>
     * To execute command redo with correct context state
     *
     * @param context context of redo execution
     * @see RegisterPersistenceFacade#findCoursesWithoutStudents()
     * @see Context
     * @see Context#setResult(Object)
     * @see Context.State#WORK
     */
    @Override
    public void executeDo(Context<?> context) {
        try {
            log.debug("Trying to find courses without students");

            final Set<Course> courses = persistenceFacade.findCoursesWithoutStudents();

            log.debug("Got courses without student {}", courses);
            context.setResult(courses);
        } catch (Exception e) {
            log.error("Cannot find courses without students.", e);
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
        return FIND_NOT_REGISTERED_COMMAND_ID;
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
