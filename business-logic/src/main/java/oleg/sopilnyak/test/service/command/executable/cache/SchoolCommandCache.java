package oleg.sopilnyak.test.service.command.executable.cache;

import oleg.sopilnyak.test.school.common.exception.EntityNotFoundException;
import oleg.sopilnyak.test.school.common.exception.core.InvalidParameterTypeException;
import oleg.sopilnyak.test.school.common.exception.education.StudentNotFoundException;
import oleg.sopilnyak.test.school.common.model.BaseType;
import oleg.sopilnyak.test.school.common.persistence.education.StudentsPersistenceFacade;
import oleg.sopilnyak.test.service.command.executable.sys.CommandContext;
import oleg.sopilnyak.test.service.command.io.Input;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.mapper.BusinessMessagePayloadMapper;
import oleg.sopilnyak.test.service.message.payload.BasePayload;
import org.slf4j.Logger;

import java.util.Optional;
import java.util.function.*;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

/**
 * Cache for the delete or update commands
 */
public abstract class SchoolCommandCache<T extends BaseType> {
    private final Class<T> entityType;
    private final String entityName;

    protected SchoolCommandCache(final Class<T> entityType) {
        this.entityType = entityType;
        this.entityName = entityType.getSimpleName();
    }

    /**
     * To get reference to command's logger
     *
     * @return reference to the logger
     */
    public abstract Logger getLog();

    /**
     * To cache into context old value of the student instance for possible rollback
     *
     * @param inputId           system-id of the student
     * @param findEntityById    function for find entity by id
     * @param adoptEntity       function for transform entity to payload
     * @param exceptionSupplier function-source of entity-not-found exception
     * @return copy of exists entity
     * @throws StudentNotFoundException if student is not exist
     * @see StudentsPersistenceFacade
     * @see StudentsPersistenceFacade#findStudentById(Long)
     * @see Context
     * @see CommandContext#setUndoParameter(Input)
     * @see BusinessMessagePayloadMapper#toPayload(BaseType)
     */
    protected T retrieveEntity(final Long inputId,
                               final LongFunction<Optional<T>> findEntityById,
                               final UnaryOperator<T> adoptEntity,
                               final Supplier<? extends EntityNotFoundException> exceptionSupplier) {

        getLog().debug("Getting entity of {} for ID:{}", entityName, inputId);

        final T existsEntity = findEntityById.apply(inputId).orElseThrow(exceptionSupplier);

        getLog().debug("Got entity of {} '{}' by ID:{}", entityName, existsEntity, inputId);

        // return copy of exists entity for undo operation
        getLog().debug("Adopting Entity to Payload '{}'", existsEntity);
        return adoptEntity.apply(existsEntity);
    }

    /**
     * To restore in database the entity from cache(context)
     *
     * @param context    command execution context
     * @param facadeSave function for saving the undo entity
     * @return saved entity
     * @see Context
     * @see Function#apply(Object)
     * @see LongFunction#apply(long)
     * @see Supplier#get()
     * @see this#rollbackCachedEntity(Context, Function, LongConsumer)
     */
    protected Optional<T> rollbackCachedEntity(final Context<?> context, final Function<T, Optional<T>> facadeSave) {
        return rollbackCachedEntity(context, facadeSave, null);
    }

    /**
     * To restore in database the entity from cache(context)
     *
     * @param context          command execution context
     * @param facadeSave       function for saving the undo entity
     * @param facadeDeleteById function for delete created entity
     * @return restored in the database cached entity
     * @see Context#getUndoParameter()
     * @see Function#apply(Object)
     * @see LongFunction#apply(long)
     * @see Supplier#get()
     * @see SchoolCommandCache#rollbackCachedEntity(Context, Function)
     */
    protected Optional<T> rollbackCachedEntity(final Context<?> context,
                                               final Function<T, Optional<T>> facadeSave,
                                               final LongConsumer facadeDeleteById) {
        // process the value of input parameter
        final Object parameter = inputParameter(context.getUndoParameter());
        // rollback updating exists entity
        if (entityType.isAssignableFrom(parameter.getClass())) {
            // save entity from parameter
            getLog().debug("Restoring changed value of {}\n'{}'", entityName, parameter);
            return facadeSave.apply(entityType.cast(parameter));
        } else if (isNull(facadeDeleteById)) {
            // delete function isn't passed to the method
            throw new InvalidParameterTypeException(entityName, parameter);
        }
        // rollback deleting new entity
        getLog().info("Deleting created value of {} with ID:{}", entityName, parameter);
        if (parameter instanceof Long id) {
            // remove created entity by id from the parameter
            facadeDeleteById.accept(id);
            final Input<?> doInput = context.getRedoParameter();
            if(isNull(doInput) || doInput.isEmpty()) {
                // command do input is empty
                getLog().debug("No input entity to clean entity-id.");
                return Optional.empty();
            }
            // cleaning input entity-id and do command result
            if (doInput.value() instanceof BasePayload payload) {
                // clear ID for further CREATE entity
                payload.setId(null);
                // clear the do command result after entity deleting
                context.setResult(null);

                getLog().debug("Got deleted {} with ID:{} successfully", entityName, id);
                return Optional.empty();
            } else {
                throw new InvalidParameterTypeException(entityName, doInput);
            }
        } else {
            // wrong type of input parameter
            getLog().info("Cannot delete {} with ID:{} because of wrong parameter type", entityName, parameter);
            throw new InvalidParameterTypeException("Long", parameter);
        }
    }

