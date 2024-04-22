package oleg.sopilnyak.test.service.command.executable.profile;

import lombok.AllArgsConstructor;
import lombok.Getter;
import oleg.sopilnyak.test.school.common.exception.NotExistProfileException;
import oleg.sopilnyak.test.school.common.persistence.ProfilePersistenceFacade;
import oleg.sopilnyak.test.school.common.model.base.PersonProfile;
import oleg.sopilnyak.test.school.common.model.PrincipalProfile;
import oleg.sopilnyak.test.school.common.model.StudentProfile;
import oleg.sopilnyak.test.service.command.executable.sys.CommandResult;
import oleg.sopilnyak.test.service.command.type.base.command.ProfileCommand;
import oleg.sopilnyak.test.service.command.type.base.Context;

import java.util.Optional;

import static oleg.sopilnyak.test.school.common.business.PersonProfileFacade.isInvalidId;


/**
 * Command-Implementation: command to update person profile instance
 */
@Getter
@AllArgsConstructor
public abstract class CreateOrUpdateProfileCommand<T> implements ProfileCommand<T> {
    private final ProfilePersistenceFacade persistenceFacade;

    /**
     * To update person's profile
     *
     * @param parameter system principal-profile instance
     * @return execution's result
     * @see Optional
     * @see PrincipalProfile
     * @deprecated commands are going to work through redo/undo
     */
    @Deprecated(forRemoval = true)
    @Override
    public CommandResult<T> execute(Object parameter) {
        try {
            getLog().debug("Trying to update person profile {}", parameter);
            final PersonProfile input = commandParameter(parameter);
            final Optional<PersonProfile> profile = persistenceFacade.saveProfile(input);
            getLog().debug("Got saved \nperson profile {}\n for input {}", profile, input);
            return CommandResult.<T>builder()
                    .result(Optional.of((T)profile))
                    .success(true)
                    .build();
        } catch (Exception e) {
            getLog().error("Cannot save the profile {}", parameter, e);
            return CommandResult.<T>builder()
                    .result((Optional<T>) Optional.of(Optional.empty()))
                    .exception(e).success(false).build();
        }
    }

    /**
     * To update person's profile<BR/>
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
            getLog().debug("Trying to change person profile using: {}", parameter.toString());
            final Long inputId = ((PersonProfile) parameter).getId();
            final boolean isCreateProfile = isInvalidId(inputId);
            if (!isCreateProfile) {
                cacheProfileForRollback(context, inputId);
            }
            final Optional<? extends PersonProfile> profile = savePersonProfile(context);
            // checking execution context state
            if (context.isFailed()) {
                // there was a fail during save person profile
                getLog().error("Cannot save person profile {}", parameter);
                rollbackCachedProfile(context);
            } else {
                // save person profile operation is done successfully
                getLog().debug("Got saved \nperson profile {}\n for input {}", profile, parameter);
                context.setResult(profile);

                if (profile.isPresent() && isCreateProfile) {
                    // saving created profile.id for undo operation
                    context.setUndoParameter(profile.get().getId());
                }
            }
        } catch (Exception e) {
            getLog().error("Cannot save the profile {}", parameter, e);
            context.failed(e);
            rollbackCachedProfile(context);
        }
    }

    /**
     * To rollback update person's profile<BR/>
     * To rollback command's execution with correct context state
     *
     * @param context context of redo execution
     * @see Context
     * @see Context#getUndoParameter()
     */
    @Override
    public void executeUndo(Context<?> context) {
        final Object parameter = context.getUndoParameter();
        try {
            getLog().debug("Trying to undo person profile changes using: {}", parameter.toString());
            if (parameter instanceof Long id) {
                persistenceFacade.deleteProfileById(id);
                getLog().debug("Got deleted \nperson profile ID:{}\n success: {}", id, true);
            } else if (parameter instanceof PersonProfile profile) {
                persistenceFacade.saveProfile(profile);
                getLog().debug("Got restored \nperson profile {}\n success: {}", profile, true);
            } else {
                throw new NullPointerException("Wrong undo parameter :" + parameter);
            }
            context.setState(Context.State.UNDONE);
        } catch (Exception e) {
            getLog().error("Cannot undo profile change {}", parameter, e);
            context.failed(e);
        }
    }

    // private methods
    private static void redoExecutionFailed(final String input, Context<?> context) {
        final Exception saveError = new NotExistProfileException(input);
        saveError.fillInStackTrace();
        context.failed(saveError);
    }

    private Optional<? extends PersonProfile> savePersonProfile(Context<?> context) {
        final Object input = context.getRedoParameter();
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
