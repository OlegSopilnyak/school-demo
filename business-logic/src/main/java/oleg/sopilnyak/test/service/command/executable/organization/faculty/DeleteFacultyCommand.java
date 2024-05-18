package oleg.sopilnyak.test.service.command.executable.organization.faculty;

import lombok.extern.slf4j.Slf4j;
import oleg.sopilnyak.test.school.common.exception.FacultyIsNotEmptyException;
import oleg.sopilnyak.test.school.common.exception.NotExistFacultyException;
import oleg.sopilnyak.test.school.common.model.Faculty;
import oleg.sopilnyak.test.school.common.persistence.organization.FacultyPersistenceFacade;
import oleg.sopilnyak.test.school.common.persistence.utility.PersistenceFacadeUtilities;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.command.executable.cache.SchoolCommandCache;
import oleg.sopilnyak.test.service.command.type.organization.FacultyCommand;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import java.util.function.Function;
import java.util.function.LongFunction;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

/**
 * Command-Implementation: command to delete the faculty of the school by id
 *
 * @see Faculty
 * @see FacultyCommand
 * @see FacultyPersistenceFacade
 * @see SchoolCommandCache
 */
@Slf4j
@Component
public class DeleteFacultyCommand
        extends SchoolCommandCache<Faculty>
        implements FacultyCommand {
    private final FacultyPersistenceFacade persistence;

    public DeleteFacultyCommand(FacultyPersistenceFacade persistence) {
        super(Faculty.class);
        this.persistence = persistence;
    }

    /**
     * DO: To delete faculty by id<BR/>
     * To execute command redo with correct context state
     *
     * @param context context of redo execution
     * @see Context
     * @see Context#getRedoParameter()
     * @see Context.State#WORK
     * @see SchoolCommandCache#retrieveEntity(Long, LongFunction, UnaryOperator, Supplier)
     * @see SchoolCommandCache#rollbackCachedEntity(Context, Function)
     * @see FacultyPersistenceFacade#findFacultyById(Long)
     * @see FacultyPersistenceFacade#toEntity(Faculty)
     * @see FacultyPersistenceFacade#deleteFaculty(Long)
     */
    @Override
    public <T> void executeDo(Context<T> context) {
        final Object parameter = context.getRedoParameter();
        try {
            log.debug("Trying to delete faculty with ID: {}", parameter);
            final Long id = commandParameter(parameter);
            final var notFoundException = new NotExistFacultyException(FACULTY_WITH_ID_PREFIX + id + " is not exists.");
            if (PersistenceFacadeUtilities.isInvalidId(id)) {
                throw notFoundException;
            }

            // getting from the database current version of the faculty
            final Faculty previous =
                    retrieveEntity(id, persistence::findFacultyById, persistence::toEntity, () -> notFoundException);

            if (!previous.getCourses().isEmpty()) {
                throw new FacultyIsNotEmptyException(FACULTY_WITH_ID_PREFIX + id + " has courses.");
            }

            // previous version of faculty is storing to context for further rollback (undo)
            context.setUndoParameter(previous);
            // deleting entity
            persistence.deleteFaculty(id);
            context.setResult(true);
            log.debug("Deleted faculty with ID: {} successfully.", id);
        } catch (Exception e) {
            log.error("Cannot delete faculty with :{}", parameter, e);
            context.failed(e);
            rollbackCachedEntity(context, persistence::save);
        }
    }

    /**
     * UNDO: To delete faculty by id<BR/>
     * To rollback command's execution with correct context state
     *
     * @param context context of redo execution
     * @see Context
     * @see Context#getUndoParameter()
     * @see Context.State#UNDONE
     * @see SchoolCommandCache#rollbackCachedEntity(Context, Function)
     * @see FacultyPersistenceFacade#save(Faculty)
     */
    @Override
    public <T> void executeUndo(Context<T> context) {
        final Object parameter = context.getUndoParameter();
        try {
            log.debug("Trying to undo faculty deletion using: {}", parameter);

            rollbackCachedEntity(context, persistence::save);

            context.setState(Context.State.UNDONE);
        } catch (Exception e) {
            log.error("Cannot undo faculty deletion {}", parameter, e);
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
        return DELETE;
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
