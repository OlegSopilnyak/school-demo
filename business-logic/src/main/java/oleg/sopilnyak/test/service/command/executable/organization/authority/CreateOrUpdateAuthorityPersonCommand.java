package oleg.sopilnyak.test.service.command.executable.organization.authority;

import lombok.extern.slf4j.Slf4j;
import oleg.sopilnyak.test.school.common.exception.NotExistAuthorityPersonException;
import oleg.sopilnyak.test.school.common.model.AuthorityPerson;
import oleg.sopilnyak.test.school.common.persistence.organization.AuthorityPersonPersistenceFacade;
import oleg.sopilnyak.test.school.common.persistence.utility.PersistenceFacadeUtilities;
import oleg.sopilnyak.test.service.command.executable.sys.CommandResult;
import oleg.sopilnyak.test.service.command.type.AuthorityPersonCommand;
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
        implements AuthorityPersonCommand<Optional<AuthorityPerson>> {
    private final AuthorityPersonPersistenceFacade persistence;

    public CreateOrUpdateAuthorityPersonCommand(AuthorityPersonPersistenceFacade persistence) {
        super(AuthorityPerson.class);
        this.persistence = persistence;
    }

    /**
     * To create or update authority person instance
     *
     * @param parameter authority person instance to save
     * @return execution's result
     * @deprecated commands are going to work through redo/undo
     */
    @Deprecated(forRemoval = true)
    @Override
    public CommandResult<Optional<AuthorityPerson>> execute(Object parameter) {
        try {
            log.debug("Trying to create or update authority person {}", parameter);
            AuthorityPerson instance = commandParameter(parameter);
            Optional<AuthorityPerson> person = persistence.save(instance);
            log.debug("Got stored authority person {} from parameter {}", person, instance);
            return CommandResult.<Optional<AuthorityPerson>>builder()
                    .result(Optional.ofNullable(person))
                    .success(true)
                    .build();
        } catch (Exception e) {
            log.error("Cannot create or update authority person by ID:{}", parameter, e);
            return CommandResult.<Optional<AuthorityPerson>>builder()
                    .result(Optional.of(Optional.empty()))
                    .exception(e).success(false).build();
        }
    }

    /**
     * DO: To create or update authority person instance<BR/>
     * To execute command redo with correct context state
     *
     * @param context context of redo execution
     * @see Context
     * @see Context#getRedoParameter()
     * @see Context.State#WORK
     * @see this#retrieveEntity(Long, LongFunction, UnaryOperator, Supplier)
     * @see this#persistRedoEntity(Context, Function)
     * @see this#rollbackCachedEntity(Context, Function)
     * @see AuthorityPersonPersistenceFacade#findAuthorityPersonById(Long)
     * @see AuthorityPersonPersistenceFacade#toEntity(AuthorityPerson)
     * @see AuthorityPersonPersistenceFacade#save(AuthorityPerson)
     * @see NotExistAuthorityPersonException
     */
    @Override
    public void executeDo(Context<?> context) {
        final Object parameter = context.getRedoParameter();
        try {
            check(parameter);
            log.debug("Trying to create or update authority person {}", parameter);
            final Long id = ((AuthorityPerson) parameter).getId();
            final boolean isCreateEntity = PersistenceFacadeUtilities.isInvalidId(id);
            if (!isCreateEntity) {
                // cached authority person is storing to context for further rollback (undo)
                context.setUndoParameter(
                        retrieveEntity(id, persistence::findAuthorityPersonById, persistence::toEntity,
                                () -> new NotExistAuthorityPersonException(PERSON_WITH_ID_PREFIX + id + " is not exists.")
                        )
                );
            }

            final Optional<AuthorityPerson> persisted = persistRedoEntity(context, persistence::save);
            // checking execution context state
            afterPersistCheck(context, () -> rollbackCachedEntity(context, persistence::save), persisted, isCreateEntity);
        } catch (Exception e) {
            rollbackCachedEntity(context, persistence::save);
            log.error("Cannot create or update authority person '{}'", parameter, e);
            context.failed(e);
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
    public void executeUndo(Context<?> context) {
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
