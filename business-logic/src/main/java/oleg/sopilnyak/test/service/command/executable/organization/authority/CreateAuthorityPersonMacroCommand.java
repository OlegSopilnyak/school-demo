package oleg.sopilnyak.test.service.command.executable.organization.authority;

import oleg.sopilnyak.test.school.common.model.AuthorityPerson;
import oleg.sopilnyak.test.school.common.model.PrincipalProfile;
import oleg.sopilnyak.test.service.command.executable.core.executor.CommandActionExecutor;
import oleg.sopilnyak.test.service.command.executable.core.ParallelMacroCommand;
import oleg.sopilnyak.test.service.command.executable.core.SequentialMacroCommand;
import oleg.sopilnyak.test.service.command.executable.core.context.CommandContext;
import oleg.sopilnyak.test.service.command.io.Input;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.command.type.base.RootCommand;
import oleg.sopilnyak.test.service.command.type.nested.PrepareNestedContextVisitor;
import oleg.sopilnyak.test.service.command.type.organization.AuthorityPersonCommand;
import oleg.sopilnyak.test.service.command.type.profile.PrincipalProfileCommand;
import oleg.sopilnyak.test.service.exception.CannotCreateCommandContextException;
import oleg.sopilnyak.test.service.exception.CannotTransferCommandResultException;
import oleg.sopilnyak.test.service.mapper.BusinessMessagePayloadMapper;
import oleg.sopilnyak.test.service.message.payload.AuthorityPersonPayload;
import oleg.sopilnyak.test.service.message.payload.PrincipalProfilePayload;

import java.security.NoSuchAlgorithmException;
import java.util.Deque;
import java.util.Optional;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Command-Implementation: command to create the authority person instance with related profile
 *
 * @see AuthorityPerson
 * @see PrincipalProfile
 * @see SequentialMacroCommand
 * @see AuthorityPersonCommand
 */
