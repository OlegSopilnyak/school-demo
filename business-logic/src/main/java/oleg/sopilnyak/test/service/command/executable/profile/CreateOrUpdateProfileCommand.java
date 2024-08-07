package oleg.sopilnyak.test.service.command.executable.profile;

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
 * Command-Base-Implementation: command to update person profile instance
 *
 * @param <E> command's working Entity type
 * @see PersonProfile
 * @see ProfileCommand
 * @see ProfilePersistenceFacade
 * @see SchoolCommandCache
 */
public abstract class CreateOrUpdateProfileCommand<E extends PersonProfile>
        extends SchoolCommandCache<E>
        implements ProfileCommand {
    protected final ProfilePersistenceFacade persistence;
    protected final BusinessMessagePayloadMapper payloadMapper;

    protected CreateOrUpdateProfileCommand(final Class<E> entityType,
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
     * To update person's profile<BR/>
     * To execute command redo with correct context state
     *
     * @param context context of redo execution
     * @see Context
     * @see Context#getRedoParameter()
     * @see Context.State#WORK
     * @see SchoolCommandCache#retrieveEntity(Long, LongFunction, UnaryOperator, Supplier)
     * @see SchoolCommandCache#persistRedoEntity(Context, Function)
     * @see SchoolCommandCache#rollbackCachedEntity(Context, Function)
     * @see CreateOrUpdateProfileCommand#functionFindById()
     * @see CreateOrUpdateProfileCommand#functionAdoptEntity()
     * @see CreateOrUpdateProfileCommand#functionSave()
     * @see NotExistProfileException
     */
    @Override
    public <T> void executeDo(Context<T> context) {
        final Object parameter = context.getRedoParameter();
        try {
            check(parameter);
            final E profile = commandParameter(parameter);
            getLog().debug("Trying to change profile using: {}", profile);
            final Long id = profile.getId();
            final boolean isCreateEntity = PersistenceFacadeUtilities.isInvalidId(id);
            if (!isCreateEntity) {
                // previous version of profile is storing to context for further rollback (undo)
                final var entity = retrieveEntity(id, functionFindById(), functionAdoptEntity(),
                        () -> new NotExistProfileException(PROFILE_WITH_ID_PREFIX + id + " is not exists.")
                );
                context.setUndoParameter(entity);
            }
            // persisting entity trough persistence layer
            final Optional<E> persisted = persistRedoEntity(context, functionSave());
            // checking command context state after entity persistence
            afterEntityPersistenceCheck(context,
                    () -> rollbackCachedEntity(context, functionSave()),
                    persisted.orElse(null), isCreateEntity);
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
     * @see SchoolCommandCache#rollbackCachedEntity(Context, Function)
     * @see ProfilePersistenceFacade#deleteProfileById(Long)
     * @see this#functionSave()
     */
    @Override
    public <T> void executeUndo(Context<T> context) {
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
