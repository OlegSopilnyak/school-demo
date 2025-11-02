package oleg.sopilnyak.test.service.command.executable.profile;

import lombok.Getter;
import oleg.sopilnyak.test.school.common.exception.EntityNotFoundException;
import oleg.sopilnyak.test.school.common.exception.profile.ProfileNotFoundException;
import oleg.sopilnyak.test.school.common.model.PersonProfile;
import oleg.sopilnyak.test.school.common.persistence.profile.ProfilePersistenceFacade;
import oleg.sopilnyak.test.school.common.persistence.utility.PersistenceFacadeUtilities;
import oleg.sopilnyak.test.service.command.executable.sys.cache.SchoolCommandCache;
import oleg.sopilnyak.test.service.command.executable.sys.CommandContext;
import oleg.sopilnyak.test.service.command.io.Input;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.command.type.profile.base.ProfileCommand;
import oleg.sopilnyak.test.service.mapper.BusinessMessagePayloadMapper;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

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
public abstract class DeleteProfileCommand<E extends PersonProfile> extends SchoolCommandCache<E>
        implements ProfileCommand<Boolean> {
    protected final transient ProfilePersistenceFacade persistence;
    protected final transient BusinessMessagePayloadMapper payloadMapper;

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
     * @see ProfileNotFoundException
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void executeDo(Context<Boolean> context) {
        final Input<Long> parameter = context.getRedoParameter();
        try {
            checkNullParameter(parameter);
            getLog().debug("Trying to delete profile using: {}", parameter.value());
            final Long id = parameter.value();
            if (PersistenceFacadeUtilities.isInvalidId(id)) {
                getLog().warn("Invalid id {}", id);
                throw exceptionFor(id);
            }
            // previous profile is storing to context for further rollback (undo)
            final E entity = retrieveEntity(id, functionFindById(), functionAdoptEntity(), () -> exceptionFor(id));
            // removing profile instance by ID from the database
            persistence.deleteProfileById(id);
            // setup undo parameter for deleted entity
            prepareDeleteEntityUndo(context, entity, () -> exceptionFor(id));
            // successful delete entity operation
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
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void executeUndo(Context<?> context) {
        final Input<?> parameter = context.getUndoParameter();
        try {
            checkNullParameter(parameter);
            getLog().debug("Trying to undo person's profile deletion using: {}", parameter);
            final E entity = rollbackCachedEntity(context, functionSave()).orElseThrow();

            getLog().debug("Updated in database: '{}'", entity);
            // change profile-id value for further do command action
            if (context instanceof CommandContext<?> commandContext) {
                commandContext.setRedoParameter(Input.of(entity.getId()));
            }
            context.setState(Context.State.UNDONE);
        } catch (Exception e) {
            getLog().error("Cannot undo profile deletion {}", parameter, e);
            context.failed(e);
        }
    }

    // private methods
    private EntityNotFoundException exceptionFor(final Long id) {
        return new ProfileNotFoundException(PROFILE_WITH_ID_PREFIX + id + " is not exists.");
    }
}
