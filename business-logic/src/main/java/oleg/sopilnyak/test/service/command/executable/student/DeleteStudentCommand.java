package oleg.sopilnyak.test.service.command.executable.student;

import lombok.extern.slf4j.Slf4j;
import oleg.sopilnyak.test.school.common.exception.EntityNotExistException;
import oleg.sopilnyak.test.school.common.exception.NotExistStudentException;
import oleg.sopilnyak.test.school.common.exception.StudentWithCoursesException;
import oleg.sopilnyak.test.school.common.model.Student;
import oleg.sopilnyak.test.school.common.persistence.students.courses.StudentsPersistenceFacade;
import oleg.sopilnyak.test.school.common.persistence.utility.PersistenceFacadeUtilities;
import oleg.sopilnyak.test.service.command.executable.cache.SchoolCommandCache;
import oleg.sopilnyak.test.service.command.type.StudentCommand;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.mapper.BusinessMessagePayloadMapper;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import java.util.function.Function;
import java.util.function.LongConsumer;
import java.util.function.LongFunction;
import java.util.function.Supplier;

/**
 * Command-Implementation: command to delete the student
 *
 * @see SchoolCommandCache
 * @see Student
 * @see StudentsPersistenceFacade
 */
@Slf4j
@Component
public class DeleteStudentCommand extends SchoolCommandCache<Student> implements StudentCommand {
    private final StudentsPersistenceFacade persistence;

    public DeleteStudentCommand(final StudentsPersistenceFacade persistence,
                                final BusinessMessagePayloadMapper payloadMapper) {
        super(Student.class, payloadMapper);
        this.persistence = persistence;
    }

    /**
     * DO: To delete the student by id<BR/>
     * To execute command redo with correct context state
     *
     * @param context context of redo execution
     * @see Context
     * @see Context#getRedoParameter()
     * @see Context.State#WORK
     * @see SchoolCommandCache#retrieveEntity(Long, LongFunction, Supplier) )
     * @see SchoolCommandCache#rollbackCachedEntity(Context, Function)
     */
    @Override
    public <T> void executeDo(Context<T> context) {
        final Object parameter = context.getRedoParameter();
        try {
            log.debug("Trying to delete student by ID: {}", parameter.toString());
            final Long inputId = commandParameter(parameter);
            final EntityNotExistException notFoundException =
                    new NotExistStudentException(STUDENT_WITH_ID_PREFIX + inputId + " is not exists.");
            if (PersistenceFacadeUtilities.isInvalidId(inputId)) {
                log.warn(STUDENT_WITH_ID_PREFIX + "{} is not exists.", inputId);
                throw notFoundException;
            }

            // previous student is storing to context for further rollback (undo)
            final Student entity = retrieveEntity(inputId, persistence::findStudentById, () -> notFoundException);

            if (!ObjectUtils.isEmpty(entity.getCourses())) {
                log.warn(STUDENT_WITH_ID_PREFIX + "{} has registered courses.", inputId);
                throw new StudentWithCoursesException(STUDENT_WITH_ID_PREFIX + inputId + " has registered courses.");
            }

            // cached student is storing to context for further rollback (undo)
            context.setUndoParameter(entity);
            persistence.deleteStudent(inputId);
            context.setResult(true);
        } catch (Exception e) {
            rollbackCachedEntity(context, persistence::save);
            log.error("Cannot delete the student by Id: {}", parameter, e);
            context.failed(e);
        }
    }

    /**
     * UNDO: To delete the student by id<BR/>
     * To rollback command's execution with correct context state
     *
     * @param context context of redo execution
     * @see Context
     * @see Context#getUndoParameter()
     * @see SchoolCommandCache#rollbackCachedEntity(Context, Function, LongConsumer, Supplier)
     */
    @Override
    public <T> void executeUndo(Context<T> context) {
        final Object parameter = context.getUndoParameter();
        try {
            log.debug("Trying to undo student deletion using: {}", parameter.toString());
            final Student student = rollbackCachedEntity(context, persistence::save)
                    .orElseThrow(() -> new NotExistStudentException("Wrong undo parameter :" + parameter));

            log.debug("Updated in database: '{}'", student);
            context.setState(Context.State.UNDONE);
        } catch (Exception e) {
            log.error("Cannot undo student deletion {}", parameter, e);
            context.failed(e);
        }
    }

    /**
     * To get unique command-id for the command
     *
     * @return value of command-id
     */
    @Override
    public String getId() {
        return DELETE_COMMAND_ID;
    }

    /**
     * To get reference to command's logger
     *
     * @return reference to the logger
     */
    @Override
    public Logger getLog() {
        return log;
    }
}
