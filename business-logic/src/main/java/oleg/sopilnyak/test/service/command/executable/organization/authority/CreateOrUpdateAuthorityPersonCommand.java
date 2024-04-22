package oleg.sopilnyak.test.service.command.executable.organization.authority;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import oleg.sopilnyak.test.school.common.model.AuthorityPerson;
import oleg.sopilnyak.test.school.common.persistence.organization.AuthorityPersonPersistenceFacade;
import oleg.sopilnyak.test.service.command.executable.sys.CommandResult;
import oleg.sopilnyak.test.service.command.type.AuthorityPersonCommand;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Command-Implementation: command to update the authority person
 */
@Slf4j
@AllArgsConstructor
@Component
public class CreateOrUpdateAuthorityPersonCommand implements AuthorityPersonCommand<Optional<AuthorityPerson>> {
    private final AuthorityPersonPersistenceFacade persistenceFacade;

    /**
     * To create or update authority person instance
     *
     * @param parameter authority person instance to save
     * @return execution's result
     * @deprecated commands are going to work through redo/undo
     */
    @Deprecated(forRemoval = true)
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
        return CREATE_OR_UPDATE;
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
