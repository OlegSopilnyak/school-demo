package oleg.sopilnyak.test.service.command.executable.sys.cache;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import oleg.sopilnyak.test.school.common.exception.EntityNotFoundException;
import oleg.sopilnyak.test.school.common.exception.core.InvalidParameterTypeException;
import oleg.sopilnyak.test.school.common.exception.education.StudentNotFoundException;
import oleg.sopilnyak.test.school.common.model.BaseType;
import oleg.sopilnyak.test.school.common.persistence.education.StudentsPersistenceFacade;
import oleg.sopilnyak.test.service.command.executable.sys.BasicCommand;
import oleg.sopilnyak.test.service.command.executable.sys.context.CommandContext;
import oleg.sopilnyak.test.service.command.io.Input;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.mapper.BusinessMessagePayloadMapper;
import oleg.sopilnyak.test.service.message.payload.BasePayload;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.LongConsumer;
import java.util.function.LongFunction;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import org.slf4j.Logger;

/**
 *  Type: CommandCache for the delete or update entities commands
 *
 * @see BasicCommand
 * @param <T> type of entity to manage
 * @param <R> type of command execution result
 */
public abstract class SchoolCommandCache<T extends BaseType, R> extends BasicCommand<R> {
    // class of the entity to manage
    private final Class<T> entityType;
    private final String entityName;
//    // beans factory to prepare the current command for transactional operations
//    protected transient BeanFactory applicationContext;
//    // reference to current command for transactional operations
//    private final AtomicReference<RootCommand<R>> self = new AtomicReference<>(null);
//
//    @Autowired
//    public final void setApplicationContext(BeanFactory applicationContext) {
//        this.applicationContext = applicationContext;
//    }
//
//    /**
//     * Reference to the current command for transactional operations
//     *
//     * @return reference to the current command
//     * @see RootCommand#self()
//     * @see RootCommand#doCommand(Context)
//     * @see RootCommand#undoCommand(Context)
//     */
//    @Override
//    public RootCommand<R> self() {
//        synchronized (RootCommand.class) {
//            if (isNull(self.get())) {
//                // getting command instance reference, which can be used for transactional operations
//                // actually it's proxy of the command with transactional executeDo/executeUndo methods
//                final String springName = springName();
//                final Class<? extends RootCommand<R>> familyType = commandFamily();
//                getLog().info("Getting command from family:{} bean-name:{}",familyType.getSimpleName(), springName);
//                self.getAndSet(applicationContext.getBean(springName, familyType));
//            }
//        }
//        return self.get();
//    }

    @SuppressWarnings("unchecked")
    protected SchoolCommandCache() {
        this.entityType = (Class<T>) BaseType.class;
        this.entityName = "BaseType";
    }

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
     * @param inputId           system-id of the entity to retrieve
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
            // clear the do command result after entity deleting
            context.setResult(null);
            final Input<?> doInput = context.getRedoParameter();
            if (isNull(doInput) || doInput.isEmpty()) {
                // command do input is empty
                getLog().debug("No input entity to clean entity-id.");
                return Optional.empty();
            }
            // cleaning input entity-id and do command result
            if (doInput.value() instanceof BasePayload<?> payload) {
                // clear ID for further CREATE entity
                payload.setId(null);

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
        // check undo parameter in the context
        final Input<T> undoParameter = context.getUndoParameter();
        // check if there is undo parameter in the context
        if (isNull(undoParameter) || undoParameter.isEmpty()) {
            // there is no undo parameter in the context
            return;
        }
        getLog().debug("Restoring state of command '{}' after fail using: {}", context.getCommand().getId(), undoParameter);
        rollbackCachedEntity(context, facadeSave);
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

            if (nonNull(persistedEntityCopy) && isCreateEntityMode
                && context instanceof CommandContext<Optional<T>> commandContext
            ) {
                commandContext.setUndoParameter(Input.of(persistedEntityCopy.getId()));
            }
        }
    }

    /**
     * Setup context's undo parameter after entity removing
     *
     * @param context           command's do context
     * @param undoEntity        removed from database entity
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
    private <I> I inputParameter(final Input<I> parameter) {
        if (isNull(parameter) || parameter.isEmpty()) {
            throw new InvalidParameterTypeException(entityName, parameter);
        } else {
            return parameter.value();
        }
    }
}
