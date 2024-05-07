package oleg.sopilnyak.test.service.command.executable.profile;

import lombok.Getter;
import oleg.sopilnyak.test.school.common.exception.NotExistProfileException;
import oleg.sopilnyak.test.school.common.model.base.PersonProfile;
import oleg.sopilnyak.test.school.common.persistence.ProfilePersistenceFacade;
import oleg.sopilnyak.test.school.common.persistence.utility.PersistenceFacadeUtilities;
import oleg.sopilnyak.test.service.command.executable.sys.CommandResult;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.command.type.profile.base.ProfileCommand;
import oleg.sopilnyak.test.service.command.type.base.command.SchoolCommandCache;

import java.util.Optional;
import java.util.function.*;

/**
 * Command-Base-Implementation: command to delete person profile instance by id
 *
 * @see PersonProfile
 * @see ProfileCommand
 * @see ProfilePersistenceFacade
 * @see SchoolCommandCache
 */
@Getter
public abstract class DeleteProfileCommand<E extends PersonProfile>
        extends SchoolCommandCache<E>
        implements ProfileCommand {

    protected final ProfilePersistenceFacade persistence;

    protected DeleteProfileCommand(Class<E> entityType, ProfilePersistenceFacade persistence) {
        super(entityType);
        this.persistence = persistence;
    }

    /**
     * to get function to find entity by id
     *
     * @return function implementation
     */
    protected abstract LongFunction<Optional<E>> functionFindById();

    /**
     * to get function to copy the entity
     *
     * @return function implementation
     */
    protected abstract UnaryOperator<E> functionCopyEntity();

    /**
     * to get function to persist the entity
     *
     * @return function implementation
     */
    protected abstract Function<E, Optional<E>> functionSave();

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
    public CommandResult<Boolean> execute(Object parameter) {
        try {
            getLog().debug("Trying to delete person profile {}", parameter);
            final Long id = commandParameter(parameter);
            final Optional<E> profile = functionFindById().apply(id);
            if (profile.isEmpty()) {
                getLog().debug("Person profile with ID:{} is not exists.", id);
                return CommandResult.<Boolean>builder().result(Optional.of(Boolean.FALSE))
                        .exception(new NotExistProfileException("Profile with ID:" + id + " is not exists."))
                        .success(false).build();
            }
            persistence.deleteProfileById(id);
            getLog().debug("Person profile with ID:{} is deleted '{}'", id, true);
            return CommandResult.<Boolean>builder()
                    .result(Optional.of(Boolean.TRUE))
                    .success(true)
                    .build();
        } catch (Exception e) {
            getLog().error("Cannot delete the person profile {}", parameter, e);
            return CommandResult.<Boolean>builder()
                    .result(Optional.of(Boolean.FALSE))
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
            check(parameter);
            getLog().debug("Trying to delete profile using: {}", parameter);
            final Long id = commandParameter(parameter);
            final var notFoundException = new NotExistProfileException(PROFILE_WITH_ID_PREFIX + id + " is not exists.");
            if (PersistenceFacadeUtilities.isInvalidId(id)) {
                throw notFoundException;
            }
            // previous profile is storing to context for further rollback (undo)
            final E previous = retrieveEntity(id, functionFindById(), functionCopyEntity(), () -> notFoundException);
            context.setUndoParameter(previous);
            persistence.deleteProfileById(id);
            context.setResult(true);
            getLog().debug("Deleted person profile with ID: {}", id);
        } catch (Exception e) {
            getLog().error("Cannot delete profile with :{}", parameter, e);
            context.failed(e);
            rollbackCachedEntity(context, functionSave());
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
