package oleg.sopilnyak.test.service.command.executable.profile;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import oleg.sopilnyak.test.school.common.exception.ProfileNotExistsException;
import oleg.sopilnyak.test.school.common.facade.peristence.ProfilePersistenceFacade;
import oleg.sopilnyak.test.school.common.model.PersonProfile;
import oleg.sopilnyak.test.school.common.model.PrincipalProfile;
import oleg.sopilnyak.test.service.command.executable.sys.CommandResult;
import oleg.sopilnyak.test.service.command.id.set.ProfileCommands;
import oleg.sopilnyak.test.service.command.type.ProfileCommand;
import oleg.sopilnyak.test.service.command.type.base.Context;

import java.util.Optional;

import static oleg.sopilnyak.test.school.common.facade.PersonProfileFacade.isInvalidId;

/**
 * Command-Implementation: command to update person profile instance
 */
@Slf4j
@AllArgsConstructor
public class CreateOrUpdateProfileCommand implements ProfileCommand<Optional<PersonProfile>> {
    private final ProfilePersistenceFacade persistenceFacade;

    /**
     * To update person's profile
     *
     * @param parameter system principal-profile instance
     * @return execution's result
     * @see Optional
     * @see PrincipalProfile
     */
    @Override
    public CommandResult<Optional<PersonProfile>> execute(Object parameter) {
        try {
            log.debug("Trying to update person profile {}", parameter);
            final PersonProfile input = commandParameter(parameter);
            final Optional<PersonProfile> profile = persistenceFacade.saveProfile(input);
            log.debug("Got saved \nperson profile {}\n for input {}", profile, input);
            return CommandResult.<Optional<PersonProfile>>builder()
                    .result(Optional.of(profile))
                    .success(true)
                    .build();
        } catch (Exception e) {
            log.error("Cannot save the profile {}", parameter, e);
            return CommandResult.<Optional<PersonProfile>>builder()
                    .result(Optional.of(Optional.empty()))
                    .exception(e).success(false).build();
        }
    }

    /**
     * To execute command (update person's profile)
     *
     * @param context context of redo execution
     * @see Context
     */
    @Override
    public void redo(Context<Optional<PersonProfile>> context) {
        if (isWrongRedoStateOf(context)) {
            log.warn("Cannot do redo of command {} with context:state '{}'", getId(), context.getState());
            context.setState(Context.State.FAIL);
            return;
        }
        final Object parameter = context.getDoParameter();
        context.setState(Context.State.WORK);
        try {
            log.debug("Trying to update person profile {}", parameter.toString());
            final PersonProfile input = commandParameter(parameter);
            final Long inputId = input.getId();
            final boolean isCreateProfile = isInvalidId(inputId);
            if (!isCreateProfile) {
                final PersonProfile existsProfile = persistenceFacade.findProfileById(inputId)
                        .orElseThrow(() -> new ProfileNotExistsException("PersonProfile with ID:" + inputId + " is not exists."));
                context.setUndoParameter(existsProfile);
            }
            final Optional<PersonProfile> profile = persistenceFacade.saveProfile(input);
            log.debug("Got saved \nperson profile {}\n for input {}", profile, input);
            if (profile.isPresent()) {
                context.setResult(profile);
                if (isCreateProfile) {
                    context.setUndoParameter(profile.get().getId());
                }
            } else {
                context.setState(Context.State.FAIL);
            }
        } catch (Exception e) {
            log.error("Cannot save the profile {}", parameter, e);
            context.failed(e);
        }
    }

    /**
     * To rollback command's execution (update person's profile)
     *
     * @param context context of redo execution
     * @see Context
     * @see Context#getUndoParameter()
     */
    @Override
    public void undo(Context<Optional<PersonProfile>> context) {
        if (isWrongUndoStateOf(context)) {
            log.warn("Cannot do undo of command {} with context:state '{}'", getId(), context.getState());
            context.setState(Context.State.FAIL);
            return;
        }
        final Object parameter = context.getUndoParameter();
        log.debug("Trying to delete person profile {}", parameter);
        context.setState(Context.State.WORK);
        try {
            if (parameter instanceof Long id) {
                persistenceFacade.deleteProfileById(id);
                log.debug("Got deleted \nperson profile ID:{}\n success: {}", id, true);
            } else if (parameter instanceof PersonProfile profile) {
                persistenceFacade.saveProfile(profile);
                log.debug("Got restored \nperson profile {}\n success: {}", profile, true);
            } else {
                throw new NullPointerException("Failed undo change profile parameter :" + parameter);
            }
            context.setState(Context.State.UNDONE);
        } catch (Exception e) {
            log.error("Cannot undo profile change {}", parameter, e);
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
        return ProfileCommands.CREATE_OR_UPDATE.id();
    }

    private static boolean isWrongRedoStateOf(Context<Optional<PersonProfile>> context) {
        return context.getState() != Context.State.READY;
    }

    private static boolean isWrongUndoStateOf(Context<Optional<PersonProfile>> context) {
        return context.getState() != Context.State.DONE;
    }
}
