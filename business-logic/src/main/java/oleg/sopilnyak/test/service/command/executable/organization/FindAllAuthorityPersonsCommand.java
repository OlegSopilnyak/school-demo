package oleg.sopilnyak.test.service.command.executable.organization;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import oleg.sopilnyak.test.school.common.facade.peristence.OrganizationPersistenceFacade;
import oleg.sopilnyak.test.school.common.model.AuthorityPerson;
import oleg.sopilnyak.test.service.command.executable.sys.CommandResult;
import oleg.sopilnyak.test.service.command.type.OrganizationCommand;
import oleg.sopilnyak.test.service.command.id.set.AuthorityPersonCommands;

import java.util.Optional;
import java.util.Set;

/**
 * Command-Implementation: command to get all authority persons of the school
 */
@Slf4j
@AllArgsConstructor
public class FindAllAuthorityPersonsCommand implements OrganizationCommand<Set<AuthorityPerson>> {
    private final OrganizationPersistenceFacade persistenceFacade;

    /**
     * To execute command's business-logic
     *
     * @param parameter not used
     * @return execution's result
     * @deprecated commands are going to work through redo/undo
     */
    @Deprecated(forRemoval = true)
    @Override
    public CommandResult<Set<AuthorityPerson>> execute(Object parameter) {
        try {
            log.debug("Trying to get all authority persons");
            final Set<AuthorityPerson> staff = persistenceFacade.findAllAuthorityPersons();
            log.debug("Got authority persons {}", staff);
            return CommandResult.<Set<AuthorityPerson>>builder()
                    .result(Optional.ofNullable(staff))
                    .success(true)
                    .build();
        } catch (Exception e) {
            log.error("Cannot find any authority person", e);
            return CommandResult.<Set<AuthorityPerson>>builder()
                    .result(Optional.of(Set.of()))
                    .exception(e).success(false).build();
        }
    }

    /**
     * To get unique command-id for the command
     *
     * @return value of command-id
     */
    @Override
    public String getId() {
        return AuthorityPersonCommands.FIND_ALL.id();
    }
}
