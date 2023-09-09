package oleg.sopilnyak.test.service.command.organization;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import oleg.sopilnyak.test.school.common.facade.peristence.OrganizationPersistenceFacade;
import oleg.sopilnyak.test.school.common.model.StudentsGroup;
import oleg.sopilnyak.test.service.command.CommandResult;
import oleg.sopilnyak.test.service.command.SchoolCommand;

import java.util.Optional;
import java.util.Set;

/**
 * Command-Implementation: command to get all authority persons of the school
 */
@Slf4j
@AllArgsConstructor
public class FindAllStudentsGroupsCommand implements SchoolCommand<Set<StudentsGroup>> {
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
            return CommandResult.<Set<StudentsGroup>>builder().result(Optional.empty()).exception(e).success(false).build();
        }
    }
}
