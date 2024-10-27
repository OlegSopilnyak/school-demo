package oleg.sopilnyak.test.service.command.executable.organization.authority;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import oleg.sopilnyak.test.school.common.exception.accsess.SchoolAccessDeniedException;
import oleg.sopilnyak.test.school.common.exception.profile.ProfileNotFoundException;
import oleg.sopilnyak.test.school.common.model.AuthorityPerson;
import oleg.sopilnyak.test.school.common.model.PrincipalProfile;
import oleg.sopilnyak.test.school.common.persistence.PersistenceFacade;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.command.type.organization.AuthorityPersonCommand;
import oleg.sopilnyak.test.service.mapper.BusinessMessagePayloadMapper;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import java.util.Optional;

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
public class LoginAuthorityPersonCommand implements AuthorityPersonCommand {
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
    public <T> void executeDo(Context<T> context) {
        final Object parameter = context.getRedoParameter();
        try {
            log.debug("Trying to login authority person by credentials:{}", parameter);
            final String[] credentials = commandParameter(parameter);
            final String userName = credentials[0];
            final String password = credentials[1];

            log.debug("Trying to get principal-profile by username:{}", userName);
            final PrincipalProfile profile = persistence.findPrincipalProfileByLogin(userName)
                    .map(payloadMapper::toPayload)
                    .orElseThrow(() -> new ProfileNotFoundException("Profile with login:'" + userName + "', is not found"));

            log.debug("Checking the password for principal-profile by username:{}", userName);
            if (!profile.isPassword(password)) {
                log.warn("Password for login: {} is incorrect", userName);
                throw new SchoolAccessDeniedException("Login authority person command failed for username:" + userName);
            }

            Long id = profile.getId();
            log.debug("Trying to find principal person with profileId:{}", id);
            final Optional<AuthorityPerson> person = persistence.findAuthorityPersonByProfileId(id);

            log.debug("Got authority person with login:'{}' {}", person, userName);
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
     * To get reference to command's logger
     *
     * @return reference to the logger
     */
    @Override
    public Logger getLog() {
        return log;
    }
}
