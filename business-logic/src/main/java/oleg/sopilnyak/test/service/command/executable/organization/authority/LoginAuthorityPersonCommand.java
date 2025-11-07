package oleg.sopilnyak.test.service.command.executable.organization.authority;

import static java.util.Objects.isNull;

import oleg.sopilnyak.test.school.common.exception.accsess.SchoolAccessDeniedException;
import oleg.sopilnyak.test.school.common.exception.profile.ProfileNotFoundException;
import oleg.sopilnyak.test.school.common.model.AuthorityPerson;
import oleg.sopilnyak.test.school.common.model.PrincipalProfile;
import oleg.sopilnyak.test.school.common.persistence.PersistenceFacade;
import oleg.sopilnyak.test.service.command.io.Input;
import oleg.sopilnyak.test.service.command.io.parameter.PairParameter;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.command.type.base.RootCommand;
import oleg.sopilnyak.test.service.command.type.education.CourseCommand;
import oleg.sopilnyak.test.service.command.type.organization.AuthorityPersonCommand;
import oleg.sopilnyak.test.service.mapper.BusinessMessagePayloadMapper;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
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
@Component("authorityPersonLogin")
public class LoginAuthorityPersonCommand implements AuthorityPersonCommand<Optional<AuthorityPerson>> {
    private final transient PersistenceFacade persistence;
    @Getter
    private final transient BusinessMessagePayloadMapper payloadMapper;
    @Autowired
    // beans factory to prepare the current command for transactional operations
    private transient ApplicationContext applicationContext;
    // reference to current command for transactional operations
    private final AtomicReference<AuthorityPersonCommand<Optional<AuthorityPerson>>> self = new AtomicReference<>(null);

    /**
     * Reference to the current command for transactional operations
     *
     * @return reference to the current command
     * @see RootCommand#self()
     * @see RootCommand#doCommand(Context)
     * @see RootCommand#undoCommand(Context)
     */
    @Override
    @SuppressWarnings("unchecked")
    public AuthorityPersonCommand<Optional<AuthorityPerson>> self() {
        synchronized (CourseCommand.class) {
            if (isNull(self.get())) {
                // getting command reference which can be used for transactional operations
                // actually it's proxy of the command with transactional executeDo method
                self.getAndSet(applicationContext.getBean("authorityPersonLogin", AuthorityPersonCommand.class));
            }
        }
        return self.get();
    }

    public LoginAuthorityPersonCommand(PersistenceFacade persistence, BusinessMessagePayloadMapper payloadMapper) {
        this.persistence = persistence;
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
    public void executeDo(Context<Optional<AuthorityPerson>> context) {
        final Input<?> parameter = context.getRedoParameter();
        try {
            checkNullParameter(parameter);
            log.debug("Trying to login authority person by credentials:{}", parameter);
            PairParameter<String> credentials = (PairParameter<String>) parameter;
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

            final Long profileId = profile.getId();
            log.debug("Trying to find principal person with profileId:{}", profileId);
            final Optional<AuthorityPerson> person = persistence.findAuthorityPersonByProfileId(profileId);

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
     * To get reference to command's logger
     *
     * @return reference to the logger
     */
    @Override
    public Logger getLog() {
        return log;
    }
}
