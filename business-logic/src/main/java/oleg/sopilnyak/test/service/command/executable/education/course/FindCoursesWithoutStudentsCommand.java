package oleg.sopilnyak.test.service.command.executable.education.course;

import oleg.sopilnyak.test.school.common.model.Course;
import oleg.sopilnyak.test.school.common.persistence.education.RegisterPersistenceFacade;
import oleg.sopilnyak.test.service.command.executable.core.BasicCommand;
import oleg.sopilnyak.test.service.command.type.core.Context;
import oleg.sopilnyak.test.service.command.type.education.CourseCommand;
import oleg.sopilnyak.test.service.mapper.BusinessMessagePayloadMapper;

import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Command-Implementation: command to find courses without students
 */
@Slf4j
@Component(CourseCommand.Component.FIND_NOT_REGISTERED)
public class FindCoursesWithoutStudentsCommand extends BasicCommand<Set<Course>> implements CourseCommand<Set<Course>> {
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
        return Component.FIND_NOT_REGISTERED;
    }

    /**
     * To get unique command-id for the command
     *
     * @return value of command-id
     */
    @Override
    public String getId() {
        return CommandId.FIND_NOT_REGISTERED;
    }

    public FindCoursesWithoutStudentsCommand(RegisterPersistenceFacade persistenceFacade, BusinessMessagePayloadMapper payloadMapper) {
        this.persistenceFacade = persistenceFacade;
        this.payloadMapper = payloadMapper;
    }

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
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public void executeDo(Context<Set<Course>> context) {
        try {
            // no input parameters
            log.debug("Trying to find courses without students");

            final Set<Course> courses = persistenceFacade.findCoursesWithoutStudents().stream()
                    .map(this::adoptEntity).collect(Collectors.toSet());

            log.debug("Got courses without student {}", courses);
            context.setResult(courses);
        } catch (Exception e) {
            log.error("Cannot find courses without students.", e);
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
