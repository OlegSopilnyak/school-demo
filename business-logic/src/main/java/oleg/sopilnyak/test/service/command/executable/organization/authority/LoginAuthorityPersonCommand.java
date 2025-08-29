package oleg.sopilnyak.test.service.command.executable.organization.authority;

import static java.util.Objects.isNull;

import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import oleg.sopilnyak.test.school.common.exception.accsess.SchoolAccessDeniedException;
import oleg.sopilnyak.test.school.common.exception.profile.ProfileNotFoundException;
import oleg.sopilnyak.test.school.common.model.AuthorityPerson;
import oleg.sopilnyak.test.school.common.model.PrincipalProfile;
import oleg.sopilnyak.test.school.common.persistence.PersistenceFacade;
import oleg.sopilnyak.test.service.command.io.Input;
import oleg.sopilnyak.test.service.command.io.parameter.PairParameter;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.command.type.organization.AuthorityPersonCommand;
import oleg.sopilnyak.test.service.mapper.BusinessMessagePayloadMapper;
import oleg.sopilnyak.test.service.message.payload.AuthorityPersonPayload;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

/**
 * Command-Implementation: command to log in authority person by username/password
 *
 * @see AuthorityPerson
 * @see AuthorityPersonCommand
 * @see PersistenceFacade
 */
@Slf4j
@AllArgsConstructor
@Component
public class LoginAuthorityPersonCommand implements AuthorityPersonCommand<Optional<AuthorityPerson>> {
    private final PersistenceFacade persistence;
    private final BusinessMessagePayloadMapper payloadMapper;


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
    @Override
    public void executeDo(Context<Optional<AuthorityPerson>> context) {
        final Input<?> parameter = context.getRedoParameter();
        try {
            checkNullParameter(parameter);
            log.debug("Trying to login authority person by credentials:{}", parameter);
            PairParameter<String> credentials = PairParameter.class.cast(parameter);
            final String username = credentials.first();
            final String password = credentials.second();

            log.debug("Trying to get principal-profile by username:{}", username);
            final PrincipalProfile profile = persistence.findPrincipalProfileByLogin(username)
                    .map(payloadMapper::toPayload)
                    .orElseThrow(() -> new ProfileNotFoundException("Profile with login:'" + username + "', is not found"));

            log.debug("Checking the password for principal-profile by username:{}", username);
            if (!profile.isPassword(password)) {
                log.warn("Password for login: {} is incorrect", username);
                throw new SchoolAccessDeniedException("Login authority person command failed for username:" + username);
            }

            Long id = profile.getId();
            log.debug("Trying to find principal person with profileId:{}", id);
            final Optional<AuthorityPerson> person = persistence.findAuthorityPersonByProfileId(id);

            log.debug("Got authority person with login:'{}' {}", person, username);
            context.setResult(person.isEmpty() ? person : person.map(payloadMapper::toPayload));
        } catch (Exception e) {
            log.error("Cannot find the authority person with login:'{}'", parameter, e);
            context.failed(e);
        }
    }

    /**
     * To get unique command-id for the command
     *
     * @return value of command-id
     */
    @Override
    public String getId() {
        return LOGIN;
    }

    /**
     * To detach command result data from persistence layer
     *
     * @param result result data to detach
     * @return detached result data
     * @see #detachResultData(Context)
     */
    @Override
    public Optional<AuthorityPerson> detachedResult(final Optional<AuthorityPerson> result) {
        return isNull(result) || result.isEmpty() ? Optional.empty() : Optional.of(
                result.get() instanceof AuthorityPersonPayload payload ?
                        payload
                        :
                        payloadMapper.toPayload(result.get())
        );
    }

    /**
     * To get mapper for business-message-payload
     *
     * @return mapper instance
     * @see BusinessMessagePayloadMapper
     */
    @Override
    public BusinessMessagePayloadMapper getPayloadMapper() {
        return payloadMapper;
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
