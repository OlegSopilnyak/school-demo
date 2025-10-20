package oleg.sopilnyak.test.service.command.executable.organization.authority;

import java.util.Deque;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import oleg.sopilnyak.test.school.common.exception.organization.AuthorityPersonNotFoundException;
import oleg.sopilnyak.test.school.common.model.AuthorityPerson;
import oleg.sopilnyak.test.school.common.model.PrincipalProfile;
import oleg.sopilnyak.test.school.common.persistence.organization.AuthorityPersonPersistenceFacade;
import oleg.sopilnyak.test.service.command.executable.ActionExecutor;
import oleg.sopilnyak.test.service.command.executable.profile.principal.DeletePrincipalProfileCommand;
import oleg.sopilnyak.test.service.command.executable.sys.MacroCommand;
import oleg.sopilnyak.test.service.command.executable.sys.LegacyParallelMacroCommand;
import oleg.sopilnyak.test.service.command.executable.sys.SequentialMacroCommand;
import oleg.sopilnyak.test.service.command.io.Input;
import oleg.sopilnyak.test.service.command.type.base.CompositeCommand;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.command.type.base.RootCommand;
import oleg.sopilnyak.test.service.command.type.nested.PrepareNestedContextVisitor;
import oleg.sopilnyak.test.service.command.type.organization.AuthorityPersonCommand;
import oleg.sopilnyak.test.service.command.type.profile.PrincipalProfileCommand;
import oleg.sopilnyak.test.service.exception.CannotCreateCommandContextException;
import oleg.sopilnyak.test.service.mapper.BusinessMessagePayloadMapper;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.SchedulingTaskExecutor;
import org.springframework.stereotype.Component;

/**
 * Command-Implementation: command to delete the authority person and the profile assigned to the person
 *
 * @see AuthorityPerson
 * @see PrincipalProfile
 * @see LegacyParallelMacroCommand
 * @see DeleteAuthorityPersonCommand
 * @see DeletePrincipalProfileCommand
 * @see AuthorityPersonPersistenceFacade
 */
@Slf4j
@Component("authorityPersonMacroDelete")
public class DeleteAuthorityPersonMacroCommand extends LegacyParallelMacroCommand<Boolean>
        implements AuthorityPersonCommand<Boolean> {
    // executor of parallel nested commands
    private final SchedulingTaskExecutor executor;
    // persistence facade for get instance of authority person by person-id (creat-context phase)
    private final transient AuthorityPersonPersistenceFacade persistence;
    @Getter
    private final transient BusinessMessagePayloadMapper payloadMapper = null;

    public DeleteAuthorityPersonMacroCommand(@Qualifier("authorityPersonDelete") AuthorityPersonCommand<?> personCommand,
                                             @Qualifier("profilePrincipalDelete") PrincipalProfileCommand<?> profileCommand,
                                             @Qualifier("parallelCommandNestedCommandsExecutor") SchedulingTaskExecutor executor,
                                             final AuthorityPersonPersistenceFacade persistence,
                                             final ActionExecutor actionExecutor) {
        super(actionExecutor);
        this.executor = executor;
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
     * To get access to command's command-context executor
     *
     * @return instance of executor
     */
    @Override
    public SchedulingTaskExecutor getExecutor() {
        return executor;
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
     * @see PrepareNestedContextVisitor#prepareContext(SequentialMacroCommand, Input)
     * @see PrepareNestedContextVisitor#prepareContext(LegacyParallelMacroCommand, Input)
     * @see CompositeCommand#createContext(Input)
     */
    @Override
    public Context<Boolean> acceptPreparedContext(final PrepareNestedContextVisitor visitor, final Input<?> macroInputParameter) {
        return AuthorityPersonCommand.super.acceptPreparedContext(visitor, macroInputParameter);
    }

    // private methods
    private static <T> Context<T> cannotCreateNestedContextFor(RootCommand<T> command) {
        throw new CannotCreateCommandContextException(command.getId());
    }

}
