package oleg.sopilnyak.test.service.command.type.base;

import oleg.sopilnyak.test.school.common.exception.EntityNotExistException;
import oleg.sopilnyak.test.school.common.exception.NotExistStudentException;
import oleg.sopilnyak.test.school.common.model.Student;
import oleg.sopilnyak.test.school.common.persistence.students.courses.StudentsPersistenceFacade;
import org.slf4j.Logger;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.LongFunction;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import static java.util.Objects.nonNull;

/**
 * Cache for the delete or update caching
 */
public abstract class SchoolCommandCache<T> {
    final Class<T> entityClass;

    protected SchoolCommandCache(Class<T> entityClass) {
        this.entityClass = entityClass;
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
        getLog().info("Getting value of {} for ID:{}", entityClass.getSimpleName(), inputId);
        final T existsEntity = findById.apply(inputId).orElseThrow(exceptionSupplier);

        // return copy of exists entity for undo operation
        getLog().info("Copying the value of '{}'", existsEntity);
        return copyEntity.apply(existsEntity);
    }

    /**
     * To restore in database the entity from cache(context)
     *
     * @param context command execution context
     */
    protected Optional<T> rollbackCachedEntity(Context<?> context, Function<T, Optional<T>> save) {
        final Object undoParameter = context.getUndoParameter();
        if (nonNull(undoParameter) && entityClass.isAssignableFrom(undoParameter.getClass())) {
            getLog().info("Restoring changed value of {} '{}'", entityClass.getSimpleName(), undoParameter);
            return save.apply((T) undoParameter);
        }
        return Optional.empty();
    }
}
