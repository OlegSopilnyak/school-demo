package oleg.sopilnyak.test.service.command.executable.profile;

import lombok.AllArgsConstructor;
import lombok.Getter;
import oleg.sopilnyak.test.school.common.exception.NotExistProfileException;
import oleg.sopilnyak.test.school.common.persistence.ProfilePersistenceFacade;
import oleg.sopilnyak.test.school.common.model.base.PersonProfile;
import oleg.sopilnyak.test.service.command.executable.sys.CommandResult;
import oleg.sopilnyak.test.service.command.type.base.command.ProfileCommand;
import oleg.sopilnyak.test.service.command.type.base.Context;

import java.util.Optional;

import static oleg.sopilnyak.test.school.common.business.PersonProfileFacade.isInvalidId;

/**
 * Command-Implementation: command to delete person profile instance by id
 */
@Getter
@AllArgsConstructor
public abstract class DeleteProfileCommand<T> implements ProfileCommand<T> {
    private final ProfilePersistenceFacade persistenceFacade;

    /**
     * To delete person's profile by id
     *
     * @param parameter system-id of person-profile to delete
     * @return execution's result
     * @see PersonProfile
     * @deprecated commands are going to work through redo/undo
     */
    @Deprecated(forRemoval = true)
    @Override
    public CommandResult<T> execute(Object parameter) {
        try {
            getLog().debug("Trying to delete person profile {}", parameter);
            final Long id = commandParameter(parameter);
            final Optional<PersonProfile> profile = persistenceFacade.findProfileById(id);
            if (profile.isEmpty()) {
                getLog().debug("Person profile with ID:{} is not exists.", id);
                return CommandResult.<T>builder().result(Optional.of((T)Boolean.FALSE))
                        .exception(new NotExistProfileException("Profile with ID:" + id + " is not exists."))
                        .success(false).build();
            }
            persistenceFacade.deleteProfileById(id);
            getLog().debug("Person profile with ID:{} is deleted '{}'", id, true);
            return CommandResult.<T>builder()
                    .result(Optional.of((T)Boolean.TRUE))
                    .success(true)
                    .build();
        } catch (Exception e) {
            getLog().error("Cannot delete the person profile {}", parameter, e);
            return CommandResult.<T>builder()
                    .result(Optional.of((T)Boolean.FALSE))
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
            getLog().debug("Trying to delete person profile using: {}", parameter.toString());
            final Long id = commandParameter(parameter);
            if (isInvalidId(id)) {
                throw new NotExistProfileException("PersonProfile with ID:" + id + " is not exists.");
            }
            cacheProfileForRollback(context, id);
            persistenceFacade.deleteProfileById(id);
            context.setResult(true);
            getLog().debug("Deleted person profile with ID: {}", id);
        } catch (Exception e) {
            context.setResult(false);
            getLog().error("Cannot delete profile with :{}", parameter, e);
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
    public void executeUndo(Context<?> context) {
        final Object parameter = context.getUndoParameter();
        try {
            getLog().debug("Trying to undo person profile deletion using: {}", parameter.toString());
            if (parameter instanceof PersonProfile profile) {
                persistenceFacade.saveProfile(profile);
                getLog().debug("Got restored person profile {}", profile);
            } else {
                throw new NullPointerException("Wrong undo parameter :" + parameter);
            }
            context.setState(Context.State.UNDONE);
        } catch (Exception e) {
            getLog().error("Cannot undo profile deletion {}", parameter, e);
            context.failed(e);
        }
    }
}
