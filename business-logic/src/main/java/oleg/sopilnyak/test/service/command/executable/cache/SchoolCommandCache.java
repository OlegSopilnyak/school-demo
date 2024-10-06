package oleg.sopilnyak.test.service.command.executable.cache;

import oleg.sopilnyak.test.school.common.exception.EntityNotExistException;
import oleg.sopilnyak.test.school.common.exception.NotExistStudentException;
import oleg.sopilnyak.test.school.common.model.base.BaseType;
import oleg.sopilnyak.test.school.common.persistence.students.courses.StudentsPersistenceFacade;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.exception.InvalidParameterTypeException;
import oleg.sopilnyak.test.service.mapper.BusinessMessagePayloadMapper;
import oleg.sopilnyak.test.service.message.BasePayload;
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
     * @throws NotExistStudentException if student is not exist
     * @see StudentsPersistenceFacade
     * @see StudentsPersistenceFacade#findStudentById(Long)
     * @see Context
     * @see Context#setUndoParameter(Object)
     * @see BusinessMessagePayloadMapper#toPayload(BaseType)
     */
    protected T retrieveEntity(final Long inputId,
                               final LongFunction<Optional<T>> findEntityById,
                               final UnaryOperator<T> adoptEntity,
                               final Supplier<? extends EntityNotExistException> exceptionSupplier) {

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
        final Object parameter = context.getUndoParameter();
        if (nonNull(parameter) && entityType.isAssignableFrom(parameter.getClass())) {
            getLog().debug("Restoring changed value of {}\n'{}'", entityName, parameter);
            return facadeSave.apply(context.getUndoParameter());
        } else if (isNull(facadeDeleteById)) {
            throw new InvalidParameterTypeException(entityName, parameter);
        }
        getLog().info("Deleting created value of {} with ID:{}", entityName, parameter);
        if (parameter instanceof Long id) {
            facadeDeleteById.accept(id);
            getLog().debug("Got deleted {} with ID:{} successfully", entityName, id);
            return Optional.empty();
        } else {
            getLog().info("Cannot delete {} with ID:{} because of wrong parameter type", entityName, parameter);
            throw new InvalidParameterTypeException("Long", parameter);
        }
    }

    /**
     * To persist entity
     *
     * @param context    command's do context
     * @param facadeSave function for saving the entity
     * @return saved instance or empty
     * @see Optional#empty()
     * @see NotExistStudentException
     */
    protected Optional<T> persistRedoEntity(final Context<?> context, final Function<T, Optional<T>> facadeSave) {
        final Object parameter = context.getRedoParameter();
        if (nonNull(parameter) && entityType.isAssignableFrom(parameter.getClass())) {
            getLog().debug("Storing changed value of {} '{}'", entityName, parameter);
            return facadeSave.apply(context.getRedoParameter());
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
     * @see Context#setUndoParameter(Object)
     * @see Optional
     * @see Runnable#run()
     */
    protected <E> void afterEntityPersistenceCheck(final Context<E> context,
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
                context.setUndoParameter(persistedEntityCopy.getId());
            }
        }
    }

    protected <E> void setupUndoParameter(final Context<E> context,
                                          final T entity,
                                          final Supplier<? extends EntityNotExistException> exceptionSupplier) {
        // clear id of the deleted entity
        if (entity instanceof BasePayload<?> payload) {
            payload.setId(null);
        } else {
            throw exceptionSupplier.get();
        }
        // cached profile is storing to context for further rollback (undo)
        context.setUndoParameter(entity);
    }
}
