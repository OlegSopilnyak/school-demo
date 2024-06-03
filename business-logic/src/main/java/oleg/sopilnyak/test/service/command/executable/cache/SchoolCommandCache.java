package oleg.sopilnyak.test.service.command.executable.cache;

import oleg.sopilnyak.test.school.common.exception.EntityNotExistException;
import oleg.sopilnyak.test.school.common.exception.NotExistStudentException;
import oleg.sopilnyak.test.school.common.model.base.BaseType;
import oleg.sopilnyak.test.school.common.persistence.students.courses.StudentsPersistenceFacade;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.mapper.BusinessMessagePayloadMapper;
import org.slf4j.Logger;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.LongConsumer;
import java.util.function.LongFunction;
import java.util.function.Supplier;

import static java.util.Objects.nonNull;

/**
 * Cache for the delete or update commands
 */
public abstract class SchoolCommandCache<T extends BaseType> {
    private final Class<T> entityType;
    private final String entityName;
    private final BusinessMessagePayloadMapper payloadMapper;

    protected SchoolCommandCache(Class<T> entityType, BusinessMessagePayloadMapper payloadMapper) {
        this.entityType = entityType;
        this.entityName = entityType.getSimpleName();
        this.payloadMapper = payloadMapper;
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
     * @param findById          function for find entity by id
     * @param exceptionSupplier function-source of entity-not-found exception
     * @return copy of exists entity
     * @throws NotExistStudentException if student is not exist
     * @see StudentsPersistenceFacade
     * @see StudentsPersistenceFacade#findStudentById(Long)
     * @see Context
     * @see Context#setUndoParameter(Object)
     * @see BusinessMessagePayloadMapper#toPayload(BaseType)
     */
    protected T retrieveEntity(final Long inputId, final LongFunction<Optional<T>> findById,
                               final Supplier<? extends EntityNotExistException> exceptionSupplier) {

        getLog().info("Getting entity of {} for ID:{}", entityName, inputId);
        final T existsEntity = findById.apply(inputId).orElseThrow(exceptionSupplier);
        getLog().info("Got entity of {} '{}'", entityName, existsEntity);

        // return copy of exists entity for undo operation
        getLog().info("Copying the value of '{}'", existsEntity);
        return (T) payloadMapper.toPayload(existsEntity);
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
     * @see this#rollbackCachedEntity(Context, Function, LongConsumer, Supplier)
     */
    protected Optional<T> rollbackCachedEntity(final Context<?> context, final Function<T, Optional<T>> facadeSave) {
        return rollbackCachedEntity(context, facadeSave, null, null);
    }

    /**
     * To restore in database the entity from cache(context)
     *
     * @param context           command execution context
     * @param facadeSave        function for saving the undo entity
     * @param facadeDeleteById  function for delete created entity
     * @param exceptionSupplier function-source of entity-not-found exception
     * @return restored in the database cached entity
     * @see Context#getUndoParameter()
     * @see Function#apply(Object)
     * @see LongFunction#apply(long)
     * @see Supplier#get()
     * @see this#rollbackCachedEntity(Context, Function)
     */
    protected Optional<T> rollbackCachedEntity(final Context<?> context,
                                               final Function<T, Optional<T>> facadeSave,
                                               final LongConsumer facadeDeleteById,
                                               final Supplier<? extends EntityNotExistException> exceptionSupplier) {
        final Object parameter = context.getUndoParameter();
        if (nonNull(parameter) && entityType.isAssignableFrom(parameter.getClass())) {
            getLog().info("Restoring changed value of {}\n'{}'", entityName, parameter);
            return facadeSave.apply((T) parameter);
        } else if (nonNull(facadeDeleteById)) {
            getLog().info("Deleting created value of {} with ID:{}", entityName, parameter);
            if (parameter instanceof Long id) {
                facadeDeleteById.accept(id);
                getLog().info("Got deleted {} with ID:{} successfully", entityName, id);
            } else {
                getLog().info("Cannot delete {} with ID:{} because '{}'", entityName, parameter, exceptionSupplier.get().getMessage());
                throw exceptionSupplier.get();
            }
        }
        return Optional.empty();
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
            getLog().info("Storing changed value of {} '{}'", entityName, parameter);
            return facadeSave.apply((T) parameter);
        } else {
            if (nonNull(parameter)) {
                final String message = "Wrong type of " + entityName + ":" + parameter.getClass().getName();
                final Exception saveError = new EntityNotExistException(message);
                saveError.fillInStackTrace();
                getLog().error(message, saveError);
                context.failed(saveError);
            }
            return Optional.empty();
        }
    }

    /**
     * To do checking after persistence of the entity
     *
     * @param context             command's do context
     * @param rollbackProcess     process to run after persistence fail
     * @param persistedEntityCopy persisted entity instance
     * @param isCreateEntity      if true there was new entity creation action
     * @see Context
     * @see Context#getRedoParameter()
     * @see Context#setResult(Object)
     * @see Context#setUndoParameter(Object)
     * @see Optional
     * @see Runnable#run()
     */
    protected void afterPersistCheck(final Context<?> context,
                                     final Runnable rollbackProcess,
                                     final Optional<T> persistedEntityCopy,
                                     final boolean isCreateEntity) {
        // checking execution context state
        if (context.isFailed()) {
            // there was a fail during store entity
            getLog().error("Cannot save {} {}", entityName, context.getRedoParameter());
            rollbackProcess.run();
        } else {
            // store entity operation is done successfully
            getLog().info("Got stored {} {}\nfrom parameter {}", entityName, persistedEntityCopy, context.getRedoParameter());
            context.setResult(persistedEntityCopy);

            if (persistedEntityCopy.isPresent() && isCreateEntity) {
                // storing created entity.id for undo operation
                context.setUndoParameter(persistedEntityCopy.get().getId());
            }
        }
    }
}