@Slf4j
@Component(AuthorityPersonCommand.Component.CREATE_NEW)
public class CreateAuthorityPersonMacroCommand extends SequentialMacroCommand<Optional<AuthorityPerson>>
        implements AuthorityPersonCommand<Optional<AuthorityPerson>> {
    @Getter
    private final transient BusinessMessagePayloadMapper payloadMapper;
    @Value("${school.mail.basic.domain:gmail.com}")
    private String emailDomain;


    /**
     * Reference to the current command for operations with the command's entities in transaction possibility<BR/>
     * Not needed transaction for this command
     *
     * @return the reference to the current command from spring beans factory (if transaction is used in the command)
     * @see RootCommand#self()
     * @see RootCommand#doCommand(Context)
     * @see RootCommand#undoCommand(Context)
     */
    @Override
    public AuthorityPersonCommand<Optional<AuthorityPerson>> self() {
        return this;
    }

    /**
     * To get unique command-id for the command
     *
     * @return value of command-id
     */
    @Override
    public String getId() {
        return AuthorityPersonCommand.CommandId.CREATE_NEW;
    }

    public CreateAuthorityPersonMacroCommand(
            @Qualifier(AuthorityPersonCommand.Component.CREATE_OR_UPDATE) AuthorityPersonCommand<?> personCommand,
            @Qualifier(PrincipalProfileCommand.Component.CREATE_OR_UPDATE) PrincipalProfileCommand<?> profileCommand,
            BusinessMessagePayloadMapper payloadMapper, CommandActionExecutor actionExecutor
    ) {
        super(actionExecutor);
        this.payloadMapper = payloadMapper;
        this.putToNest(profileCommand);
        this.putToNest(personCommand);

    }


    /**
     * To get final main do-command result from nested command-contexts
     *
     * @param contexts nested command-contexts
     * @return the command result's value
     * @see oleg.sopilnyak.test.service.command.executable.core.MacroCommand#afterExecutionProcessing(Context, Deque, Deque, Deque)
     */
    @SuppressWarnings("unchecked")
    @Override
    public Optional<AuthorityPerson> finalCommandResult(Deque<Context<?>> contexts) {
        return contexts.stream().filter(CreateAuthorityPersonMacroCommand::hasPerson)
                .map(context -> (Context<Optional<AuthorityPerson>>) context).findFirst()
                .flatMap(context -> context.getResult().orElseGet(Optional::empty));
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
     * @param command             nested command instance
     * @param macroInputParameter macro-command input parameter
     * @param <N>                 type of create-or-update person nested command result
     * @return built context of the command for input parameter
     * @see Input
     * @see AuthorityPerson
     * @see AuthorityPersonCommand
     * @see CreateAuthorityPersonMacroCommand#createPersonContext(AuthorityPersonCommand, AuthorityPerson)
     * @see Context
     */
    @Override
    public <N> Context<N> prepareContext(final AuthorityPersonCommand<N> command, final Input<?> macroInputParameter) {
        return macroInputParameter.value() instanceof AuthorityPerson person && isUpdatePersonCommand(command.getId())
                ? createPersonContext(command, person)
                : cannotCreateNestedContextFor(command);
    }

    /**
     * To prepare context for particular type of the nested command
     *
     * @param command             nested command instance
     * @param macroInputParameter macro-command input parameter
     * @param <N>                 type of create-or-update principal-profile nested command result
     * @return built context of the command for input parameter
     * @see Input
     * @see AuthorityPerson
     * @see AuthorityPersonCommand
     * @see CreateAuthorityPersonMacroCommand#createProfileContext(PrincipalProfileCommand, AuthorityPerson)
     * @see Context
     */
    @Override
    public <N> Context<N> prepareContext(final PrincipalProfileCommand<N> command, final Input<?> macroInputParameter) {
        return macroInputParameter.value() instanceof AuthorityPerson person && isUpdateProfileCommand(command.getId())
                ? createProfileContext(command, person)
                : cannotCreateNestedContextFor(command);
    }

    /**
     * To create context for create-or-update per command
     *
     * @param command create-or-update person command instance
     * @param person  input parameter of person to create
     * @param <N>     type of create-or-update person nested command result
     * @return built context of the command for input person
     * @see BusinessMessagePayloadMapper#toPayload(AuthorityPerson)
     * @see AuthorityPersonPayload
     */
    public <N> Context<N> createPersonContext(final AuthorityPersonCommand<N> command, final AuthorityPerson person) {
        final AuthorityPersonPayload payload = adoptEntity(person);
        // prepare entity for create person sequence
        payload.setId(null);
        // create command-context with parameter by default
        return command.createContext(Input.of(payload));
    }

    /**
     * To create context for create-or-update person profile command
     *
     * @param command create-or-update person profile command instance
     * @param person  input parameter of person to create
     * @param <N>     type of create-or-update principal-profile nested command result
     * @return built context of the command for input parameter
     * @see PrincipalProfilePayload
     */
    public <N> Context<N> createProfileContext(final PrincipalProfileCommand<N> command, final AuthorityPerson person) {
        final String emailPrefix = person.getFirstName().trim().toLowerCase() + "." + person.getLastName().trim().toLowerCase();
        final PrincipalProfilePayload payload = PrincipalProfilePayload.builder().id(null)
                .phone("Not-Exists-Yet").email(emailPrefix + "@" + emailDomain)
                .login(emailPrefix)
                .build();
        try {
            payload.setSignature(payload.makeSignatureFor(""));
        } catch (NoSuchAlgorithmException e) {
            log.error("Cannot make the signature for '{}'", command.getId(), e);
            throw new CannotCreateCommandContextException(command.getId(), e);
        }
        // create command-context with created parameter by default
        return command.createContext(Input.of(payload));
    }

// for command do activities as nested command

    /**
     * To transfer result of the previous command execution to the next command context
     *
     * @param executedCommand the command-owner of transferred result
     * @param result          result of previous command execution value
     * @param toExecute       the command context of the next command
     */
    @Override
    @SuppressWarnings("unchecked")
    public void transferResult(
            final RootCommand<?> executedCommand, final Object result, final Context<?> toExecute
    ) {
        if (!isUpdateProfile(executedCommand)) {
            return;
        }
        // transfer profile-update nested command result (profile-id) to person-update input
        final String commandId = toExecute.getCommand().getId();
        if (isOptionalProfile(result) && hasPerson(toExecute)) {
            log.debug("Transferring to context of '{}' the result {}", commandId, result);
            final Long profileId = ((Optional<PrincipalProfile>) result)
                    .orElseThrow(() -> new CannotTransferCommandResultException(commandId))
                    .getId();
            log.debug("Transferring profile-id:{} to context of '{}'", profileId, commandId);
            transferProfileIdToAuthorityPersonUpdateInput(profileId, toExecute);
        } else {
            log.error("Cannot transfer to context of '{}' the result {}", commandId, result);
            throw new CannotTransferCommandResultException(commandId);
        }
    }

    /**
     * To transfer profile-id to create-person command input
     *
     * @param profileId the id of profile created before person
     * @param target    create-person command context
     * @see Context#getRedoParameter()
     * @see CommandContext#setRedoParameter(Input)
     * @see AuthorityPersonPayload#setProfileId(Long)
     */
    public void transferProfileIdToAuthorityPersonUpdateInput(final Long profileId, @NonNull final Context<?> target) {
        final AuthorityPersonPayload personPayload = target.<AuthorityPersonPayload>getRedoParameter().value();
        log.debug("Transferring profile id: {} to person: {}", profileId, personPayload);
        personPayload.setProfileId(profileId);
        if (target instanceof CommandContext<?> commandContext) {
            commandContext.setRedoParameter(Input.of(personPayload));
            log.debug("Transferred to authority person change input parameter: {}", personPayload);
        } else {
            final Throwable cause = new IllegalStateException("Wrong type of command context");
            throw new CannotTransferCommandResultException(target.getCommand().getId(), cause);
        }
    }

    /**
     * To prepare context for nested command using the visitor
     *
     * @param visitor visitor of prepared contexts
     * @param input   Macro-Command call's input parameter
     * @return prepared for nested command context
     * @see PrepareNestedContextVisitor#prepareContext(SequentialMacroCommand, Input)
     * @see PrepareNestedContextVisitor#prepareContext(ParallelMacroCommand, Input)
     * @see oleg.sopilnyak.test.service.command.type.base.CompositeCommand#createContext(Input)
     */
    @Override
    public Context<Optional<AuthorityPerson>> acceptPreparedContext(
            final PrepareNestedContextVisitor visitor, final Input<?> input
    ) {
        return AuthorityPersonCommand.super.acceptPreparedContext(visitor, input);
    }

    // private methods
    // to check the command types
    // is update person command-id
    private static boolean isUpdatePersonCommand(final String commandId) {
        return AuthorityPersonCommand.CommandId.CREATE_OR_UPDATE.equals(commandId);
    }

    // is update profile command-id
    private static boolean isUpdateProfileCommand(final String commandId) {
        return PrincipalProfileCommand.CommandId.CREATE_OR_UPDATE.equals(commandId);
    }

    // is update profile nested command
    private static boolean isUpdateProfile(final RootCommand<?> nestedCommand) {
        return nestedCommand instanceof PrincipalProfileCommand && isUpdateProfileCommand(nestedCommand.getId());
    }

    // to check is result to transfer has necessary type
    private static boolean isOptionalProfile(final Object result) {
        return result instanceof Optional<?> optional
                && optional.isPresent() && optional.get() instanceof PrincipalProfile;
    }

    // to check is context for create or update the person
    private static boolean hasPerson(Context<?> context) {
        final RootCommand<?> command = context.getCommand();
        return command instanceof AuthorityPersonCommand<?> && CommandId.CREATE_OR_UPDATE.equals(command.getId());
    }

    private static <T> Context<T> cannotCreateNestedContextFor(RootCommand<T> command) {
        throw new CannotCreateCommandContextException(command.getId());
    }
}
