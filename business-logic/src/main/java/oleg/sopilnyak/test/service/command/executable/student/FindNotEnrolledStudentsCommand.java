package oleg.sopilnyak.test.service.command.executable.student;

import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import oleg.sopilnyak.test.school.common.model.Student;
import oleg.sopilnyak.test.school.common.persistence.education.RegisterPersistenceFacade;
import oleg.sopilnyak.test.service.command.type.StudentCommand;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.mapper.BusinessMessagePayloadMapper;
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
    private final transient BusinessMessagePayloadMapper payloadMapper;

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
     * To detach command result data from persistence layer
     *
     * @param result result data to detach
     * @return detached result data
     * @see oleg.sopilnyak.test.service.command.type.base.RootCommand#detachResultData(Context)
     */
    @Override
    public Set<Student> detachedResult(final Set<Student> result) {
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
