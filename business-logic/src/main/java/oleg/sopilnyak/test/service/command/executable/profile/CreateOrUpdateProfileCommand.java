package oleg.sopilnyak.test.service.command.executable.profile;

import oleg.sopilnyak.test.school.common.exception.NotExistProfileException;
import oleg.sopilnyak.test.school.common.model.PrincipalProfile;
import oleg.sopilnyak.test.school.common.model.base.PersonProfile;
import oleg.sopilnyak.test.school.common.persistence.ProfilePersistenceFacade;
import oleg.sopilnyak.test.school.common.persistence.utility.PersistenceFacadeUtilities;
import oleg.sopilnyak.test.service.command.executable.sys.CommandResult;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.command.type.base.command.ProfileCommand;
import oleg.sopilnyak.test.service.command.type.base.command.SchoolCommandCache;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.LongFunction;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;


/**
 * Command-Implementation: command to update person profile instance
 */
public abstract class CreateOrUpdateProfileCommand<T, C extends PersonProfile>
        extends SchoolCommandCache<C>
        implements ProfileCommand<T> {

    protected final ProfilePersistenceFacade persistence;

    protected CreateOrUpdateProfileCommand(Class<C> entityType, ProfilePersistenceFacade persistence) {
        super(entityType);
        this.persistence = persistence;
    }

    protected abstract LongFunction<Optional<C>> functionFindById();

    protected abstract UnaryOperator<C> functionCopyEntity();

    protected abstract Function<C, Optional<C>> functionSave();

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
            final C input = commandParameter(parameter);
            final Optional<C> profile = functionSave().apply(input);
            getLog().debug("Got saved \nperson profile {}\n for input {}", profile, input);
            return CommandResult.<T>builder()
                    .result(Optional.of((T) profile))
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
     * @see Context#getRedoParameter()
     * @see Context.State#WORK
     * @see this#retrieveEntity(Long, LongFunction, UnaryOperator, Supplier)
     * @see this#persistRedoEntity(Context, Function)
     * @see this#rollbackCachedEntity(Context, Function)
     */
    @Override
    public void executeDo(Context<?> context) {
        final Object parameter = context.getRedoParameter();
        try {
            getLog().debug("Trying to change person profile using: {}", parameter);
            final Long inputId = ((C) parameter).getId();
            final boolean isCreateProfile = PersistenceFacadeUtilities.isInvalidId(inputId);
            if (!isCreateProfile) {
                // cached profile is storing to context for further rollback (undo)
                context.setUndoParameter(
                        retrieveEntity(inputId, functionFindById(), functionCopyEntity(),
                                () -> new NotExistProfileException(PROFILE_WITH_ID_PREFIX + inputId + " is not exists.")
                        )
                );
            }
            final Optional<C> profile = persistRedoEntity(context, functionSave());
            // checking execution context state
            if (context.isFailed()) {
                // there was a fail during save person profile
                getLog().error("Cannot save person profile {}", parameter);
                rollbackCachedEntity(context, functionSave());
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
            rollbackCachedEntity(context, functionSave());
        }
    }

    /**
     * To rollback update person's profile<BR/>
     * To rollback command's execution with correct context state
     *
     * @param context context of redo execution
     * @see Context
     * @see Context#getUndoParameter()
     * @see Context.State#UNDONE
     * @see this#rollbackCachedEntity(Context, Function, LongFunction, Supplier)
     * @see ProfilePersistenceFacade#deleteProfileById(Long)
     */
    @Override
    public void executeUndo(Context<?> context) {
        final Object parameter = context.getUndoParameter();
        try {
            getLog().debug("Trying to undo person profile changes using: {}", parameter);

            rollbackCachedEntity(context, functionSave(), persistence::deleteProfileById,
                    () -> new NotExistProfileException("Wrong undo parameter :" + parameter));
            context.setState(Context.State.UNDONE);
        } catch (Exception e) {
            getLog().error("Cannot undo profile change {}", parameter, e);
            context.failed(e);
        }
    }
}
