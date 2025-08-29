package oleg.sopilnyak.test.service.command.executable.course;

import static java.util.Objects.isNull;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import oleg.sopilnyak.test.school.common.model.Course;
import oleg.sopilnyak.test.school.common.persistence.education.CoursesPersistenceFacade;
import oleg.sopilnyak.test.service.command.io.Input;
import oleg.sopilnyak.test.service.command.type.CourseCommand;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.mapper.BusinessMessagePayloadMapper;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Command-Implementation: command to get course by id
 */
@Slf4j
@AllArgsConstructor
@Component
public class FindCourseCommand implements CourseCommand<Optional<Course>> {
    private final CoursesPersistenceFacade persistenceFacade;
    private final transient BusinessMessagePayloadMapper payloadMapper;

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
    public void executeDo(Context<Optional<Course>> context) {
        final Input<Long> parameter = context.getRedoParameter();
        try {
            checkNullParameter(parameter);
            final Long id = parameter.value();
            log.debug("Trying to find course by ID:{}", id);

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
     * To detach command result data from persistence layer
     *
     * @param result result data to detach
     * @return detached result data
     * @see #detachResultData(Context)
     */
    @Override
    public Optional<Course> detachedResult(final Optional<Course> result) {
        return isNull(result) || result.isEmpty() ? Optional.empty() : Optional.of(payloadMapper.toPayload(result.get()));
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
