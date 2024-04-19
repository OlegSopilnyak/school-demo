package oleg.sopilnyak.test.service.command.type.base;

import oleg.sopilnyak.test.school.common.exception.StudentNotExistsException;
import oleg.sopilnyak.test.school.common.facade.peristence.students.courses.StudentsPersistenceFacade;
import oleg.sopilnyak.test.school.common.model.Student;
import org.slf4j.Logger;

import java.util.Optional;

/**
 * Type for update school-student command
 */
public interface ChangeStudentCommand {
    /**
     * To get reference to command's persistence facade
     *
     * @return reference to the persistence facade
     */
    StudentsPersistenceFacade getPersistenceFacade();

    /**
     * To get reference to command's logger
     *
     * @return reference to the logger
     */
    Logger getLog();

    /**
     * To cache into context old value of the student instance for possible rollback
     *
     * @param inputId system-id of the student
     * @throws StudentNotExistsException if student is not exist
     * @see StudentsPersistenceFacade
     * @see StudentsPersistenceFacade#findStudentById(Long)
     * @see StudentsPersistenceFacade#toEntity(Student)
     * @see Context
     * @see Context#setUndoParameter(Object)
     */
    default Object cacheEntityForRollback(Long inputId) throws StudentNotExistsException {
        final Student existsEntity = getPersistenceFacade().findStudentById(inputId)
                .orElseThrow(() -> new StudentNotExistsException("Student with ID:" + inputId + " is not exists."));
        // return copy of exists entity for undo operation
        return getPersistenceFacade().toEntity(existsEntity);
    }

    /**
     * To restore student entity from cache(context)
     *
     * @param context command execution context
     */
    default void rollbackCachedEntity(Context<?> context) {
        final Object undoParameter = context.getUndoParameter();
        if (undoParameter instanceof Student student) {
            getLog().debug("Restoring changed value of student {}", student);
            getPersistenceFacade().save(student);
        }
    }

    /**
     * To persist entity
     *
     * @param context command's do context
     * @return saved instance or empty
     * @see Student
     * @see Optional#empty()
     */
    default Optional<Student> persistRedoEntity(Context<?> context) {
        final Object input = context.getRedoParameter();
        if (input instanceof Student student) {
            return getPersistenceFacade().save(student);
        } else {
            final String message = "Wrong type of student :" + input.getClass().getName();
            final Exception saveError = new StudentNotExistsException(message);
            saveError.fillInStackTrace();
            getLog().error(message, saveError);
            context.failed(saveError);
            return Optional.empty();
        }
    }
}
