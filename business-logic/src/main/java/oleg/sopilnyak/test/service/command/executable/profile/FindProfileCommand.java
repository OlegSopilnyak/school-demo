package oleg.sopilnyak.test.service.command.executable.profile;

import lombok.Getter;
import oleg.sopilnyak.test.school.common.model.PersonProfile;
import oleg.sopilnyak.test.school.common.persistence.profile.ProfilePersistenceFacade;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.command.type.profile.base.ProfileCommand;

import java.util.Optional;
import java.util.function.LongFunction;

/**
 * Command-Base-Implementation: command to get profile by id
 *
 * @param <E> command's working Entity type
 * @see PersonProfile
 * @see ProfileCommand
 * @see ProfilePersistenceFacade
 */
@Getter
public abstract class FindProfileCommand<E extends PersonProfile> implements ProfileCommand<Optional<E>> {
    protected final ProfilePersistenceFacade persistence;

    protected FindProfileCommand(ProfilePersistenceFacade persistence) {
        this.persistence = persistence;
    }

    /**
     * to get function to find entity by id
     *
     * @return function implementation
     */
    protected abstract LongFunction<Optional<E>> functionFindById();

    /**
     * DO: To find profile (no matter type) by id<BR/>
     * To execute command redo with correct context state
     *
     * @param context context of redo execution
     * @see Context
     * @see Context#getRedoParameter()
     * @see Context.State#WORK
     * @see this#functionFindById()
     */
    @Override
    public void executeDo(Context<Optional<E>> context) {
        final Object parameter = context.getRedoParameter();
        try {
            checkNullParameter(parameter);
            getLog().debug("Trying to find profile by ID:{}", parameter);
            final Long id = commandParameter(parameter);

            final Optional<E> profile = functionFindById().apply(id);

            getLog().debug("Got profile {} by ID:{}", profile, id);
            context.setResult(profile);
        } catch (Exception e) {
            getLog().error("Cannot find the profile by ID:{}", parameter, e);
            context.failed(e);
        }
    }
}
