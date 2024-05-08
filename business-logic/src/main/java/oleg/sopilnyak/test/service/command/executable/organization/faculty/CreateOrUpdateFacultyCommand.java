package oleg.sopilnyak.test.service.command.executable.organization.faculty;

import lombok.extern.slf4j.Slf4j;
import oleg.sopilnyak.test.school.common.exception.NotExistFacultyException;
import oleg.sopilnyak.test.school.common.model.Faculty;
import oleg.sopilnyak.test.school.common.persistence.organization.FacultyPersistenceFacade;
import oleg.sopilnyak.test.school.common.persistence.utility.PersistenceFacadeUtilities;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.command.type.base.command.SchoolCommandCache;
import oleg.sopilnyak.test.service.command.type.organization.FacultyCommand;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.LongFunction;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

/**
 * Command-Implementation: command to create or update the faculty of the school
 *
 * @see Faculty
 * @see FacultyCommand
 * @see FacultyPersistenceFacade
 * @see SchoolCommandCache
 */
@Slf4j
@Component
public class CreateOrUpdateFacultyCommand
        extends SchoolCommandCache<Faculty>
        implements FacultyCommand {
    private final FacultyPersistenceFacade persistence;

    public CreateOrUpdateFacultyCommand(FacultyPersistenceFacade persistence) {
        super(Faculty.class);
        this.persistence = persistence;
    }

    /**
     * DO: To create or update the faculty of the school<BR/>
     * To execute command redo with correct context state
     *
     * @param context context of redo execution
     * @see Context
     * @see Context#getRedoParameter()
     * @see Context.State#WORK
     * @see SchoolCommandCache#retrieveEntity(Long, LongFunction, UnaryOperator, Supplier)
     * @see SchoolCommandCache#persistRedoEntity(Context, Function)
     * @see SchoolCommandCache#rollbackCachedEntity(Context, Function)
     * @see FacultyPersistenceFacade#findFacultyById(Long)
     * @see FacultyPersistenceFacade#toEntity(Faculty)
     * @see FacultyPersistenceFacade#save(Faculty)
     * @see NotExistFacultyException
     */
    @Override
    public <T> void executeDo(Context<T> context) {
        final Object parameter = context.getRedoParameter();
        try {
            check(parameter);
            log.debug("Trying to create or update faculty {}", parameter);
            final Long id = ((Faculty) parameter).getId();
            final boolean isCreateEntity = PersistenceFacadeUtilities.isInvalidId(id);
            if (!isCreateEntity) {
                // previous version of faculty is storing to context for further rollback (undo)
                final Faculty previous = retrieveEntity(id, persistence::findFacultyById, persistence::toEntity,
                        () -> new NotExistFacultyException(FACULTY_WITH_ID_PREFIX + id + " is not exists.")
                );
                context.setUndoParameter(previous);
            }

            final Optional<Faculty> persisted = persistRedoEntity(context, persistence::save);
            // checking execution context state
            afterPersistCheck(context, () -> rollbackCachedEntity(context, persistence::save), persisted, isCreateEntity);
        } catch (Exception e) {
            log.error("Cannot create or update faculty '{}'", parameter, e);
            context.failed(e);
            rollbackCachedEntity(context, persistence::save);
        }
    }

    /**
     * UNDO: To create or update the faculty of the school<BR/>
     * To rollback command's execution with correct context state
     *
     * @param context context of redo execution
     * @see Context
     * @see Context.State#UNDONE
     * @see Context#getUndoParameter()
     * @see SchoolCommandCache#rollbackCachedEntity(Context, Function)
     * @see FacultyPersistenceFacade#save(Faculty)
     * @see FacultyPersistenceFacade#deleteFaculty(Long)
     * @see NotExistFacultyException
     */
    @Override
    public <T> void executeUndo(Context<T> context) {
        final Object parameter = context.getUndoParameter();
        try {
            log.debug("Trying to undo faculty changes using: {}", parameter);

            rollbackCachedEntity(context, persistence::save, persistence::deleteFaculty,
                    () -> new NotExistFacultyException("Wrong undo parameter :" + parameter));

            context.setState(Context.State.UNDONE);
        } catch (Exception e) {
            log.error("Cannot undo faculty change {}", parameter, e);
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
        return CREATE_OR_UPDATE;
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
