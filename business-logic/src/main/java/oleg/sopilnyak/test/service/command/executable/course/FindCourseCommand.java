package oleg.sopilnyak.test.service.command.executable.course;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import oleg.sopilnyak.test.school.common.model.Course;
import oleg.sopilnyak.test.school.common.persistence.students.courses.CoursesPersistenceFacade;
import oleg.sopilnyak.test.service.command.type.CourseCommand;
import oleg.sopilnyak.test.service.command.type.base.Context;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Command-Implementation: command to get course by id
 */
@Slf4j
@AllArgsConstructor
@Component
public class FindCourseCommand implements CourseCommand {
    private final CoursesPersistenceFacade persistenceFacade;

    /**
     * To find course by id<BR/>
     * To execute command redo with correct context state
     *
     * @param context context of redo execution
     * @see CoursesPersistenceFacade#findCourseById(Long)
     * @see Context
     * @see Context#setResult(Object)
     * @see Context.State#WORK
     */
    @Override
    public <T> void executeDo(Context<T> context) {
        final Object parameter = context.getRedoParameter();
        try {
            log.debug("Trying to find course by ID:{}", parameter.toString());

            final Long id = commandParameter(parameter);
            final Optional<Course> course = persistenceFacade.findCourseById(id);

            log.debug("Got course {} by ID:{}", course, id);
            context.setResult(course);
        } catch (Exception e) {
            log.error("Cannot find the course by ID:{}", parameter, e);
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
        return FIND_BY_ID;
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
