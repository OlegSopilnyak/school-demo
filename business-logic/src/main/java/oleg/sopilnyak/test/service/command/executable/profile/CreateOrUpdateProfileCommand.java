package oleg.sopilnyak.test.service.command.executable.profile;

import lombok.Getter;
import oleg.sopilnyak.test.school.common.exception.profile.ProfileNotFoundException;
import oleg.sopilnyak.test.school.common.model.PersonProfile;
import oleg.sopilnyak.test.school.common.persistence.profile.ProfilePersistenceFacade;
import oleg.sopilnyak.test.school.common.persistence.utility.PersistenceFacadeUtilities;
import oleg.sopilnyak.test.service.command.executable.cache.SchoolCommandCache;
import oleg.sopilnyak.test.service.command.executable.sys.CommandContext;
import oleg.sopilnyak.test.service.command.io.Input;
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
@Getter
public abstract class CreateOrUpdateProfileCommand<E extends PersonProfile> extends SchoolCommandCache<E>
        implements ProfileCommand<Optional<E>> {
    protected final transient ProfilePersistenceFacade persistence;
    protected final transient BusinessMessagePayloadMapper payloadMapper;

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
     * @see SchoolCommandCache#restoreInitialCommandState(Context, Function)
     * @see CreateOrUpdateProfileCommand#functionFindById()
     * @see CreateOrUpdateProfileCommand#functionAdoptEntity()
     * @see CreateOrUpdateProfileCommand#functionSave()
     * @see ProfileNotFoundException
     */
    @Override
    public void executeDo(Context<Optional<E>> context) {
        final Input<?> parameter = context.getRedoParameter();
        try {
            checkNullParameter(parameter);
            final E profile = (E) parameter.value();
            final Long id = profile.getId();
            final boolean isCreateEntityMode = PersistenceFacadeUtilities.isInvalidId(id);
            if (!isCreateEntityMode) {
                getLog().debug("Trying to update profile using: {}", profile);
                // previous version of profile is storing to context for further rollback (undo)
                final PersonProfile entity = retrieveEntity(
                        id, functionFindById(), functionAdoptEntity(),
                        () -> new ProfileNotFoundException(PROFILE_WITH_ID_PREFIX + id + " is not exists.")
                );

                getLog().debug("Previous value of the entity stored for possible undo: {}", entity);
                if (context instanceof CommandContext<?> commandContext) {
                    commandContext.setUndoParameter(Input.of(entity));
                }
            } else {
                getLog().debug("Trying to create profile using: {}", profile);
            }
            // persisting entity trough persistence layer
            final Optional<E> persisted = persistRedoEntity(context, functionSave());
            // checking command context state after entity persistence
            afterEntityPersistenceCheck(
                    context, () -> rollbackCachedEntity(context, functionSave()),
                    persisted.orElse(null), isCreateEntityMode
            );
        } catch (Exception e) {
            getLog().error("Cannot save the profile {}", parameter, e);
            context.failed(e);
            restoreInitialCommandState(context, functionSave());
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
     * @see CreateOrUpdateProfileCommand#functionSave()
     */
    @Override
    public void executeUndo(Context<?> context) {
        final Input<?> parameter = context.getUndoParameter();
        try {
            checkNullParameter(parameter);
            getLog().debug("Trying to undo person profile changes using: {}", parameter);

            rollbackCachedEntity(context, functionSave(), persistence::deleteProfileById);

            context.setState(Context.State.UNDONE);
        } catch (Exception e) {
            getLog().error("Cannot undo profile change {}", parameter, e);
            context.failed(e);
        }
    }
}
