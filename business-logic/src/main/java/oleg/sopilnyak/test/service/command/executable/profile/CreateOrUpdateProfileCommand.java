package oleg.sopilnyak.test.service.command.executable.profile;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import oleg.sopilnyak.test.school.common.exception.ProfileNotExistsException;
import oleg.sopilnyak.test.school.common.facade.peristence.ProfilePersistenceFacade;
import oleg.sopilnyak.test.school.common.model.PersonProfile;
import oleg.sopilnyak.test.school.common.model.PrincipalProfile;
import oleg.sopilnyak.test.school.common.model.StudentProfile;
import oleg.sopilnyak.test.service.command.executable.sys.CommandResult;
import oleg.sopilnyak.test.service.command.type.ProfileCommand;
import oleg.sopilnyak.test.service.command.type.base.Context;
import org.slf4j.Logger;

import java.util.Optional;

import static oleg.sopilnyak.test.school.common.facade.PersonProfileFacade.isInvalidId;

/**
 * Command-Implementation: command to update person profile instance
 */
@Slf4j
@Getter
@AllArgsConstructor
public class CreateOrUpdateProfileCommand implements ProfileCommand<Optional<? extends PersonProfile>> {
    private final ProfilePersistenceFacade persistenceFacade;

    @Override
    public Logger getLog() {
        return log;
    }

    /**
     * To update person's profile
     *
     * @param parameter system principal-profile instance
     * @return execution's result
     * @see Optional
     * @see PrincipalProfile
     */
    @Override
    public CommandResult<Optional<? extends PersonProfile>> execute(Object parameter) {
        try {
            log.debug("Trying to update person profile {}", parameter);
            final PersonProfile input = commandParameter(parameter);
            final Optional<PersonProfile> profile = persistenceFacade.saveProfile(input);
            log.debug("Got saved \nperson profile {}\n for input {}", profile, input);
            return CommandResult.<Optional<? extends PersonProfile>>builder()
                    .result(Optional.of(profile))
                    .success(true)
                    .build();
        } catch (Exception e) {
            log.error("Cannot save the profile {}", parameter, e);
            return CommandResult.<Optional<? extends PersonProfile>>builder()
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
    public void doRedo(Context<Optional<? extends PersonProfile>> context) {
        final Object parameter = context.getDoParameter();
        try {
            log.debug("Trying to change person profile using: {}", parameter.toString());
            final Long inputId = ((PersonProfile) parameter).getId();
            final boolean isCreateProfile = isInvalidId(inputId);
            if (!isCreateProfile) {
                cacheProfileForRollback(context, inputId);
            }
            final Optional<? extends PersonProfile> profile = savePersonProfile(context);
            // checking execution context state
            if (context.getState() == Context.State.FAIL) {
                // there was a fail during save person profile
                log.error("Cannot save person profile {}", parameter);
                rollbackCachedProfile(context);
            } else {
                // save person profile operation is done successfully
                log.debug("Got saved \nperson profile {}\n for input {}", profile, parameter);
                context.setResult(profile);

                if (profile.isPresent() && isCreateProfile) {
                    // saving created profile.id for undo operation
                    context.setUndoParameter(profile.get().getId());
                }
            }
        } catch (Exception e) {
            log.error("Cannot save the profile {}", parameter, e);
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
    public void doUndo(Context<Optional<? extends PersonProfile>> context) {
        final Object parameter = context.getUndoParameter();
        try {
            log.debug("Trying to undo person profile changes using: {}", parameter.toString());
            if (parameter instanceof Long id) {
                persistenceFacade.deleteProfileById(id);
                log.debug("Got deleted \nperson profile ID:{}\n success: {}", id, true);
            } else if (parameter instanceof PersonProfile profile) {
                persistenceFacade.saveProfile(profile);
                log.debug("Got restored \nperson profile {}\n success: {}", profile, true);
            } else {
                throw new NullPointerException("Wrong undo parameter :" + parameter);
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
        return ProfileCommand.CREATE_OR_UPDATE_COMMAND_ID;
    }

    // private methods
    private static void redoExecutionFailed(final String input, Context<Optional<? extends PersonProfile>> context) {
        final Exception saveError = new ProfileNotExistsException(input);
        saveError.fillInStackTrace();
        context.failed(saveError);
    }

    private Optional<? extends PersonProfile> savePersonProfile(Context<Optional<? extends PersonProfile>> context) {
        final Object input = context.getDoParameter();
        if (input instanceof PrincipalProfile principalProfile) {
            return persistenceFacade.save(principalProfile);
        } else if (input instanceof StudentProfile studentProfile) {
            return persistenceFacade.save(studentProfile);
        } else {
            redoExecutionFailed("Wrong type of person profile :" + input.getClass().getName(), context);
            return Optional.empty();
        }
    }
}
