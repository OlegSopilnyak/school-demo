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
 * Command-Implementation: command to get profile by id
 */
@Getter
public abstract class FindProfileCommand<T, C extends PersonProfile> implements ProfileCommand<T> {
    protected final ProfilePersistenceFacade persistence;

    protected FindProfileCommand(ProfilePersistenceFacade persistence) {
        this.persistence = persistence;
    }

    protected abstract LongFunction<Optional<C>> functionFindById();

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
    public CommandResult<T> execute(Object parameter) {
        try {
            getLog().debug("Trying to find profile by ID:{}", parameter);
            final Long id = commandParameter(parameter);
            final Optional<C> profile = functionFindById().apply(id);
            getLog().debug("Got profile {} by ID:{}", profile, id);
            return CommandResult.<T>builder()
                    .result(Optional.ofNullable((T)profile))
                    .success(true)
                    .build();
        } catch (Exception e) {
            getLog().error("Cannot find the profile by ID:{}", parameter, e);
            return CommandResult.<T>builder()
                    .result((Optional<T>) Optional.of(Optional.empty()))
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
            getLog().debug("Trying to find person profile by ID:{}", parameter);
            final Long id = commandParameter(parameter);

            final Optional<C> profile = functionFindById().apply(id);

            getLog().debug("Got profile {} by ID:{}", profile, id);
            context.setResult(profile);
        } catch (Exception e) {
            getLog().error("Cannot find the profile by ID:{}", parameter, e);
            context.failed(e);
        }
    }
}
