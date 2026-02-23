package oleg.sopilnyak.test.service.command.executable.organization.authority;

import oleg.sopilnyak.test.school.common.business.facade.organization.AuthorityPersonFacade;
import oleg.sopilnyak.test.school.common.exception.core.InvalidParameterTypeException;
import oleg.sopilnyak.test.school.common.exception.profile.ProfileNotFoundException;
import oleg.sopilnyak.test.school.common.model.authentication.AccessCredentials;
import oleg.sopilnyak.test.school.common.model.organization.AuthorityPerson;
import oleg.sopilnyak.test.school.common.persistence.PersistenceFacade;
import oleg.sopilnyak.test.school.common.security.AuthenticationFacade;
import oleg.sopilnyak.test.service.command.executable.core.BasicCommand;
import oleg.sopilnyak.test.service.command.executable.core.context.CommandContext;
import oleg.sopilnyak.test.service.command.io.Input;
import oleg.sopilnyak.test.service.command.io.parameter.PairParameter;
import oleg.sopilnyak.test.service.command.type.core.Context;
import oleg.sopilnyak.test.service.command.type.core.RootCommand;
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
 * Command-Implementation: command to log in authority person by username/password
 *
 * @see AuthorityPerson
 * @see AuthorityPersonCommand
 * @see PersistenceFacade
 */
@Slf4j
@Component(AuthorityPersonCommand.Component.LOGIN)
public class LoginAuthorityPersonCommand extends BasicCommand<Optional<AccessCredentials>>
        implements AuthorityPersonCommand<Optional<AccessCredentials>> {
    // authentication functionality facade
    private final transient AuthenticationFacade authenticationFacade;
//    private final transient PersistenceFacade persistence;
    // mapper of common types to module's payload types
    @Getter
    private final transient BusinessMessagePayloadMapper payloadMapper;

    /**
     * The name of command bean in spring beans factory
     *
     * @return spring name of the command
     */
    @Override
    public String springName() {
        return Component.LOGIN;
    }

    /**
     * To get unique command-id for the command
     *
     * @return value of command-id
     */
    @Override
    public String getId() {
        return AuthorityPersonFacade.LOGIN;
    }

//    public LoginAuthorityPersonCommand(AuthenticationFacade authentication, PersistenceFacade persistence,
//                                       BusinessMessagePayloadMapper payloadMapper) {
    public LoginAuthorityPersonCommand(AuthenticationFacade authentication, BusinessMessagePayloadMapper payloadMapper) {
        this.authenticationFacade = authentication;
//        this.persistence = persistence;
        this.payloadMapper = payloadMapper;
    }

    /**
     * DO: To login authority person by credentials<BR/>
     * To execute command redo with correct context state
     *
     * @param context context of redo execution
     * @see Context
     * @see Context#getRedoParameter()
     * @see Context#setResult(Object)
     * @see Context.State#WORK
     * @see PersistenceFacade#findPrincipalProfileByLogin(String)
     */
    @SuppressWarnings("unchecked")
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void executeDo(Context<Optional<AccessCredentials>> context) {
        final Input<?> parameter = context.getRedoParameter();
        try {
            checkNullParameter(parameter);
            log.debug("Trying to sign in authority person...");
            final PairParameter<String> credentials = (PairParameter<String>) parameter;
            final String username = credentials.first();
            final String password = credentials.second();

            log.debug("Trying to sign in authority person with username:'{}'", username);
            final AccessCredentials accessCredentials = authenticationFacade.signIn(username, password)
                    .map(payloadMapper::toPayload)
                    .orElseThrow(() -> new ProfileNotFoundException("Profile with login:'" + username + "', is not found"));
            if (context instanceof CommandContext<Optional<AccessCredentials>> commandContext) {
                commandContext.setResult(Optional.of(accessCredentials));
                commandContext.setUndoParameter(Input.of(accessCredentials.getToken()));
            } else {
                throw new InvalidParameterTypeException("CommandContext", context);
            }
//            log.debug("Trying to get principal-profile by username:{}", username);
//            final PrincipalProfile profile = persistence.findPrincipalProfileByLogin(username)
//                    .map(payloadMapper::toPayload)
//                    .orElseThrow(() -> new ProfileNotFoundException("Profile with login:'" + username + "', is not found"));
//
//            log.debug("Checking the password for principal-profile by username:{}", username);
//            if (!profile.isPassword(password)) {
//                log.warn("Password for login: {} is incorrect", username);
//                throw new SchoolAccessDeniedException("Login authority person command failed for username:" + username);
//            }
//
//            final Long profileId = profile.getId();
//            log.debug("Trying to find principal person with profileId:{}", profileId);
//            final Optional<AuthorityPerson> person = persistence.findAuthorityPersonByProfileId(profileId);
//
//            log.debug("Got authority person with login:'{}' {}", person, username);
//            context.setResult(person.isEmpty() ? person : person.map(payloadMapper::toPayload));
        } catch (Exception e) {
            log.error("Cannot sign in the authority person with credentials: {}", parameter, e);
            context.failed(e);
        }
    }

    /**
     * To sign out user signed in in method above<BR/>
     * To rollback command's execution with correct context state
     * <BR/> the type of command result doesn't matter
     *
     * @param context context of redo execution
     * @see Context.State#DONE
     * @see Context#setState(Context.State)
     * @see RootCommand#executeUndo(Context)
     */
    @Override
    public void executeUndo(Context<?> context) {
        final Input<String> parameter = context.getUndoParameter();
        try {
            log.debug("Trying to sign out authority person...");

            final String activeToken = parameter.value();
            authenticationFacade.signOut(activeToken);
            log.debug("Person is signed out, token: {}", activeToken);
        } catch (Exception e) {
            log.error("Cannot sign out the authority person with credentials: {}", parameter, e);
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
