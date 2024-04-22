package oleg.sopilnyak.test.service.command.executable.profile;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import oleg.sopilnyak.test.school.common.model.base.PersonProfile;
import oleg.sopilnyak.test.school.common.persistence.ProfilePersistenceFacade;
import oleg.sopilnyak.test.service.command.executable.sys.CommandResult;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.command.type.base.command.ProfileCommand;
import org.slf4j.Logger;

import java.util.Optional;

/**
 * Command-Implementation: command to get profile by id
 */
@Getter
@AllArgsConstructor
public abstract class FindProfileCommand<T> implements ProfileCommand<T> {
    private final ProfilePersistenceFacade persistenceFacade;

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
            final Optional<PersonProfile> profile = persistenceFacade.findProfileById(id);
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
     * To execute command redo with correct context state
     *
     * @param context context of redo execution
     * @see Context
     * @see Context.State#WORK
     */
    @Override
    public void executeDo(Context<?> context) {
        final Object parameter = context.getRedoParameter();
        try {
            getLog().debug("Trying to find person profile by ID:{}", parameter.toString());
            final Long id = commandParameter(parameter);
            final Optional<PersonProfile> profile = persistenceFacade.findProfileById(id);
            getLog().debug("Got profile {} by ID:{}", profile, id);
            context.setResult(profile);
        } catch (Exception e) {
            getLog().error("Cannot find the profile by ID:{}", parameter, e);
            context.failed(e);
        }
    }
}
