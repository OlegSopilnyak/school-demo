package oleg.sopilnyak.test.service.command.executable.organization.authority;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import oleg.sopilnyak.test.school.common.persistence.OrganizationPersistenceFacade;
import oleg.sopilnyak.test.school.common.model.AuthorityPerson;
import oleg.sopilnyak.test.service.command.executable.sys.CommandResult;
import oleg.sopilnyak.test.service.command.type.AuthorityPersonCommand;
import org.slf4j.Logger;

import java.util.Optional;
import java.util.Set;

/**
 * Command-Implementation: command to get all authority persons of the school
 */
@Slf4j
@AllArgsConstructor
public class FindAllAuthorityPersonsCommand implements AuthorityPersonCommand<Set<AuthorityPerson>> {
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