package oleg.sopilnyak.test.service.command.executable.profile.principal;

import static java.util.Objects.isNull;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.LongFunction;
import lombok.extern.slf4j.Slf4j;
import oleg.sopilnyak.test.school.common.model.PrincipalProfile;
import oleg.sopilnyak.test.school.common.persistence.profile.ProfilePersistenceFacade;
import oleg.sopilnyak.test.service.command.executable.profile.FindProfileCommand;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.command.type.base.RootCommand;
import oleg.sopilnyak.test.service.command.type.profile.PrincipalProfileCommand;
import oleg.sopilnyak.test.service.mapper.BusinessMessagePayloadMapper;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;


/**
 * Command-Implementation: command to find student profile instance
 *
 * @see PrincipalProfileCommand
 * @see PrincipalProfile
 * @see ProfilePersistenceFacade
 */
@Slf4j
@Component("profilePrincipalFind")
public class FindPrincipalProfileCommand extends FindProfileCommand<PrincipalProfile>
        implements PrincipalProfileCommand<Optional<PrincipalProfile>> {
    @Autowired
    // beans factory to prepare the current command for transactional operations
    private transient ApplicationContext applicationContext;
    // reference to current command for transactional operations
    private final AtomicReference<PrincipalProfileCommand<Optional<PrincipalProfile>>> self = new AtomicReference<>(null);

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
    public PrincipalProfileCommand<Optional<PrincipalProfile>> self() {
        synchronized (PrincipalProfileCommand.class) {
            if (isNull(self.get())) {
                // getting command reference which can be used for transactional operations
                // actually it's proxy of the command with transactional executeDo method
                self.getAndSet(applicationContext.getBean("profilePrincipalFind", PrincipalProfileCommand.class));
            }
        }
        return self.get();
    }

    /**
     * Constructor
     *
     * @param persistence persistence facade instance
     */
    public FindPrincipalProfileCommand(ProfilePersistenceFacade persistence, BusinessMessagePayloadMapper payloadMapper) {
        super(persistence, payloadMapper);
    }

    /**
     * To get unique command-id for the command
     *
     * @return value of command-id
     */
    @Override
    public String getId() {
        return FIND_BY_ID;
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

    /**
     * to get function to find entity by id
     *
     * @return function implementation
     */
    @Override
    protected LongFunction<Optional<PrincipalProfile>> functionFindById() {
        return persistence::findPrincipalProfileById;
    }
}
