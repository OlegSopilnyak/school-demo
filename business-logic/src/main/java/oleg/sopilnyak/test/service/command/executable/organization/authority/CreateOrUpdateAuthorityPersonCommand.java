package oleg.sopilnyak.test.service.command.executable.organization.authority;

import lombok.extern.slf4j.Slf4j;
import oleg.sopilnyak.test.school.common.exception.NotExistAuthorityPersonException;
import oleg.sopilnyak.test.school.common.model.AuthorityPerson;
import oleg.sopilnyak.test.school.common.persistence.organization.AuthorityPersonPersistenceFacade;
import oleg.sopilnyak.test.school.common.persistence.utility.PersistenceFacadeUtilities;
import oleg.sopilnyak.test.service.command.executable.cache.SchoolCommandCache;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.command.type.organization.AuthorityPersonCommand;
import oleg.sopilnyak.test.service.mapper.BusinessMessagePayloadMapper;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.LongFunction;
import java.util.function.Supplier;

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
public class CreateOrUpdateAuthorityPersonCommand
        extends SchoolCommandCache<AuthorityPerson>
        implements AuthorityPersonCommand {
    private final AuthorityPersonPersistenceFacade persistence;

    public CreateOrUpdateAuthorityPersonCommand(final AuthorityPersonPersistenceFacade persistence,
                                                final BusinessMessagePayloadMapper payloadMapper) {
        super(AuthorityPerson.class, payloadMapper);
        this.persistence = persistence;
    }

    /**
     * DO: To create or update authority person instance<BR/>
     * To execute command redo with correct context state
     *
     * @param context context of redo execution
     * @see Context
     * @see Context#getRedoParameter()
     * @see Context.State#WORK
     * @see this#retrieveEntity(Long, LongFunction, Supplier)
     * @see this#persistRedoEntity(Context, Function)
     * @see this#rollbackCachedEntity(Context, Function)
     * @see AuthorityPersonPersistenceFacade#findAuthorityPersonById(Long)
     * @see AuthorityPersonPersistenceFacade#save(AuthorityPerson)
     * @see NotExistAuthorityPersonException
     */
    @Override
    public <T> void executeDo(Context<T> context) {
        final Object parameter = context.getRedoParameter();
        try {
            check(parameter);
            log.debug("Trying to create or update authority person {}", parameter);
            final Long id = ((AuthorityPerson) parameter).getId();
            final boolean isCreateEntity = PersistenceFacadeUtilities.isInvalidId(id);
            if (!isCreateEntity) {
                // previous version of authority person is storing to context for further rollback (undo)
                final AuthorityPerson entity = retrieveEntity(id, persistence::findAuthorityPersonById,
                        () -> new NotExistAuthorityPersonException(PERSON_WITH_ID_PREFIX + id + " is not exists.")
                );
                context.setUndoParameter(entity);
            }

            final Optional<AuthorityPerson> persisted = persistRedoEntity(context, persistence::save);
            // checking execution context state
            afterPersistCheck(context, () -> rollbackCachedEntity(context, persistence::save), persisted, isCreateEntity);
        } catch (Exception e) {
            log.error("Cannot create or update authority person '{}'", parameter, e);
            context.failed(e);
            rollbackCachedEntity(context, persistence::save);
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
     * @see NotExistAuthorityPersonException
     */
    @Override
    public <T> void executeUndo(Context<T> context) {
        final Object parameter = context.getUndoParameter();
        try {
            log.debug("Trying to undo authority person changes using: {}", parameter);

            rollbackCachedEntity(context, persistence::save, persistence::deleteAuthorityPerson,
                    () -> new NotExistAuthorityPersonException("Wrong undo parameter :" + parameter));

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