    /**
     * To restore initial data state of the command before command's do
     *
     * @param context    command's do context
     * @param facadeSave function for saving the entity
     * @param <E>        type of command's do result
     * @see Context#getUndoParameter()
     * @see SchoolCommandCache#rollbackCachedEntity(Context, Function)
     */
    protected <E> void restoreInitialCommandState(final Context<E> context,
                                                  final Function<T, Optional<T>> facadeSave) {
        if (nonNull(context.getUndoParameter())) {
            getLog().debug("Restoring state of command '{}' after fail using: {}", context.getCommand().getId(), context.getUndoParameter());
            rollbackCachedEntity(context, facadeSave);
        }
    }

    /**
     * To persist entity
     *
     * @param context    command's do context
     * @param facadeSave function for saving the entity
     * @return saved instance or empty
     * @see Optional#empty()
     * @see StudentNotFoundException
     */
    protected Optional<T> persistRedoEntity(final Context<?> context, final Function<T, Optional<T>> facadeSave) {
        // process the value of input parameter
        final Object parameter = inputParameter(context.getRedoParameter());
        if (entityType.isAssignableFrom(parameter.getClass())) {
            getLog().debug("Storing changed value of {} '{}'", entityName, parameter);
            final T entity = entityType.cast(parameter);
            return facadeSave.apply(entity);
        }
        getLog().warn("Invalid redo parameter type (expected '{}' for [{}])", entityName, parameter);
        throw new InvalidParameterTypeException(entityName, parameter);
    }

    /**
     * To do checking after persistence of the entity
     *
     * @param context             command's do context
     * @param rollbackProcess     process to run after persistence fail
     * @param persistedEntityCopy persisted entity instance copy
     * @param isCreateEntityMode  if true there was new entity creation action
     * @see Context
     * @see Context#getRedoParameter()
     * @see Context#setResult(Object)
     * @see CommandContext#setUndoParameter(Input)
     * @see Optional
     * @see Runnable#run()
     */
    protected void afterEntityPersistenceCheck(final Context<Optional<T>> context,
                                               final Runnable rollbackProcess,
                                               final T persistedEntityCopy,
                                               final boolean isCreateEntityMode) {
        // checking execution context state
        if (context.isFailed()) {
            // there was a fail during store entity operation
            getLog().error("Couldn't save entity of '{}' value: {}", entityName, context.getRedoParameter());
            rollbackProcess.run();
        } else {
            // store entity operation if it is done successfully
            getLog().debug(
                    "Got stored entity of '{}' value {}\nfrom parameter {}",
                    entityName, persistedEntityCopy, context.getRedoParameter()
            );

            context.setResult(Optional.ofNullable(persistedEntityCopy));

            if (nonNull(persistedEntityCopy) && isCreateEntityMode) {
                // storing created entity.id for undo operation
                if (context instanceof CommandContext<Optional<T>> commandContext) {
                    commandContext.setUndoParameter(Input.of(persistedEntityCopy.getId()));
                }
            }
        }
    }

    /**
     * Setup context's undo parameter after entity removing
     *
     * @param context           command's do context
     * @param undoEntity            removed from database entity
     * @param exceptionSupplier function-source of entity-not-found exception
     * @param <E>               type of command result
     * @see BasePayload
     * @see BasePayload#setId(Long)
     * @see CommandContext#setUndoParameter(Input)
     * @see Supplier#get()
     */
    protected <E> void prepareDeleteEntityUndo(final Context<E> context, final T undoEntity,
                                               final Supplier<? extends EntityNotFoundException> exceptionSupplier) {
        // clear id of the deleted entity
        if (undoEntity instanceof BasePayload<?> undo) {
            // prepare entity for further creation (clear ID)
            undo.setId(null);
        } else {
            // wrong type of the undo entity
            throw exceptionSupplier.get();
        }
        // cached profile is storing to context for further rollback (undo)
        if (context instanceof CommandContext<E> commandContext) {
            commandContext.setUndoParameter(Input.of(undoEntity));
        }
    }

    // private methods
    private <T> T inputParameter(final Input<T> parameter) {
        if (isNull(parameter) || parameter.isEmpty()) {
            throw new InvalidParameterTypeException(entityName, parameter);
        } else {
            return parameter.value();
        }
    }
}
