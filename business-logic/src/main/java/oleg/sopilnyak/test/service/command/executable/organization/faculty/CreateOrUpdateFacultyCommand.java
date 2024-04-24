package oleg.sopilnyak.test.service.command.executable.organization.faculty;

import lombok.extern.slf4j.Slf4j;
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
        implements FacultyCommand<Optional<Faculty>> {
    private final FacultyPersistenceFacade persistence;

    public CreateOrUpdateFacultyCommand(FacultyPersistenceFacade persistence) {
        super(Faculty.class);
        this.persistence = persistence;
    }

    /**
     * To create or update the faculty of the school
     *
     * @param parameter faculty instance to save
     * @return execution's result
     * @deprecated commands are going to work through redo/undo
     */
    @Deprecated(forRemoval = true)
    @Override
    public CommandResult<Optional<Faculty>> execute(Object parameter) {
        try {
            log.debug("Trying to create or update faculty {}", parameter);
            Faculty instance = commandParameter(parameter);
            Optional<Faculty> faculty = persistence.save(instance);
            log.debug("Got stored faculty {} from parameter {}", faculty, instance);
            return CommandResult.<Optional<Faculty>>builder()
                    .result(Optional.ofNullable(faculty)).success(true).build();
        } catch (Exception e) {
            log.error("Cannot create or update faculty {}", parameter, e);
            return CommandResult.<Optional<Faculty>>builder()
                    .result(Optional.of(Optional.empty()))
                    .exception(e).success(false).build();
        }
    }

    /**
     * DO: To create or update the faculty of the school<BR/>
     * To execute command redo with correct context state
     *
     * @param context context of redo execution
     * @see Context
     * @see Context#getRedoParameter()
     * @see Context.State#WORK
     * @see this#retrieveEntity(Long, LongFunction, UnaryOperator, Supplier)
     * @see this#persistRedoEntity(Context, Function)
     * @see this#rollbackCachedEntity(Context, Function)
     * @see FacultyPersistenceFacade#findFacultyById(Long)
     * @see FacultyPersistenceFacade#toEntity(Faculty)
     * @see FacultyPersistenceFacade#save(Faculty)
     * @see NotExistFacultyException
     */
    @Override
    public void executeDo(Context<?> context) {
        final Object parameter = context.getRedoParameter();
        try {
            check(parameter);
            log.debug("Trying to create or update faculty {}", parameter);
            final Long id = ((Faculty) parameter).getId();
            final boolean isCreateEntity = PersistenceFacadeUtilities.isInvalidId(id);
            if (!isCreateEntity) {
                // cached faculty is storing to context for further rollback (undo)
                context.setUndoParameter(
                        retrieveEntity(id, persistence::findFacultyById, persistence::toEntity,
                                () -> new NotExistFacultyException(FACULTY_WITH_ID_PREFIX + id + " is not exists.")
                        )
                );
            }

            final Optional<Faculty> persisted = persistRedoEntity(context, persistence::save);
            // checking execution context state
            afterPersistCheck(context, () -> rollbackCachedEntity(context, persistence::save), persisted, isCreateEntity);
        } catch (Exception e) {
            rollbackCachedEntity(context, persistence::save);
            log.error("Cannot create or update faculty '{}'", parameter, e);
            context.failed(e);
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
     * @see this#rollbackCachedEntity(Context, Function)
     * @see FacultyPersistenceFacade#save(Faculty)
     * @see FacultyPersistenceFacade#deleteFaculty(Long)
     * @see NotExistFacultyException
     */
    @Override
    public void executeUndo(Context<?> context) {
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
