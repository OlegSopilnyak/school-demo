package oleg.sopilnyak.test.service.command.executable.profile;

import lombok.Getter;
import oleg.sopilnyak.test.school.common.exception.NotExistProfileException;
import oleg.sopilnyak.test.school.common.model.base.PersonProfile;
import oleg.sopilnyak.test.school.common.persistence.ProfilePersistenceFacade;
import oleg.sopilnyak.test.school.common.persistence.utility.PersistenceFacadeUtilities;
import oleg.sopilnyak.test.service.command.executable.sys.CommandResult;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.command.type.base.command.ProfileCommand;
import oleg.sopilnyak.test.service.command.type.base.command.SchoolCommandCache;

import java.util.Optional;
import java.util.function.*;

/**
 * Command-Implementation: command to delete person profile instance by id
 *
 * @see PersonProfile
 * @see ProfileCommand
 * @see ProfilePersistenceFacade
 * @see SchoolCommandCache
 */
@Getter
public abstract class DeleteProfileCommand<T, C extends PersonProfile>
        extends SchoolCommandCache<C>
        implements ProfileCommand<T> {

    protected final ProfilePersistenceFacade persistence;

    protected DeleteProfileCommand(Class<C> entityType, ProfilePersistenceFacade persistence) {
        super(entityType);
        this.persistence = persistence;
    }

    protected abstract LongFunction<Optional<C>> functionFindById();

    protected abstract UnaryOperator<C> functionCopyEntity();

    protected abstract Function<C, Optional<C>> functionSave();

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
            final Optional<C> profile = functionFindById().apply(id);
            if (profile.isEmpty()) {
                getLog().debug("Person profile with ID:{} is not exists.", id);
                return CommandResult.<T>builder().result(Optional.of((T) Boolean.FALSE))
                        .exception(new NotExistProfileException("Profile with ID:" + id + " is not exists."))
                        .success(false).build();
            }
            persistence.deleteProfileById(id);
            getLog().debug("Person profile with ID:{} is deleted '{}'", id, true);
            return CommandResult.<T>builder()
                    .result(Optional.of((T) Boolean.TRUE))
                    .success(true)
                    .build();
        } catch (Exception e) {
            getLog().error("Cannot delete the person profile {}", parameter, e);
            return CommandResult.<T>builder()
                    .result(Optional.of((T) Boolean.FALSE))
                    .exception(e).success(false).build();
        }
    }

    /**
     * DO: To delete person's profile by id<BR/>
     * To execute command redo with correct context state
     *
     * @param context context of redo execution
     * @see Context
     * @see Context#getRedoParameter()
     * @see Context.State#WORK
     * @see SchoolCommandCache#retrieveEntity(Long, LongFunction, UnaryOperator, Supplier)
     * @see SchoolCommandCache#rollbackCachedEntity(Context, Function)
     * @see ProfilePersistenceFacade#deleteProfileById(Long)
     * @see this#functionFindById()
     * @see this#functionCopyEntity()
     * @see this#functionSave()
     * @see NotExistProfileException
     */
    @Override
    public void executeDo(Context<?> context) {
        final Object parameter = context.getRedoParameter();
        try {
            getLog().debug("Trying to delete person profile using: {}", parameter);
            final Long id = commandParameter(parameter);
            final var notFoundException = new NotExistProfileException(PROFILE_WITH_ID_PREFIX + id + " is not exists.");
            if (PersistenceFacadeUtilities.isInvalidId(id)) {
                throw notFoundException;
            }
            // cached profile is storing to context for further rollback (undo)
            context.setUndoParameter(
                    retrieveEntity(id, functionFindById(), functionCopyEntity(), () -> notFoundException)
            );
            persistence.deleteProfileById(id);
            context.setResult(true);
            getLog().debug("Deleted person profile with ID: {}", id);
        } catch (Exception e) {
            rollbackCachedEntity(context, functionSave());
            getLog().error("Cannot delete profile with :{}", parameter, e);
            context.failed(e);
        }
    }

    /**
     * UNDO: To delete person's profile by id<BR/>
     * To rollback command's execution with correct context state
     *
     * @param context context of redo execution
     * @see Context
     * @see Context#getUndoParameter()
     * @see Context.State#UNDONE
     * @see SchoolCommandCache#rollbackCachedEntity(Context, Function)
     * @see this#functionSave()
     */
    @Override
    public void executeUndo(Context<?> context) {
        final Object parameter = context.getUndoParameter();
        try {
            getLog().debug("Trying to undo person profile deletion using: {}", parameter);

            rollbackCachedEntity(context, functionSave());

            context.setState(Context.State.UNDONE);
        } catch (Exception e) {
            getLog().error("Cannot undo profile deletion {}", parameter, e);
            context.failed(e);
        }
    }
}
