package oleg.sopilnyak.test.service.command.executable.organization.authority;

import oleg.sopilnyak.test.school.common.business.facade.organization.AuthorityPersonFacade;
import oleg.sopilnyak.test.school.common.exception.profile.ProfileNotFoundException;
import oleg.sopilnyak.test.school.common.model.authentication.AccessCredentials;
import oleg.sopilnyak.test.school.common.model.organization.AuthorityPerson;
import oleg.sopilnyak.test.school.common.persistence.PersistenceFacade;
import oleg.sopilnyak.test.school.common.security.AuthenticationFacade;
import oleg.sopilnyak.test.service.command.executable.core.BasicCommand;
import oleg.sopilnyak.test.service.command.io.Input;
import oleg.sopilnyak.test.service.command.type.core.Context;
import oleg.sopilnyak.test.service.command.type.organization.AuthorityPersonCommand;
import oleg.sopilnyak.test.service.mapper.BusinessMessagePayloadMapper;

import java.util.Optional;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Command-Implementation: command to log out authority person by authorization token
 *
 * @see AuthorityPerson
 * @see AuthorityPersonCommand
 * @see PersistenceFacade
 */
@Slf4j
@Component(AuthorityPersonCommand.Component.LOGOUT)
public class LogoutAuthorityPersonCommand extends BasicCommand<Optional<AccessCredentials>>
        implements AuthorityPersonCommand<Optional<AccessCredentials>> {
    // authentication functionality facade
    private final transient AuthenticationFacade authenticationFacade;
    // mapper of common types to module's payload types
    @Getter
    private final transient BusinessMessagePayloadMapper payloadMapper;

    public LogoutAuthorityPersonCommand(AuthenticationFacade authenticationFacade, BusinessMessagePayloadMapper payloadMapper) {
        this.authenticationFacade = authenticationFacade;
        this.payloadMapper = payloadMapper;
    }

    /**
     * The name of command bean in spring beans factory
     *
     * @return spring name of the command
     */
    @Override
    public String springName() {
        return Component.LOGOUT;
    }

    /**
     * To get unique command-id for the command
     *
     * @return value of command-id
     */
    @Override
    public String getId() {
        return AuthorityPersonFacade.LOGOUT;
    }

    /**
     * DO: To logout authority person by token<BR/>
     * To execute command redo with correct context state
     *
     * @param context context of redo execution
     * @see Context
     * @see Context#getRedoParameter()
     * @see Context#setResult(Object)
     * @see Context.State#WORK
     * @see PersistenceFacade#findPrincipalProfileByLogin(String)
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void executeDo(Context<Optional<AccessCredentials>> context) {
        final Input<String> parameter = context.getRedoParameter();
        try {
            checkNullParameter(parameter);
            final String token = parameter.value();
            log.debug("Trying to logout authority person by token:'{}'", token);
            final AccessCredentials accessCredentials = authenticationFacade.signOut(token)
                    .map(payloadMapper::toPayload)
                    .orElseThrow(() -> new ProfileNotFoundException("Profile with token:'" + token + "', is not found"));
            context.setResult(Optional.of(accessCredentials));
        } catch (Exception e) {
            log.error("Cannot find the authority person with login:'{}'", parameter, e);
            context.failed(e);
        }
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
