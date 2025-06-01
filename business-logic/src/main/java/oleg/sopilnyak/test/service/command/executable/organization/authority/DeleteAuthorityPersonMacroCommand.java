package oleg.sopilnyak.test.service.command.executable.organization.authority;

import java.util.Deque;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import oleg.sopilnyak.test.school.common.exception.organization.AuthorityPersonNotFoundException;
import oleg.sopilnyak.test.school.common.model.AuthorityPerson;
import oleg.sopilnyak.test.school.common.model.PrincipalProfile;
import oleg.sopilnyak.test.school.common.persistence.organization.AuthorityPersonPersistenceFacade;
import oleg.sopilnyak.test.service.command.executable.profile.principal.DeletePrincipalProfileCommand;
import oleg.sopilnyak.test.service.command.executable.sys.MacroCommand;
import oleg.sopilnyak.test.service.command.executable.sys.ParallelMacroCommand;
import oleg.sopilnyak.test.service.command.executable.sys.SequentialMacroCommand;
import oleg.sopilnyak.test.service.command.io.Input;
import oleg.sopilnyak.test.service.command.type.CompositeCommand;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.command.type.base.RootCommand;
import oleg.sopilnyak.test.service.command.type.nested.NestedCommandExecutionVisitor;
import oleg.sopilnyak.test.service.command.type.nested.PrepareContextVisitor;
import oleg.sopilnyak.test.service.command.type.organization.AuthorityPersonCommand;
import oleg.sopilnyak.test.service.command.type.profile.PrincipalProfileCommand;
import oleg.sopilnyak.test.service.exception.CannotCreateCommandContextException;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.SchedulingTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

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
@Component
public class DeleteAuthorityPersonMacroCommand extends ParallelMacroCommand<Boolean>
        implements AuthorityPersonCommand<Boolean> {
    // executor of parallel nested commands
    private final ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    private final int maxPoolSize;
    // persistence facade for get instance of authority person by person-id (creat-context phase)
    private final AuthorityPersonPersistenceFacade persistence;

    public DeleteAuthorityPersonMacroCommand(
            final DeleteAuthorityPersonCommand deletePersonCommand,
            final DeletePrincipalProfileCommand deleteProfileCommand,
            final AuthorityPersonPersistenceFacade persistence,
            @Value("${school.parallel.max.pool.size:100}") final int maxPoolSize
    ) {
        this.maxPoolSize = maxPoolSize;
        this.persistence = persistence;
        super.putToNest(deletePersonCommand);
        super.putToNest(deleteProfileCommand);
    }

    /**
     * To get final main do-command result from nested command-contexts
     *
     * @param contexts nested command-contexts
     * @return the command result's value
     * @see oleg.sopilnyak.test.service.command.executable.sys.MacroCommand#postExecutionProcessing(Context, Deque, Deque, Deque)
     */
    @Override
    public Boolean finalCommandResult(Deque<Context<?>> contexts) {
        return contexts.stream()
                .map(nested -> ((Context<Boolean>) nested).getResult().orElse(false))
                .reduce(Boolean.TRUE, Boolean::logicalAnd);
    }

    /**
     * To get access to command's command-context executor
     *
     * @return instance of executor
     */
    @Override
    public SchedulingTaskExecutor getExecutor() {
        return executor;
    }

    /**
     * To prepare and run nested commands runner executor
     *
     * @see ThreadPoolTaskExecutor#initialize()
     * @see MacroCommand#fromNest()
     */
    @PostConstruct
    public void runThreadPoolExecutor() {
        executor.setCorePoolSize(fromNest().size());
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(maxPoolSize);
        executor.initialize();
    }

    /**
     * To shut down nested commands runner executor
     *
     * @see ThreadPoolTaskExecutor#shutdown()
     */
    @PreDestroy
    public void stopThreadPoolExecutor() {
        executor.shutdown();
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
     * To get unique command-id for the command
     *
     * @return value of command-id
     */
    @Override
    public String getId() {
        return DELETE_ALL;
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
        return mainInput.value() instanceof Long personId && PrincipalProfileCommand.DELETE_BY_ID.equals(command.getId()) ?
                createPrincipalProfileContext(command, personId) : cannotCreateNestedContextFor(command);
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
    public <N> Context<N> createPrincipalProfileContext(PrincipalProfileCommand<N> command, Long personId) {
        final Long profileId = persistence.findAuthorityPersonById(personId)
                .orElseThrow(() -> new AuthorityPersonNotFoundException(PERSON_WITH_ID_PREFIX + personId + " is not exists."))
                .getProfileId();
        return command.createContext(Input.of(profileId));
    }

// for command activities as nested command

    /**
     * To prepare context for nested command using the visitor
     *
     * @param visitor             visitor of prepared contexts
     * @param macroInputParameter Macro-Command call's input parameter
     * @return prepared for nested command context
     * @see PrepareContextVisitor#prepareContext(SequentialMacroCommand, Input)
     * @see PrepareContextVisitor#prepareContext(ParallelMacroCommand, Input)
     * @see MacroCommand#createContext(Input)
     */
    @Override
    public Context<Boolean> acceptPreparedContext(final PrepareContextVisitor visitor, final Input<?> macroInputParameter) {
        return super.acceptPreparedContext(visitor, macroInputParameter);
    }

    /**
     * To execute command Do as a nested command
     *
     * @param visitor       visitor to do nested command execution
     * @param context       context for nested command execution
     * @param stateListener listener of context-state-change
     * @see NestedCommandExecutionVisitor#doNestedCommand(RootCommand, Context, Context.StateChangedListener)
     * @see Context#addStateListener(Context.StateChangedListener)
     * @see CompositeCommand#doCommand(Context)
     * @see Context#removeStateListener(Context.StateChangedListener)
     * @see Context.StateChangedListener#stateChanged(Context, Context.State, Context.State)
     */
    @Override
    public void doAsNestedCommand(final NestedCommandExecutionVisitor visitor,
                                  final Context<?> context, final Context.StateChangedListener stateListener) {
        super.doAsNestedCommand(visitor, context, stateListener);
    }

    /**
     * To execute command Undo as a nested command
     *
     * @param visitor visitor to do nested command execution
     * @param context context for nested command execution
     * @see NestedCommandExecutionVisitor#undoNestedCommand(RootCommand, Context)
     * @see CompositeCommand#undoCommand(Context)
     */
    @Override
    public Context<?> undoAsNestedCommand(final NestedCommandExecutionVisitor visitor, final Context<?> context) {
        return super.undoAsNestedCommand(visitor, context);
    }

    private static <T> Context<T> cannotCreateNestedContextFor(RootCommand<T> command) {
        throw new CannotCreateCommandContextException(command.getId());
    }

}
