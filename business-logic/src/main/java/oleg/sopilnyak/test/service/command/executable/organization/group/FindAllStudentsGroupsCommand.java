package oleg.sopilnyak.test.service.command.executable.organization.group;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import oleg.sopilnyak.test.school.common.persistence.OrganizationPersistenceFacade;
import oleg.sopilnyak.test.school.common.model.StudentsGroup;
import oleg.sopilnyak.test.school.common.persistence.organization.StudentsGroupPersistenceFacade;
import oleg.sopilnyak.test.service.command.executable.sys.CommandResult;
import oleg.sopilnyak.test.service.command.type.StudentsGroupCommand;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.Set;

/**
 * Command-Implementation: command to get all authority persons of the school
 */
@Slf4j
@AllArgsConstructor
@Component
public class FindAllStudentsGroupsCommand implements StudentsGroupCommand<Set<StudentsGroup>> {
    private final StudentsGroupPersistenceFacade persistenceFacade;

    /**
     * To execute command's business-logic
     *
     * @param parameter not used
     * @return execution's result
     * @deprecated commands are going to work through redo/undo
     */
    @Deprecated(forRemoval = true)
    @Override
    public CommandResult<Set<StudentsGroup>> execute(Object parameter) {
        try {
            log.debug("Trying to get all students groups");
            final Set<StudentsGroup> groups = persistenceFacade.findAllStudentsGroups();
            log.debug("Got students groups {}", groups);
            return CommandResult.<Set<StudentsGroup>>builder()
                    .result(Optional.ofNullable(groups))
                    .success(true)
                    .build();
        } catch (Exception e) {
            log.error("Cannot find any students group", e);
            return CommandResult.<Set<StudentsGroup>>builder()
                    .result(Optional.of(Set.of())).exception(e).success(false).build();
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
     * To get reference to command's logger
     *
     * @return reference to the logger
     */
    @Override
    public Logger getLog() {
        return log;
    }
}
