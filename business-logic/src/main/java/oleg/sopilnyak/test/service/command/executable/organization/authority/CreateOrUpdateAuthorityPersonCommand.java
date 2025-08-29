package oleg.sopilnyak.test.service.command.executable.organization.authority;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.LongFunction;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import oleg.sopilnyak.test.school.common.exception.organization.AuthorityPersonNotFoundException;
import oleg.sopilnyak.test.school.common.model.AuthorityPerson;
import oleg.sopilnyak.test.school.common.persistence.organization.AuthorityPersonPersistenceFacade;
import oleg.sopilnyak.test.school.common.persistence.utility.PersistenceFacadeUtilities;
import oleg.sopilnyak.test.service.command.executable.cache.SchoolCommandCache;
import oleg.sopilnyak.test.service.command.executable.sys.CommandContext;
import oleg.sopilnyak.test.service.command.io.Input;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.command.type.organization.AuthorityPersonCommand;
import oleg.sopilnyak.test.service.mapper.BusinessMessagePayloadMapper;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

/**
 * Command-Implementation: command to update the authority person
 *
 * @see AuthorityPerson
 * @see AuthorityPersonCommand
 * @see AuthorityPersonPersistenceFacade
 * @see SchoolCommandCache
 */
@Slf4j
@Component
public class CreateOrUpdateAuthorityPersonCommand extends SchoolCommandCache<AuthorityPerson>
        implements AuthorityPersonCommand<Optional<AuthorityPerson>> {
    private final transient AuthorityPersonPersistenceFacade persistence;
    @Getter
    private final transient BusinessMessagePayloadMapper payloadMapper;

    public CreateOrUpdateAuthorityPersonCommand(final AuthorityPersonPersistenceFacade persistence,
                                                final BusinessMessagePayloadMapper payloadMapper) {
        super(AuthorityPerson.class);
        this.persistence = persistence;
        this.payloadMapper = payloadMapper;
    }

    /**
     * DO: To create or update authority person instance<BR/>
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
     * @see AuthorityPersonPersistenceFacade#findAuthorityPersonById(Long)
     * @see AuthorityPersonPersistenceFacade#save(AuthorityPerson)
     * @see AuthorityPersonNotFoundException
     */
    @Override
    public void executeDo(Context<Optional<AuthorityPerson>> context) {
        final Input<AuthorityPerson> parameter = context.getRedoParameter();
        try {
            checkNullParameter(parameter);
            final AuthorityPerson doParameter = parameter.value();
            final Long id = doParameter.getId();
            final boolean isCreateEntityMode = PersistenceFacadeUtilities.isInvalidId(id);
            if (!isCreateEntityMode) {
                log.debug("Trying to update authority person using: {}", doParameter);
                // previous version of authority person is storing to context for further rollback (undo)
                final AuthorityPerson entity = retrieveEntity(
                        id, persistence::findAuthorityPersonById, payloadMapper::toPayload,
                        () -> new AuthorityPersonNotFoundException(PERSON_WITH_ID_PREFIX + id + " is not exists.")
                );
                // save undo input parameter
                if (context instanceof CommandContext<?> commandContext) {
                    log.debug("Previous value of the entity stored for possible command's undo: {}", entity);
                    commandContext.setUndoParameter(Input.of(entity));
                }
            } else {
                log.debug("Trying to create authority person using: {}", doParameter);
            }
            // persisting entity trough persistence layer
            final Optional<AuthorityPerson> persisted = persistRedoEntity(context, persistence::save);
            // checking command context state after entity persistence
            afterEntityPersistenceCheck(
                    context, () -> rollbackCachedEntity(context, persistence::save),
                    persisted.orElse(null), isCreateEntityMode
            );
        } catch (Exception e) {
            log.error("Cannot create or update authority person '{}'", parameter, e);
            context.failed(e);
            restoreInitialCommandState(context, persistence::save);
        }
    }

    /**
     * UNDO: To create or update authority person instance<BR/>
     * To rollback command's execution with correct context state
     *
     * @param context context of redo execution
     * @see Context
     * @see Context.State#UNDONE
     * @see Context#getUndoParameter()
     * @see this#rollbackCachedEntity(Context, Function)
     * @see AuthorityPersonPersistenceFacade#save(AuthorityPerson)
     * @see AuthorityPersonPersistenceFacade#deleteAuthorityPerson(Long)
     * @see AuthorityPersonNotFoundException
     */
    @Override
    public void executeUndo(Context<?> context) {
        final Input<?> parameter = context.getUndoParameter();
        try {
            checkNullParameter(parameter);
            log.debug("Trying to undo authority person changes using: {}", parameter);

            rollbackCachedEntity(context, persistence::save, persistence::deleteAuthorityPerson);

            context.setState(Context.State.UNDONE);
        } catch (Exception e) {
            log.error("Cannot undo authority person change {}", parameter, e);
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
