package oleg.sopilnyak.test.service.command.executable.profile;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import oleg.sopilnyak.test.school.common.facade.peristence.ProfilePersistenceFacade;
import oleg.sopilnyak.test.school.common.model.PersonProfile;
import oleg.sopilnyak.test.service.command.executable.sys.CommandResult;
import oleg.sopilnyak.test.service.command.type.ProfileCommand;
import oleg.sopilnyak.test.service.command.type.base.Context;
import org.slf4j.Logger;

import java.util.Optional;

/**
 * Command-Implementation: command to get profile by id
 */
@Slf4j
@Getter
@AllArgsConstructor
public class FindProfileCommand implements ProfileCommand<Optional<PersonProfile>> {
    private final ProfilePersistenceFacade persistenceFacade;

    @Override
    public Logger getLog() {
        return log;
    }

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
    public CommandResult<Optional<PersonProfile>> execute(Object parameter) {
        try {
            log.debug("Trying to find profile by ID:{}", parameter);
            final Long id = commandParameter(parameter);
            final Optional<PersonProfile> profile = persistenceFacade.findProfileById(id);
            log.debug("Got profile {} by ID:{}", profile, id);
            return CommandResult.<Optional<PersonProfile>>builder()
                    .result(Optional.ofNullable(profile))
                    .success(true)
                    .build();
        } catch (Exception e) {
            log.error("Cannot find the profile by ID:{}", parameter, e);
            return CommandResult.<Optional<PersonProfile>>builder()
                    .result(Optional.of(Optional.empty()))
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
    public void doRedo(Context<Optional<PersonProfile>> context) {
        final Object parameter = context.getRedoParameter();
        try {
            log.debug("Trying to find person profile by ID:{}", parameter.toString());
            final Long id = commandParameter(parameter);
            final Optional<PersonProfile> profile = persistenceFacade.findProfileById(id);
            log.debug("Got profile {} by ID:{}", profile, id);
            context.setResult(profile);
        } catch (Exception e) {
            log.error("Cannot find the profile by ID:{}", parameter, e);
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
        return ProfileCommand.FIND_BY_ID_COMMAND_ID;
    }
}
