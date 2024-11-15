package oleg.sopilnyak.test.service.command.executable.organization.authority;

import lombok.extern.slf4j.Slf4j;
import oleg.sopilnyak.test.school.common.exception.organization.AuthorityPersonManagesFacultyException;
import oleg.sopilnyak.test.school.common.exception.EntityNotFoundException;
import oleg.sopilnyak.test.school.common.exception.organization.AuthorityPersonNotFoundException;
import oleg.sopilnyak.test.school.common.model.AuthorityPerson;
import oleg.sopilnyak.test.school.common.persistence.organization.AuthorityPersonPersistenceFacade;
import oleg.sopilnyak.test.school.common.persistence.utility.PersistenceFacadeUtilities;
import oleg.sopilnyak.test.service.command.executable.cache.SchoolCommandCache;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.command.type.organization.AuthorityPersonCommand;
import oleg.sopilnyak.test.service.mapper.BusinessMessagePayloadMapper;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import java.util.function.Function;
import java.util.function.LongFunction;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

/**
 * Command-Implementation: command to delete the authority person by id
 *
 * @see AuthorityPerson
 * @see AuthorityPersonCommand
 * @see AuthorityPersonPersistenceFacade
 * @see SchoolCommandCache
 */
@Slf4j
@Component
public class DeleteAuthorityPersonCommand extends SchoolCommandCache<AuthorityPerson>
        implements AuthorityPersonCommand<Boolean> {
    private final AuthorityPersonPersistenceFacade persistence;
    private final BusinessMessagePayloadMapper payloadMapper;

    public DeleteAuthorityPersonCommand(final AuthorityPersonPersistenceFacade persistence,
                                        final BusinessMessagePayloadMapper payloadMapper) {
        super(AuthorityPerson.class);
        this.persistence = persistence;
        this.payloadMapper = payloadMapper;
    }

    /**
     * DO: To delete authority person by id<BR/>
     * To execute command redo with correct context state
     *
     * @param context context of redo execution
     * @see Context
     * @see Context#getRedoParameter()
     * @see Context.State#WORK
     * @see SchoolCommandCache#retrieveEntity(Long, LongFunction, UnaryOperator, Supplier)
     * @see SchoolCommandCache#rollbackCachedEntity(Context, Function)
     * @see AuthorityPersonPersistenceFacade#findAuthorityPersonById(Long)
     * @see AuthorityPersonPersistenceFacade#deleteAuthorityPerson(Long)
     * @see AuthorityPersonNotFoundException
     */
    @Override
    public void executeDo(Context<Boolean> context) {
        final Object parameter = context.getRedoParameter();
        try {
            checkNullParameter(parameter);
            log.debug("Trying to delete authority person using: {}", parameter);
            final Long id = commandParameter(parameter);
            if (PersistenceFacadeUtilities.isInvalidId(id)) {
                log.warn("Invalid id {}", id);
                throw exceptionFor(id);
            }
            // getting from the database current version of the authority person
            final AuthorityPerson entity = retrieveEntity(
                    id, persistence::findAuthorityPersonById, payloadMapper::toPayload, () -> exceptionFor(id)
            );
            if (!entity.getFaculties().isEmpty()) {
                log.warn(PERSON_WITH_ID_PREFIX + "{} is managing faculties.", id);
                throw new AuthorityPersonManagesFacultyException(PERSON_WITH_ID_PREFIX + id + " is managing faculties.");
            }
            // removing authority person instance by ID from the database
            persistence.deleteAuthorityPerson(id);
            // setup undo parameter for deleted entity
            setupUndoParameter(context, entity, () -> exceptionFor(id));
            // successful delete entity operation
            context.setResult(true);
            log.debug("Deleted authority person with ID: {} successfully.", id);
        } catch (Exception e) {
            log.error("Cannot delete authority person with :{}", parameter, e);
            context.failed(e);
        }
    }

    /**
     * UNDO: To delete authority person by id<BR/>
     * To rollback command's execution with correct context state
     *
     * @param context context of redo execution
     * @see Context
     * @see Context#getUndoParameter()
     * @see Context.State#UNDONE
     * @see this#rollbackCachedEntity(Context, Function)
     */
    @Override
    public void executeUndo(Context<?> context) {
        final Object parameter = context.getUndoParameter();
        try {
            checkNullParameter(parameter);
            log.debug("Trying to undo authority person deletion using: {}", parameter);

            final var entity = rollbackCachedEntity(context, persistence::save).orElseThrow();

            // change authority-person-id value for further do command action
            context.setRedoParameter(entity.getId());
            context.setState(Context.State.UNDONE);
        } catch (Exception e) {
            log.error("Cannot undo authority person deletion {}", parameter, e);
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

    // private methods
    private EntityNotFoundException exceptionFor(final Long id) {
        return new AuthorityPersonNotFoundException(PERSON_WITH_ID_PREFIX + id + " is not exists.");
    }
}
