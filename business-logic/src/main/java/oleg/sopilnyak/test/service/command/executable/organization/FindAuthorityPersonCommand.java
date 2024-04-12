package oleg.sopilnyak.test.service.command.executable.organization;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import oleg.sopilnyak.test.school.common.facade.peristence.OrganizationPersistenceFacade;
import oleg.sopilnyak.test.school.common.model.AuthorityPerson;
import oleg.sopilnyak.test.service.command.executable.sys.CommandResult;
import oleg.sopilnyak.test.service.command.id.set.AuthorityPersonCommands;
import oleg.sopilnyak.test.service.command.type.OrganizationCommand;

import java.util.Optional;

/**
 * Command-Implementation: command to get authority person by id
 */
@Slf4j
@AllArgsConstructor
public class FindAuthorityPersonCommand implements OrganizationCommand<Optional<AuthorityPerson>> {
    private final OrganizationPersistenceFacade persistenceFacade;

    /**
     * To find authority person by id
     *
     * @param parameter system authority-person-id
     * @return execution's result
     * @deprecated commands are going to work through redo/undo
     */
    @Deprecated(forRemoval = true)
    @Override
    public CommandResult<Optional<AuthorityPerson>> execute(Object parameter) {
        try {
            log.debug("Trying to find authority person by ID:{}", parameter);
            Long id = commandParameter(parameter);
            Optional<AuthorityPerson> person = persistenceFacade.findAuthorityPersonById(id);
            log.debug("Got authority person {} by ID:{}", person, id);
            return CommandResult.<Optional<AuthorityPerson>>builder()
                    .result(Optional.ofNullable(person))
                    .success(true)
                    .build();
        } catch (Exception e) {
            log.error("Cannot find the authority person by ID:{}", parameter, e);
            return CommandResult.<Optional<AuthorityPerson>>builder()
                    .result(Optional.of(Optional.empty()))
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
        return AuthorityPersonCommands.FIND_BY_ID.id();
    }
}
