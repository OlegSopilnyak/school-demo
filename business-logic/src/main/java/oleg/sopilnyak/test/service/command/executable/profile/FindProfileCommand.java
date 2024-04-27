package oleg.sopilnyak.test.service.command.executable.profile;

import lombok.Getter;
import oleg.sopilnyak.test.school.common.model.base.PersonProfile;
import oleg.sopilnyak.test.school.common.persistence.ProfilePersistenceFacade;
import oleg.sopilnyak.test.service.command.executable.sys.CommandResult;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.command.type.base.command.ProfileCommand;

import java.util.Optional;
import java.util.function.LongFunction;

/**
 * Command-Base-Implementation: command to get profile by id
 *
 * @param <R> command's Result type
 * @param <E> command's working Entity type
 * @see PersonProfile
 * @see ProfileCommand
 * @see ProfilePersistenceFacade
 */
@Getter
public abstract class FindProfileCommand<R, E extends PersonProfile> implements ProfileCommand<R> {
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
     * To find profile (no matter type) by id
     *
     * @param parameter system profile-id
     * @return execution's result
     * @see Optional
     * @see PersonProfile
     * @deprecated commands are going to work through redo/undo
     */
    @Deprecated(forRemoval = true)
    @Override
    public CommandResult<R> execute(Object parameter) {
        try {
            getLog().debug("Trying to find profile by ID:{}", parameter);
            final Long id = commandParameter(parameter);
            final Optional<E> profile = functionFindById().apply(id);
            getLog().debug("Got profile {} by ID:{}", profile, id);
            return CommandResult.<R>builder()
                    .result(Optional.ofNullable((R) profile))
                    .success(true)
                    .build();
        } catch (Exception e) {
            getLog().error("Cannot find the profile by ID:{}", parameter, e);
            return CommandResult.<R>builder()
                    .result((Optional<R>) Optional.of(Optional.empty()))
                    .exception(e).success(false).build();
        }
    }

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
    public void executeDo(Context<?> context) {
        final Object parameter = context.getRedoParameter();
        try {
            check(parameter);
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
