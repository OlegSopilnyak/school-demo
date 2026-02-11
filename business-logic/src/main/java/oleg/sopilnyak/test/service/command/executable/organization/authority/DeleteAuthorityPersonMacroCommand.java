package oleg.sopilnyak.test.service.command.executable.organization.authority;

import static java.util.Objects.isNull;

import oleg.sopilnyak.test.school.common.business.facade.organization.AuthorityPersonFacade;
import oleg.sopilnyak.test.school.common.business.facade.profile.PrincipalProfileFacade;
import oleg.sopilnyak.test.school.common.exception.organization.AuthorityPersonNotFoundException;
import oleg.sopilnyak.test.school.common.model.organization.AuthorityPerson;
import oleg.sopilnyak.test.school.common.model.person.profile.PrincipalProfile;
import oleg.sopilnyak.test.school.common.persistence.organization.AuthorityPersonPersistenceFacade;
import oleg.sopilnyak.test.service.command.executable.core.executor.CommandActionExecutor;
import oleg.sopilnyak.test.service.command.executable.profile.principal.DeletePrincipalProfileCommand;
import oleg.sopilnyak.test.service.command.executable.core.MacroCommand;
import oleg.sopilnyak.test.service.command.executable.core.ParallelMacroCommand;
import oleg.sopilnyak.test.service.command.executable.core.SequentialMacroCommand;
import oleg.sopilnyak.test.service.command.io.Input;
import oleg.sopilnyak.test.service.command.type.core.CompositeCommand;
import oleg.sopilnyak.test.service.command.type.core.Context;
import oleg.sopilnyak.test.service.command.type.core.RootCommand;
import oleg.sopilnyak.test.service.command.type.core.nested.PrepareNestedContextVisitor;
import oleg.sopilnyak.test.service.command.type.organization.AuthorityPersonCommand;
import oleg.sopilnyak.test.service.command.type.profile.PrincipalProfileCommand;
import oleg.sopilnyak.test.service.command.type.profile.StudentProfileCommand;
import oleg.sopilnyak.test.service.exception.CannotCreateCommandContextException;

import java.util.Deque;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import lombok.extern.slf4j.Slf4j;

/**
 * Command-Implementation: command to delete the authority person and the profile assigned to the person
 *
 * @see AuthorityPerson
 * @see PrincipalProfile
 * @see ParallelMacroCommand
 * @see DeleteAuthorityPersonCommand
 * @see DeletePrincipalProfileCommand
 * @see AuthorityPersonPersistenceFacade
 */
