package oleg.sopilnyak.test.service.command.executable.organization.authority;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import oleg.sopilnyak.test.school.common.exception.organization.AuthorityPersonManagesFacultyException;
import oleg.sopilnyak.test.school.common.exception.EntityNotFoundException;
import oleg.sopilnyak.test.school.common.exception.organization.AuthorityPersonNotFoundException;
import oleg.sopilnyak.test.school.common.model.AuthorityPerson;
import oleg.sopilnyak.test.school.common.persistence.organization.AuthorityPersonPersistenceFacade;
import oleg.sopilnyak.test.school.common.persistence.utility.PersistenceFacadeUtilities;
import oleg.sopilnyak.test.service.command.executable.sys.cache.SchoolCommandCache;
import oleg.sopilnyak.test.service.command.executable.sys.context.CommandContext;
import oleg.sopilnyak.test.service.command.io.Input;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.command.type.base.RootCommand;
import oleg.sopilnyak.test.service.command.type.organization.AuthorityPersonCommand;
import oleg.sopilnyak.test.service.mapper.BusinessMessagePayloadMapper;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serial;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.LongFunction;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import static java.util.Objects.isNull;

/**
 * Command-Implementation: command to delete the authority person by id
 *
 * @see AuthorityPerson
 * @see AuthorityPersonCommand
 * @see AuthorityPersonPersistenceFacade
 * @see SchoolCommandCache
 */
@Slf4j
@Component("authorityPersonDelete")
public class DeleteAuthorityPersonCommand extends SchoolCommandCache<AuthorityPerson>
        implements AuthorityPersonCommand<Boolean> {
    @Serial
    private static final long serialVersionUID = -6678589218747269152L;
    // facade to manipulate AuthorityPerson entities
    private final transient AuthorityPersonPersistenceFacade persistence;
    @Getter
    private final transient BusinessMessagePayloadMapper payloadMapper;
    @Autowired
    // beans factory to prepare the current command for transactional operations
    private transient ApplicationContext applicationContext;
    // reference to current command for transactional operations
    private final AtomicReference<RootCommand<Boolean>> self;

    /**
     * Reference to the current command for transactional operations
     *
     * @return reference to the current command
     * @see RootCommand#self()
     * @see RootCommand#doCommand(Context)
     * @see RootCommand#undoCommand(Context)
     */
    @Override
    @SuppressWarnings("unchecked")
    public RootCommand<Boolean> self() {
        synchronized (AuthorityPersonCommand.class) {
            if (isNull(self.get())) {
                // getting command reference which can be used for transactional operations
                // actually it's proxy of the command with transactional executeDo method
                self.getAndSet(applicationContext.getBean("authorityPersonDelete", AuthorityPersonCommand.class));
            }
        }
        return self.get();
    }

    public DeleteAuthorityPersonCommand(final AuthorityPersonPersistenceFacade persistence,
                                        final BusinessMessagePayloadMapper payloadMapper) {
        super(AuthorityPerson.class);
        self = new  AtomicReference<>(null);
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
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void executeDo(Context<Boolean> context) {
        final Input<Long> parameter = context.getRedoParameter();
        try {
            checkNullParameter(parameter);
            log.debug("Trying to delete authority person using: {}", parameter);
            final Long id = parameter.value();
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
            prepareDeleteEntityUndo(context, entity, () -> exceptionFor(id));
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
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void executeUndo(Context<?> context) {
        final Input<?> parameter = context.getUndoParameter();
        try {
            checkNullParameter(parameter);
            log.debug("Trying to undo authority person deletion using: {}", parameter);

            final var entity = rollbackCachedEntity(context, persistence::save).orElseThrow();

            // change authority-person-id value for further do command action
            if (context instanceof CommandContext<?> commandContext) {
                commandContext.setRedoParameter(Input.of(entity.getId()));
            }
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
