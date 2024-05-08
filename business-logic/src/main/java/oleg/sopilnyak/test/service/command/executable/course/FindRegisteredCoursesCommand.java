package oleg.sopilnyak.test.service.command.executable.course;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import oleg.sopilnyak.test.school.common.model.Course;
import oleg.sopilnyak.test.school.common.persistence.students.courses.RegisterPersistenceFacade;
import oleg.sopilnyak.test.service.command.type.CourseCommand;
import oleg.sopilnyak.test.service.command.type.base.Context;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Command-Implementation: command to find courses registered to student
 */
@Slf4j
@AllArgsConstructor
@Component
public class FindRegisteredCoursesCommand implements CourseCommand {
    private final RegisterPersistenceFacade persistenceFacade;

    /**
     * To find courses registered to student by id <BR/>
     * To execute command redo with correct context state
     *
     * @param context context of redo execution
     * @see RegisterPersistenceFacade#findCoursesRegisteredForStudent(Long)
     * @see Context
     * @see Context#setResult(Object)
     * @see Context.State#WORK
     */
    @Override
    public <T> void executeDo(Context<T> context) {
        final Object parameter = context.getRedoParameter();
        try {
            log.debug("Trying to find courses registered to student ID: {}", parameter);

            final Long id = commandParameter(parameter);
            final Set<Course> courses = persistenceFacade.findCoursesRegisteredForStudent(id);

            log.debug("Got courses {} for student ID:{}", courses, id);
            context.setResult(courses);
        } catch (Exception e) {
            log.error("Cannot find courses registered to student ID:{}", parameter, e);
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
        return FIND_REGISTERED_COMMAND_ID;
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
