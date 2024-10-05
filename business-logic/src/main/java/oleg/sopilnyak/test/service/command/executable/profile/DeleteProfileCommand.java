package oleg.sopilnyak.test.service.command.executable.profile;

import lombok.Getter;
import oleg.sopilnyak.test.school.common.exception.NotExistProfileException;
import oleg.sopilnyak.test.school.common.model.base.PersonProfile;
import oleg.sopilnyak.test.school.common.persistence.ProfilePersistenceFacade;
import oleg.sopilnyak.test.school.common.persistence.utility.PersistenceFacadeUtilities;
import oleg.sopilnyak.test.service.command.executable.cache.SchoolCommandCache;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.command.type.profile.base.ProfileCommand;
import oleg.sopilnyak.test.service.mapper.BusinessMessagePayloadMapper;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.LongFunction;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

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
    protected final BusinessMessagePayloadMapper payloadMapper;

    protected DeleteProfileCommand(final Class<E> entityType,
                                   final ProfilePersistenceFacade persistence,
                                   final BusinessMessagePayloadMapper payloadMapper) {
        super(entityType);
        this.persistence = persistence;
        this.payloadMapper = payloadMapper;
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
    protected abstract UnaryOperator<E> functionAdoptEntity();

    /**
     * to get function to persist the entity
     *
     * @return function implementation
     */
    protected abstract Function<E, Optional<E>> functionSave();

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
     * @see DeleteProfileCommand#functionFindById()
     * @see DeleteProfileCommand#functionAdoptEntity()
     * @see DeleteProfileCommand#functionSave()
     * @see NotExistProfileException
     */
    @Override
    public <T> void executeDo(Context<T> context) {
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
            final var entity = retrieveEntity(
                    id, functionFindById(), functionAdoptEntity(), () -> notFoundException
            );
            // removing profile instance by ID from the database
            persistence.deleteProfileById(id);
            // cached profile is storing to context for further rollback (undo)
            context.setUndoParameter(entity);
            context.setResult(true);
            getLog().debug("Deleted person profile with ID: {}", id);
        } catch (Exception e) {
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
     * @see DeleteProfileCommand#functionSave()
     */
    @Override
    public <T> void executeUndo(Context<T> context) {
        final Object parameter = context.getUndoParameter();
        try {
            check(parameter);
            getLog().debug("Trying to undo person's profile deletion using: {}", parameter);
            final E entity = rollbackCachedEntity(context, functionSave()).orElseThrow();

            getLog().debug("Updated in database: '{}'", entity);
            // change profile-id value for further do command action
            context.setRedoParameter(entity.getId());
            context.setState(Context.State.UNDONE);
        } catch (Exception e) {
            getLog().error("Cannot undo profile deletion {}", parameter, e);
            context.failed(e);
        }
    }
}
