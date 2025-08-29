package oleg.sopilnyak.test.service.command.executable.course;

import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import oleg.sopilnyak.test.school.common.model.Course;
import oleg.sopilnyak.test.school.common.persistence.education.RegisterPersistenceFacade;
import oleg.sopilnyak.test.service.command.type.CourseCommand;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.mapper.BusinessMessagePayloadMapper;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Command-Implementation: command to find courses without students
 */
@Slf4j
@AllArgsConstructor
@Component
public class FindCoursesWithoutStudentsCommand implements CourseCommand<Set<Course>> {
    private final RegisterPersistenceFacade persistenceFacade;
    private final transient BusinessMessagePayloadMapper payloadMapper;

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
    public void executeDo(Context<Set<Course>> context) {
        try {
            // no input parameters
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
        return FIND_NOT_REGISTERED;
    }

    /**
     * To detach command result data from persistence layer
     *
     * @param result result data to detach
     * @return detached result data
     * @see #detachResultData(Context)
     */
    @Override
    public Set<Course> detachedResult(final Set<Course> result) {
        return result.stream().map(payloadMapper::toPayload).collect(Collectors.toSet());
    }

    /**
     * To get mapper for business-message-payload
     *
     * @return mapper instance
     * @see BusinessMessagePayloadMapper
     */
    @Override
    public BusinessMessagePayloadMapper getPayloadMapper() {
        return payloadMapper;
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
