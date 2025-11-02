package oleg.sopilnyak.test.service.command.executable.education.course;

import static java.util.Objects.isNull;

import oleg.sopilnyak.test.school.common.model.Course;
import oleg.sopilnyak.test.school.common.persistence.education.CoursesPersistenceFacade;
import oleg.sopilnyak.test.service.command.io.Input;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.command.type.base.RootCommand;
import oleg.sopilnyak.test.service.command.type.education.CourseCommand;
import oleg.sopilnyak.test.service.mapper.BusinessMessagePayloadMapper;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Command-Implementation: command to get course by id
 */
@Slf4j
@Getter
@Component("courseFind")
public class FindCourseCommand implements CourseCommand<Optional<Course>> {
    private final transient CoursesPersistenceFacade persistenceFacade;
    private final transient BusinessMessagePayloadMapper payloadMapper;
    @Autowired
    // beans factory to prepare the current command for transactional operations
    private transient ApplicationContext applicationContext;
    // reference to current command for transactional operations
    private final AtomicReference<CourseCommand<Optional<Course>>> self = new AtomicReference<>(null);

    /**
     * Reference to the current command for transactional operations
     *
     * @return reference to the current command
     * @see RootCommand#self()
     * @see RootCommand#doCommand(Context)
     * @see RootCommand#undoCommand(Context)
     */
    @Override
    @SuppressWarnings("unchecked")
    public CourseCommand<Optional<Course>> self() {
        synchronized (CourseCommand.class) {
            if (isNull(self.get())) {
                // getting command reference which can be used for transactional operations
                // actually it's proxy of the command with transactional executeDo method
                self.getAndSet(applicationContext.getBean("courseFind", CourseCommand.class));
            }
        }
        return self.get();
    }

    public FindCourseCommand(CoursesPersistenceFacade persistenceFacade, BusinessMessagePayloadMapper payloadMapper) {
        this.persistenceFacade = persistenceFacade;
        this.payloadMapper = payloadMapper;
    }

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
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public void executeDo(Context<Optional<Course>> context) {
        final Input<Long> parameter = context.getRedoParameter();
        try {
            checkNullParameter(parameter);
            final Long id = parameter.value();
            log.debug("Trying to find course by ID:{}", id);

            final Optional<Course> course = persistenceFacade.findCourseById(id).map(this::adoptEntity);

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