@Slf4j
@Component(AuthorityPersonCommand.Component.DELETE_ALL)
public class DeleteAuthorityPersonMacroCommand extends ParallelMacroCommand<Boolean>
        implements MacroDeleteAuthorityPerson<Boolean> {
    // beans factory to prepare the current command for transactional operations
    private transient BeanFactory applicationContext;
    @Autowired
    public final void setApplicationContext(BeanFactory applicationContext) {
        this.applicationContext = applicationContext;
    }
    // persistence facade for get instance of authority person by person-id (creat-context phase)
    private final transient AuthorityPersonPersistenceFacade persistence;
    // reference to current command for transactional operations
    private final AtomicReference<MacroDeleteAuthorityPerson<Boolean>> self = new AtomicReference<>(null);

    /**
     * Reference to the current command for transactional operations
     *
     * @return reference to the current command from spring beans factory
     * @see PrepareNestedContextVisitor#prepareContext(StudentProfileCommand, Input)
     * @see this#createPrincipalProfileContext(PrincipalProfileCommand, Long)
     */
    private MacroDeleteAuthorityPerson<Boolean> transactional() {
        synchronized (MacroDeleteAuthorityPerson.class) {
            if (isNull(self.get())) {
                // getting command instance reference, which can be used for transactional operations
                // actually it's proxy of the command with transactional executeDo/executeUndo methods
                final String springName = Component.DELETE_ALL;
                final Class<MacroDeleteAuthorityPerson<Boolean>> familyType = commandFamily();
                getLog().debug("Getting command from family:{} bean-name:{}",familyType.getSimpleName(), springName);
                self.getAndSet(applicationContext.getBean(springName, familyType));
            }
        }
        return self.get();
    }

    /**
     * To get unique command-id for the command
     *
     * @return value of command-id
     */
    @Override
    public String getId() {
        return AuthorityPersonFacade.DELETE_MACRO;
    }

    /**
     * Reference to the current command for operations with the command's entities in transaction possibility<BR/>
     * Not needed transaction for this command
     *
     * @return the reference to the current command from spring beans factory
     * @see RootCommand#self()
     * @see RootCommand#doCommand(Context)
     * @see RootCommand#undoCommand(Context)
     */
    @Override
    public MacroDeleteAuthorityPerson<Boolean> self() {
        return this;
    }

    public DeleteAuthorityPersonMacroCommand(
            @Qualifier(AuthorityPersonCommand.Component.DELETE) AuthorityPersonCommand<?> personCommand,
            @Qualifier(PrincipalProfileCommand.Component.DELETE_BY_ID) PrincipalProfileCommand<?> profileCommand,
            @Qualifier(EXECUTOR_BEAN_NAME) Executor executor,
            AuthorityPersonPersistenceFacade persistence, CommandActionExecutor actionExecutor
    ) {
        super(actionExecutor, executor);
        this.persistence = persistence;
        super.putToNest(personCommand);
        super.putToNest(profileCommand);
    }

    /**
     * To get final main do-command result from nested command-contexts
     *
     * @param contexts nested command-contexts
     * @return the command result's value
     * @see MacroCommand#afterExecutionProcessing(Context, Deque, Deque, Deque)
     */
    @SuppressWarnings("unchecked")
    @Override
    public Boolean finalCommandResult(Deque<Context<?>> contexts) {
        return contexts.stream()
                .map(nested -> ((Context<Boolean>) nested).getResult().orElse(false))
                .reduce(Boolean.TRUE, Boolean::logicalAnd);
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

    /**
     * To prepare context for particular type of the nested command
     *
     * @param command   nested command instance
     * @param mainInput macro-command input parameter
     * @param <N>       type of delete principal profile nested command result
     * @return built context of the command for input parameter
     * @see Input
     * @see AuthorityPerson
     * @see PrincipalProfileCommand
     * @see DeleteAuthorityPersonMacroCommand#createPrincipalProfileContext(PrincipalProfileCommand, Long)
     * @see Context
     */
    @Override
    public <N> Context<N> prepareContext(final PrincipalProfileCommand<N> command, final Input<?> mainInput) {
        return mainInput.value() instanceof Long personId
                &&
                PrincipalProfileFacade.DELETE_BY_ID.equals(command.getId())
                // creating nested command-context in the transaction
                ? transactional().createPrincipalProfileContext(command, personId)
                // cannot create the nested command-context
                : cannotCreateNestedContextFor(command);
    }

    /**
     * To create context for delete person profile command
     *
     * @param command  delete principal person profile command instance
     * @param personId related person-id value
     * @param <N>      type of delete principal profile nested command result
     * @return built context of the command for input parameter
     * @see AuthorityPersonPersistenceFacade#findAuthorityPersonById(Long)
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public <N> Context<N> createPrincipalProfileContext(PrincipalProfileCommand<N> command, Long personId) {
        final Long profileId = persistence.findAuthorityPersonById(personId).map(AuthorityPerson::getProfileId)
                .orElseThrow(() -> personNotExistsException(personId));
        return command.createContext(Input.of(profileId));
    }

// for command activities as nested command

    /**
     * To prepare context for nested command using the visitor
     *
     * @param visitor             visitor of prepared contexts
     * @param macroInputParameter Macro-Command call's input parameter
     * @return prepared for nested command context
     * @see PrepareNestedContextVisitor#prepareContext(SequentialMacroCommand, Input)
     * @see PrepareNestedContextVisitor#prepareContext(ParallelMacroCommand, Input)
     * @see CompositeCommand#createContext(Input)
     */
    @Override
    public Context<Boolean> acceptPreparedContext(final PrepareNestedContextVisitor visitor, final Input<?> macroInputParameter) {
        return MacroDeleteAuthorityPerson.super.acceptPreparedContext(visitor, macroInputParameter);
    }

    // private methods
    private static <T> Context<T> cannotCreateNestedContextFor(RootCommand<T> command) {
        throw new CannotCreateCommandContextException(command.getId());
    }

    private static AuthorityPersonNotFoundException personNotExistsException(Long personId) {
        return new AuthorityPersonNotFoundException(PERSON_WITH_ID_PREFIX + personId + " is not exists.");
    }
}
