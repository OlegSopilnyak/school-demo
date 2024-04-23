package oleg.sopilnyak.test.service.command.type.base.command;

import oleg.sopilnyak.test.school.common.exception.EntityNotExistException;
import oleg.sopilnyak.test.school.common.exception.NotExistStudentException;
import oleg.sopilnyak.test.school.common.model.Student;
import oleg.sopilnyak.test.school.common.model.base.BaseType;
import oleg.sopilnyak.test.school.common.persistence.students.courses.StudentsPersistenceFacade;
import oleg.sopilnyak.test.service.command.type.base.Context;
import org.slf4j.Logger;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.LongFunction;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import static java.util.Objects.nonNull;

/**
 * Cache for the delete or update commands
 */
public abstract class SchoolCommandCache<T extends BaseType> {
    private final Class<T> entityType;
    private final String entityName;

    protected SchoolCommandCache(Class<T> entityType) {
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
     * @param inputId system-id of the student
     * @return copy of exists entity
     * @throws NotExistStudentException if student is not exist
     * @see StudentsPersistenceFacade
     * @see StudentsPersistenceFacade#findStudentById(Long)
     * @see StudentsPersistenceFacade#toEntity(Student)
     * @see Context
     * @see Context#setUndoParameter(Object)
     */
    protected T retrieveEntity(Long inputId,
                               LongFunction<Optional<T>> findById, UnaryOperator<T> copyEntity,
                               Supplier<? extends EntityNotExistException> exceptionSupplier) {

        getLog().info("Getting entity of {} for ID:{}", entityName, inputId);
        final T existsEntity = findById.apply(inputId).orElseThrow(exceptionSupplier);
        getLog().info("Got entity of {} '{}'", entityName, existsEntity);

        // return copy of exists entity for undo operation
        getLog().info("Copying the value of '{}'", existsEntity);
        return copyEntity.apply(existsEntity);
    }

    /**
     * To restore in database the entity from cache(context)
     *
     * @param context command execution context
     * @param save    function for saving the undo entity
     * @return saved entity
     * @see Context
     * @see Function#apply(Object)
     * @see LongFunction#apply(long)
     * @see Supplier#get()
     * @see this#rollbackCachedEntity(Context, Function, LongFunction, Supplier)
     */
    protected Optional<T> rollbackCachedEntity(Context<?> context, Function<T, Optional<T>> save) {
        return rollbackCachedEntity(context, save, null, null);
    }

    /**
     * To restore in database the entity from cache(context)
     *
     * @param context command execution context
     * @param save    function for saving the undo entity
     * @param deleteById function for delete created entity
     * @param exceptionSupplier the source of entity-not-found exception
     * @return restored in the database cached entity
     * @see Context#getUndoParameter()
     * @see Function#apply(Object)
     * @see LongFunction#apply(long)
     * @see Supplier#get()
     * @see this#rollbackCachedEntity(Context, Function, LongFunction, Supplier)
     */
    protected Optional<T> rollbackCachedEntity(Context<?> context,
                                               Function<T, Optional<T>> save,
                                               LongFunction<Boolean> deleteById,
                                               Supplier<? extends EntityNotExistException> exceptionSupplier) {
        final Object parameter = context.getUndoParameter();
        if (nonNull(parameter) && entityType.isAssignableFrom(parameter.getClass())) {
            getLog().info("Restoring changed value of {}\n'{}'", entityName, parameter);
            return save.apply((T) parameter);
        } else if (nonNull(deleteById)) {
            getLog().info("Deleting created value of {} with ID:{}", entityName, parameter);
            if (parameter instanceof Long id) {
                final boolean success = deleteById.apply(id);
                getLog().info("Got deleted {} with ID:{} success: {}", entityName, id, success);
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
     * @param context command's do context
     * @return saved instance or empty
     * @see Optional#empty()
     */
    protected Optional<T> persistRedoEntity(Context<?> context, Function<T, Optional<T>> save) {
        final Object parameter = context.getRedoParameter();
        if (nonNull(parameter) && entityType.isAssignableFrom(parameter.getClass())) {
            getLog().info("Storing changed value of {} '{}'", entityName, parameter);
            return save.apply((T) parameter);
        } else {
            if (nonNull(parameter)) {
                final String message = "Wrong type of student :" + parameter.getClass().getName();
                final Exception saveError = new NotExistStudentException(message);
                saveError.fillInStackTrace();
                getLog().error(message, saveError);
                context.failed(saveError);
            }
            return Optional.empty();
        }
    }
}
