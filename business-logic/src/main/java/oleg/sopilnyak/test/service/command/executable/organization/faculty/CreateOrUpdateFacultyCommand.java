package oleg.sopilnyak.test.service.command.executable.organization.faculty;

import oleg.sopilnyak.test.school.common.exception.organization.FacultyNotFoundException;
import oleg.sopilnyak.test.school.common.model.Faculty;
import oleg.sopilnyak.test.school.common.persistence.organization.FacultyPersistenceFacade;
import oleg.sopilnyak.test.school.common.persistence.utility.PersistenceFacadeUtilities;
import oleg.sopilnyak.test.service.command.executable.sys.cache.SchoolCommandCache;
import oleg.sopilnyak.test.service.command.executable.sys.context.CommandContext;
import oleg.sopilnyak.test.service.command.io.Input;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.command.type.organization.FacultyCommand;
import oleg.sopilnyak.test.service.mapper.BusinessMessagePayloadMapper;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.LongFunction;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Command-Implementation: command to create or update the faculty of the school
 *
 * @see Faculty
 * @see FacultyCommand
 * @see FacultyPersistenceFacade
 * @see SchoolCommandCache
 */
@Slf4j
@Component(FacultyCommand.SPRING_CREATE_OR_UPDATE)
public class CreateOrUpdateFacultyCommand extends SchoolCommandCache<Faculty, Optional<Faculty>>
        implements FacultyCommand<Optional<Faculty>> {
    private final transient FacultyPersistenceFacade persistence;
    @Getter
    private final transient BusinessMessagePayloadMapper payloadMapper;

    /**
     * The name of command bean in spring beans factory
     *
     * @return spring name of the command
     */
    @Override
    public String springName() {
        return SPRING_CREATE_OR_UPDATE;
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

    public CreateOrUpdateFacultyCommand(final FacultyPersistenceFacade persistence,
                                        final BusinessMessagePayloadMapper payloadMapper) {
        super(Faculty.class);
        this.persistence = persistence;
        this.payloadMapper = payloadMapper;
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
     * @see SchoolCommandCache#restoreInitialCommandState(Context, Function)
     * @see FacultyPersistenceFacade#findFacultyById(Long)
     * @see FacultyPersistenceFacade#save(Faculty)
     * @see FacultyNotFoundException
     */
    @Override
    public void executeDo(Context<Optional<Faculty>> context) {
        final Input<Faculty> parameter = context.getRedoParameter();
        try {
            checkNullParameter(parameter);
            log.debug("Trying to create or update faculty {}", parameter);
            final Long id = parameter.value().getId();
            final boolean isCreateEntityMode = PersistenceFacadeUtilities.isInvalidId(id);
            if (!isCreateEntityMode) {
                // previous version of faculty is storing to context for further rollback (undo)
                final var entity = retrieveEntity(
                        id, persistence::findFacultyById, payloadMapper::toPayload,
                        () -> new FacultyNotFoundException(FACULTY_WITH_ID_PREFIX + id + " is not exists.")
                );
                if (context instanceof CommandContext<?> commandContext) {
                    log.debug("Previous value of the entity stored for possible command's undo: {}", entity);
                    commandContext.setUndoParameter(Input.of(entity));
                }
            } else {
                log.debug("Trying to create faculty using: {}", parameter);
            }
            // persisting entity trough persistence layer
            final Optional<Faculty> persisted = persistRedoEntity(context, persistence::save);
            // checking command context state after entity persistence
            afterEntityPersistenceCheck(
                    context, () -> rollbackCachedEntity(context, persistence::save),
                    persisted.orElse(null), isCreateEntityMode
            );
        } catch (Exception e) {
            log.error("Cannot create or update faculty '{}'", parameter, e);
            context.failed(e);
            restoreInitialCommandState(context, persistence::save);
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
     * @see FacultyNotFoundException
     */
    @Override
    public void executeUndo(Context<?> context) {
        final Input<?> parameter = context.getUndoParameter();
        try {
            checkNullParameter(parameter);
            log.debug("Trying to undo faculty changes using: {}", parameter);

            rollbackCachedEntity(context, persistence::save, persistence::deleteFaculty);

            context.setState(Context.State.UNDONE);
        } catch (Exception e) {
            log.error("Cannot undo faculty change {}", parameter, e);
            context.failed(e);
        }
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
