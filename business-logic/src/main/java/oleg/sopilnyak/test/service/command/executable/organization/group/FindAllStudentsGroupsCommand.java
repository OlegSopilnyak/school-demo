package oleg.sopilnyak.test.service.command.executable.organization.group;

import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import oleg.sopilnyak.test.school.common.model.StudentsGroup;
import oleg.sopilnyak.test.school.common.persistence.organization.StudentsGroupPersistenceFacade;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.command.type.organization.StudentsGroupCommand;
import oleg.sopilnyak.test.service.mapper.BusinessMessagePayloadMapper;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Command-Implementation: command to get all students groups of the school
 *
 * @see StudentsGroup
 * @see StudentsGroupCommand
 * @see StudentsGroupPersistenceFacade
 */
@Slf4j
@AllArgsConstructor
@Component("studentsGroupFindAll")
public class FindAllStudentsGroupsCommand implements StudentsGroupCommand<Set<StudentsGroup>> {
    private final transient StudentsGroupPersistenceFacade persistence;
    private final transient BusinessMessagePayloadMapper payloadMapper;

    /**
     * DO: To get all students groups of the school<BR/>
     * To execute command redo with correct context state
     *
     * @param context context of redo execution
     * @see Context
     * @see Context.State#WORK
     * @see StudentsGroupPersistenceFacade#findAllStudentsGroups()
     */
    @Override
    public void executeDo(Context<Set<StudentsGroup>> context) {
        try {
            log.debug("Trying to get all students groups");

            final Set<StudentsGroup> groups = persistence.findAllStudentsGroups();

            log.debug("Got students groups {}", groups);
            context.setResult(groups);
        } catch (Exception e) {
            log.error("Cannot find any students groups", e);
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
        return FIND_ALL;
    }

    /**
     * To detach command result data from persistence layer
     *
     * @param result result data to detach
     * @return detached result data
     * @see #afterExecuteDo(Context)
     */
    @Override
    public Set<StudentsGroup> detachedResult(Set<StudentsGroup> result) {
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
