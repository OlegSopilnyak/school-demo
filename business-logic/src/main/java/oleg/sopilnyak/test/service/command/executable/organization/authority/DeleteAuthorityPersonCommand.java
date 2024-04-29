package oleg.sopilnyak.test.service.command.executable.organization.authority;

import lombok.extern.slf4j.Slf4j;
import oleg.sopilnyak.test.school.common.exception.AuthorityPersonManageFacultyException;
import oleg.sopilnyak.test.school.common.exception.NotExistAuthorityPersonException;
import oleg.sopilnyak.test.school.common.model.AuthorityPerson;
import oleg.sopilnyak.test.school.common.persistence.organization.AuthorityPersonPersistenceFacade;
import oleg.sopilnyak.test.school.common.persistence.utility.PersistenceFacadeUtilities;
import oleg.sopilnyak.test.service.command.executable.sys.CommandResult;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.command.type.base.command.SchoolCommandCache;
import oleg.sopilnyak.test.service.command.type.organization.AuthorityPersonCommand;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import java.util.Optional;
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
public class DeleteAuthorityPersonCommand
        extends SchoolCommandCache<AuthorityPerson>
        implements AuthorityPersonCommand<Boolean> {
    private final AuthorityPersonPersistenceFacade persistence;

    public DeleteAuthorityPersonCommand(AuthorityPersonPersistenceFacade persistence) {
        super(AuthorityPerson.class);
        this.persistence = persistence;
    }

    /**
     * To delete authority person by id
     *
     * @param parameter system authority-person-id
     * @return execution's result
     * @deprecated commands are going to work through redo/undo
     */
    @Deprecated(forRemoval = true)
    @Override
    public CommandResult<Boolean> execute(Object parameter) {
        try {
            log.debug("Trying to delete authority person with ID: {}", parameter);
            Long id = commandParameter(parameter);
            Optional<AuthorityPerson> person = persistence.findAuthorityPersonById(id);
            if (person.isEmpty()) {
                return CommandResult.<Boolean>builder()
                        .result(Optional.of(false))
                        .exception(new NotExistAuthorityPersonException("AuthorityPerson with ID:" + id + " is not exists."))
                        .success(false).build();
            } else if (!person.get().getFaculties().isEmpty()) {
                return CommandResult.<Boolean>builder()
                        .result(Optional.of(false))
                        .exception(new AuthorityPersonManageFacultyException("AuthorityPerson with ID:" + id + " is managing faculties."))
                        .success(false).build();
            }

            persistence.deleteAuthorityPerson(id);

            log.debug("Deleted authority person {} {}", person.get(), true);
            return CommandResult.<Boolean>builder()
                    .result(Optional.of(true))
                    .success(true)
                    .build();
        } catch (Exception e) {
            log.error("Cannot delete the authority person by ID:{}", parameter, e);
            return CommandResult.<Boolean>builder()
                    .result(Optional.of(false))
                    .exception(e).success(false).build();
        }
    }

    /**
     * DO: To delete authority person by id<BR/>
     * To execute command redo with correct context state
     *
     * @param context context of redo execution
     * @see Context
     * @see Context#getRedoParameter()
     * @see Context.State#WORK
     * @see this#retrieveEntity(Long, LongFunction, UnaryOperator, Supplier)
     * @see this#rollbackCachedEntity(Context, Function)
     * @see AuthorityPersonPersistenceFacade#findAuthorityPersonById(Long)
     * @see AuthorityPersonPersistenceFacade#toEntity(AuthorityPerson)
     * @see AuthorityPersonPersistenceFacade#deleteAuthorityPerson(Long)
     */
    @Override
    public void executeDo(Context<?> context) {
        final Object parameter = context.getRedoParameter();
        try {
            log.debug("Trying to delete authority person using: {}", parameter);
            final Long id = commandParameter(parameter);
            final var notFoundException = new NotExistAuthorityPersonException(PERSON_WITH_ID_PREFIX + id + " is not exists.");
            if (PersistenceFacadeUtilities.isInvalidId(id)) {
                throw notFoundException;
            }

            final AuthorityPerson entity =
                    retrieveEntity(id, persistence::findAuthorityPersonById, persistence::toEntity, () -> notFoundException);

            if (!entity.getFaculties().isEmpty()) {
                throw new AuthorityPersonManageFacultyException(PERSON_WITH_ID_PREFIX + id + " is managing faculties.");
            }
            // cached authority person is storing to context for further rollback (undo)
            context.setUndoParameter(entity);
            // deleting entity
            persistence.deleteAuthorityPerson(id);
            context.setResult(true);
            log.debug("Deleted authority person with ID: {} successfully.", id);
        } catch (Exception e) {
            rollbackCachedEntity(context, persistence::save);
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
            log.debug("Trying to undo authority person deletion using: {}", parameter);

            rollbackCachedEntity(context, persistence::save);

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
}
