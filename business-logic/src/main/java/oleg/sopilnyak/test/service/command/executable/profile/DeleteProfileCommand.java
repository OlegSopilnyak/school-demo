package oleg.sopilnyak.test.service.command.executable.profile;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import oleg.sopilnyak.test.school.common.exception.ProfileNotExistsException;
import oleg.sopilnyak.test.school.common.facade.peristence.ProfilePersistenceFacade;
import oleg.sopilnyak.test.school.common.model.PersonProfile;
import oleg.sopilnyak.test.service.command.executable.sys.CommandResult;
import oleg.sopilnyak.test.service.command.id.set.ProfileCommands;
import oleg.sopilnyak.test.service.command.type.ProfileCommand;
import oleg.sopilnyak.test.service.command.type.base.Context;
import org.slf4j.Logger;

import java.util.Optional;

/**
 * Command-Implementation: command to delete person profile instance by id
 */
@Slf4j
@Getter
@AllArgsConstructor
public class DeleteProfileCommand implements ProfileCommand<Boolean> {
    private final ProfilePersistenceFacade persistenceFacade;

    @Override
    public Logger getLog() {
        return log;
    }

    /**
     * To delete person's profile by id
     *
     * @param parameter system-id of person-profile to delete
     * @return execution's result
     * @see PersonProfile
     */
    @Override
    public CommandResult<Boolean> execute(Object parameter) {
        try {
            log.debug("Trying to delete person profile {}", parameter);
            final Long id = commandParameter(parameter);
            final Optional<PersonProfile> profile = persistenceFacade.findProfileById(id);
            if (profile.isEmpty()) {
                log.debug("Person profile with ID:{} is not exists.", id);
                return CommandResult.<Boolean>builder().result(Optional.of(false))
                        .exception(new ProfileNotExistsException("Profile with ID:" + id + " is not exists."))
                        .success(false).build();
            }
            persistenceFacade.deleteProfileById(id);
            log.debug("Person profile with ID:{} is deleted '{}'", id, true);
            return CommandResult.<Boolean>builder()
                    .result(Optional.of(true))
                    .success(true)
                    .build();
        } catch (Exception e) {
            log.error("Cannot delete the person profile {}", parameter, e);
            return CommandResult.<Boolean>builder()
                    .result(Optional.of(false))
                    .exception(e).success(false).build();
        }
    }

    /**
     * To get unique command-id for the command
     *
     * @return value of command-id
     */
    @Override
    public String getId() {
        return ProfileCommands.DELETE_BY_ID.id();
    }

    /**
     * To execute command redo with correct context state
     *
     * @param context context of redo execution
     * @see Context
     * @see Context.State#WORK
     */
    @Override
    public void doRedo(Context<Boolean> context) {
        final Object parameter = context.getDoParameter();
        try {
            log.debug("Trying to delete person profile using: {}", parameter.toString());
            final Long id = commandParameter(parameter);
            cacheProfileForRollback(context, id);
            persistenceFacade.deleteProfileById(id);
            context.setResult(true);
            log.debug("Deleted person profile with ID: {}", id);
        } catch (Exception e) {
            context.setResult(false);
            log.error("Cannot delete profile with :{}", parameter, e);
            context.failed(e);
            rollbackCachedProfile(context);
        }
    }

    /**
     * To rollback command's execution with correct context state
     *
     * @param context context of redo execution
     * @see Context
     * @see Context#getUndoParameter()
     */
    @Override
    public void doUndo(Context<Boolean> context) {
        final Object parameter = context.getUndoParameter();
        try {
            log.debug("Trying to undo person profile deletion using: {}", parameter.toString());
            if (parameter instanceof PersonProfile profile) {
                persistenceFacade.saveProfile(profile);
                log.debug("Got restored person profile {}", profile);
            } else {
                throw new NullPointerException("Wrong undo parameter :" + parameter);
            }
            context.setState(Context.State.UNDONE);
        } catch (Exception e) {
            log.error("Cannot undo profile deletion {}", parameter, e);
            context.failed(e);
        }
    }
}
