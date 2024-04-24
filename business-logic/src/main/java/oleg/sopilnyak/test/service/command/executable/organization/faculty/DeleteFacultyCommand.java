package oleg.sopilnyak.test.service.command.executable.organization.faculty;

import lombok.extern.slf4j.Slf4j;
import oleg.sopilnyak.test.school.common.exception.FacultyIsNotEmptyException;
import oleg.sopilnyak.test.school.common.exception.NotExistFacultyException;
import oleg.sopilnyak.test.school.common.model.Faculty;
import oleg.sopilnyak.test.school.common.persistence.organization.FacultyPersistenceFacade;
import oleg.sopilnyak.test.school.common.persistence.utility.PersistenceFacadeUtilities;
import oleg.sopilnyak.test.service.command.executable.sys.CommandResult;
import oleg.sopilnyak.test.service.command.type.FacultyCommand;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.command.type.base.command.SchoolCommandCache;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import java.util.Optional;
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
        implements FacultyCommand<Boolean> {
    private final FacultyPersistenceFacade persistence;

    public DeleteFacultyCommand(FacultyPersistenceFacade persistence) {
        super(Faculty.class);
        this.persistence = persistence;
    }

    /**
     * To delete faculty by id
     *
     * @param parameter system faculty-id
     * @return execution's result
     * @deprecated commands are going to work through redo/undo
     */
    @Deprecated(forRemoval = true)
    @Override
    public CommandResult<Boolean> execute(Object parameter) {
        try {
            log.debug("Trying to delete faculty with ID: {}", parameter);
            Long id = commandParameter(parameter);
            Optional<Faculty> person = persistence.findFacultyById(id);
            if (person.isEmpty()) {
                return CommandResult.<Boolean>builder().result(Optional.empty())
                        .exception(new NotExistFacultyException("Faculty with ID:" + id + " is not exists."))
                        .success(false).build();
            } else if (!person.get().getCourses().isEmpty()) {
                return CommandResult.<Boolean>builder().result(Optional.empty())
                        .exception(new FacultyIsNotEmptyException("Faculty with ID:" + id + " has courses."))
                        .success(false).build();
            }

            persistence.deleteFaculty(id);

            log.debug("Deleted faculty {} {}", person.get(), true);
            return CommandResult.<Boolean>builder().result(Optional.of(true)).success(true).build();
        } catch (Exception e) {
            log.error("Cannot delete the authority person by ID:{}", parameter, e);
            return CommandResult.<Boolean>builder().result(Optional.empty())
                    .exception(e).success(false).build();
        }
    }

    /**
     * DO: To delete faculty by id<BR/>
     * To execute command redo with correct context state
     *
     * @param context context of redo execution
     * @see Context
     * @see Context#getRedoParameter()
     * @see Context.State#WORK
     * @see this#retrieveEntity(Long, LongFunction, UnaryOperator, Supplier)
     * @see this#rollbackCachedEntity(Context, Function)
     * @see FacultyPersistenceFacade#findFacultyById(Long)
     * @see FacultyPersistenceFacade#toEntity(Faculty)
     * @see FacultyPersistenceFacade#deleteFaculty(Long)
     */
    @Override
    public void executeDo(Context<?> context) {
        final Object parameter = context.getRedoParameter();
        try {
            log.debug("Trying to delete faculty with ID: {}", parameter);
            final Long id = commandParameter(parameter);
            final var notFoundException = new NotExistFacultyException(FACULTY_WITH_ID_PREFIX + id + " is not exists.");
            if (PersistenceFacadeUtilities.isInvalidId(id)) {
                throw notFoundException;
            }
            // cached faculty is storing to context for further rollback (undo)
            context.setUndoParameter(
                    retrieveEntity(id, persistence::findFacultyById, persistence::toEntity, () -> notFoundException)
            );
            persistence.deleteFaculty(id);
            context.setResult(true);
            log.debug("Deleted faculty with ID: {} successfully.", id);
        } catch (Exception e) {
            rollbackCachedEntity(context, persistence::save);
            log.error("Cannot delete faculty with :{}", parameter, e);
            context.failed(e);
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
     * @see this#rollbackCachedEntity(Context, Function)
     * @see FacultyPersistenceFacade#save(Faculty)
     */
    @Override
    public void executeUndo(Context<?> context) {
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
