package oleg.sopilnyak.test.service.command.executable.organization;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import oleg.sopilnyak.test.school.common.facade.peristence.OrganizationPersistenceFacade;
import oleg.sopilnyak.test.school.common.model.StudentsGroup;
import oleg.sopilnyak.test.service.command.executable.CommandResult;
import oleg.sopilnyak.test.service.command.type.OrganizationCommand;
import oleg.sopilnyak.test.service.facade.organization.entity.StudentsGroupCommandFacade;

import java.util.Optional;
import java.util.Set;

/**
 * Command-Implementation: command to get all authority persons of the school
 */
@Slf4j
@AllArgsConstructor
public class FindAllStudentsGroupsCommand implements OrganizationCommand<Set<StudentsGroup>> {
    private final OrganizationPersistenceFacade persistenceFacade;

    /**
     * To execute command's business-logic
     *
     * @param parameter command's parameter
     * @return execution's result
     */
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
        return StudentsGroupCommandFacade.FIND_ALL;
    }
}
