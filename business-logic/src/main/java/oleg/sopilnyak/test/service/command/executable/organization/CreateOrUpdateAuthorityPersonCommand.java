package oleg.sopilnyak.test.service.command.executable.organization;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import oleg.sopilnyak.test.school.common.facade.peristence.OrganizationPersistenceFacade;
import oleg.sopilnyak.test.school.common.model.AuthorityPerson;
import oleg.sopilnyak.test.service.command.executable.CommandResult;
import oleg.sopilnyak.test.service.command.type.OrganizationCommand;
import oleg.sopilnyak.test.service.command.id.set.AuthorityPersonCommands;

import java.util.Optional;

/**
 * Command-Implementation: command to update the authority person
 */
@Slf4j
@AllArgsConstructor
public class CreateOrUpdateAuthorityPersonCommand implements OrganizationCommand<Optional<AuthorityPerson>> {
    private final OrganizationPersistenceFacade persistenceFacade;

    /**
     * To create or update authority person instance
     *
     * @param parameter authority person instance to save
     * @return execution's result
     */
    @Override
    public CommandResult<Optional<AuthorityPerson>> execute(Object parameter) {
        try {
            log.debug("Trying to create or update authority person {}", parameter);
            AuthorityPerson instance = commandParameter(parameter);
            Optional<AuthorityPerson> person = persistenceFacade.save(instance);
            log.debug("Got stored authority person {} from parameter {}", person, instance);
            return CommandResult.<Optional<AuthorityPerson>>builder()
                    .result(Optional.ofNullable(person))
                    .success(true)
                    .build();
        } catch (Exception e) {
            log.error("Cannot create or update authority person by ID:{}", parameter, e);
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
        return AuthorityPersonCommands.CREATE_OR_UPDATE;
    }
}
