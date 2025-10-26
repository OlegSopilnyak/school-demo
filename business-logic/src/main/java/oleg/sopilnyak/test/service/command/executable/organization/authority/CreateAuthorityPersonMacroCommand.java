package oleg.sopilnyak.test.service.command.executable.organization.authority;

import oleg.sopilnyak.test.school.common.model.AuthorityPerson;
import oleg.sopilnyak.test.school.common.model.PrincipalProfile;
import oleg.sopilnyak.test.service.command.executable.ActionExecutor;
import oleg.sopilnyak.test.service.command.executable.sys.CommandContext;
import oleg.sopilnyak.test.service.command.executable.sys.ParallelMacroCommand;
import oleg.sopilnyak.test.service.command.executable.sys.SequentialMacroCommand;
import oleg.sopilnyak.test.service.command.io.Input;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.command.type.base.RootCommand;
import oleg.sopilnyak.test.service.command.type.nested.NestedCommand;
import oleg.sopilnyak.test.service.command.type.nested.PrepareNestedContextVisitor;
import oleg.sopilnyak.test.service.command.type.nested.TransferTransitionalResultVisitor;
import oleg.sopilnyak.test.service.command.type.organization.AuthorityPersonCommand;
import oleg.sopilnyak.test.service.command.type.profile.PrincipalProfileCommand;
import oleg.sopilnyak.test.service.exception.CannotCreateCommandContextException;
import oleg.sopilnyak.test.service.exception.CannotTransferCommandResultException;
import oleg.sopilnyak.test.service.exception.UnableExecuteCommandException;
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
@Component("authorityPersonMacroCreate")
public class CreateAuthorityPersonMacroCommand extends SequentialMacroCommand<Optional<AuthorityPerson>>
        implements AuthorityPersonCommand<Optional<AuthorityPerson>> {
    @Getter
    private final transient BusinessMessagePayloadMapper payloadMapper;

    @Value("${school.mail.basic.domain:gmail.com}")
    private String emailDomain;

    public CreateAuthorityPersonMacroCommand(@Qualifier("authorityPersonUpdate") AuthorityPersonCommand<?> personCommand,
                                             @Qualifier("profilePrincipalUpdate") PrincipalProfileCommand<?> profileCommand,
                                             final BusinessMessagePayloadMapper payloadMapper,
                                             final ActionExecutor actionExecutor) {
        super(actionExecutor);
        this.putToNest(profileCommand);
        this.putToNest(personCommand);
        this.payloadMapper = payloadMapper;

    }


    /**
     * To get final main do-command result from nested command-contexts
     *
     * @param contexts nested command-contexts
     * @return the command result's value
     * @see oleg.sopilnyak.test.service.command.executable.sys.MacroCommand#afterExecutionProcessing(Context, Deque, Deque, Deque)
     */
    @SuppressWarnings("unchecked")
    @Override
    public Optional<AuthorityPerson> finalCommandResult(Deque<Context<?>> contexts) {
        return contexts.stream()
                .filter(c -> c.getCommand() instanceof PersonInSequenceCommand)
                .map(c -> (Context<Optional<AuthorityPerson>>) c).findFirst()
                .flatMap(c -> c.getResult().orElseGet(Optional::empty));
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
        return CREATE_NEW;
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
        return macroInputParameter.value() instanceof AuthorityPerson person && AuthorityPersonCommand.CREATE_OR_UPDATE.equals(command.getId()) ?
                createPersonContext(command, person) : cannotCreateNestedContextFor(command);
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
        final AuthorityPersonPayload payload =
                person instanceof AuthorityPersonPayload personPayload ? personPayload : payloadMapper.toPayload(person);
        // prepare entity for create person sequence
        payload.setId(null);
        // create command-context with parameter by default
        return command.createContext(Input.of(payload));
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
        return macroInputParameter.value() instanceof AuthorityPerson person && PrincipalProfileCommand.CREATE_OR_UPDATE.equals(command.getId()) ?
                createProfileContext(command, person) : cannotCreateNestedContextFor(command);
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
        final PrincipalProfilePayload payload = PrincipalProfilePayload.builder()
                .id(null).phone("Not-Exists-Yet").email(emailPrefix + "@" + emailDomain)
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

    /**
     * To prepare command for sequential macro-command
     *
     * @param command nested command to wrap
     * @return wrapped nested command
     * @see SequentialMacroCommand#putToNest(NestedCommand)
     */
    @Override
    public NestedCommand<?> wrap(final NestedCommand<?> command) {
        if (command instanceof AuthorityPersonCommand<?> personCommand) {
            return wrap(personCommand);
        } else if (command instanceof PrincipalProfileCommand<?> profileCommand) {
            return wrap(profileCommand);
        }
        throw new UnableExecuteCommandException(command.getId());
    }

// for command do activities as nested command

    /**
     * To transfer result from current command to next command context.<BR/>
     * Send create-profile command result (profile-id) to create-person command input
     *
     * @param command successfully executed command
     * @param result  the result of successful command execution
     * @param target  next command context to execute command's redo
     * @see PrincipalProfileCommand#doCommand(Context)
     * @see oleg.sopilnyak.test.service.command.executable.sys.CommandContext#setRedoParameter(Input)
     * @see SequentialMacroCommand#executeNested(Deque, Context.StateChangedListener)
     * @see CreateAuthorityPersonMacroCommand#transferProfileIdToAuthorityPersonInput(Long, Context)
     * @see AuthorityPersonPayload#setProfileId(Long)
     * @see CannotTransferCommandResultException
     */
    @Override
    public <S, T> void transferPreviousExecuteDoResult(final PrincipalProfileCommand<?> command,
                                                       @NonNull final S result,
                                                       @NonNull final Context<T> target) {
        if (result instanceof Optional<?> opt && opt.orElseThrow() instanceof PrincipalProfile profile
            && AuthorityPersonCommand.CREATE_OR_UPDATE.equals(target.getCommand().getId())) {
            // send create-profile result (profile-id) to create-person input (AuthorityPersonPayload#setProfileId)
            transferProfileIdToAuthorityPersonInput(profile.getId(), target);
        } else {
            throw new CannotTransferCommandResultException(command.getId());
        }
    }

    /**
     * To transfer profile-id to create-person command input
     *
     * @param profileId the id of profile created before person
     * @param target    create-person command context
     * @see Context#getRedoParameter()
     * @see oleg.sopilnyak.test.service.command.executable.sys.CommandContext#setRedoParameter(Input)
     * @see AuthorityPersonPayload#setProfileId(Long)
     */
    public void transferProfileIdToAuthorityPersonInput(final Long profileId, @NonNull final Context<?> target) {
        final AuthorityPersonPayload personPayload = target.<AuthorityPersonPayload>getRedoParameter().value();
        log.debug("Transferring profile id: {} to person: {}", profileId, personPayload);
        personPayload.setProfileId(profileId);
        if (target instanceof CommandContext<?> commandContext) {
            commandContext.setRedoParameter(Input.of(personPayload));
            log.debug("Transferred to student changed input parameter: {}", personPayload);
        }
    }

    /**
     * To prepare context for nested command using the visitor
     *
     * @param visitor               visitor of prepared contexts
     * @param commandInputParameter Macro-Command call's input parameter
     * @return prepared for nested command context
     * @see PrepareNestedContextVisitor#prepareContext(SequentialMacroCommand, Input)
     * @see PrepareNestedContextVisitor#prepareContext(ParallelMacroCommand, Input)
     * @see oleg.sopilnyak.test.service.command.type.base.CompositeCommand#createContext(Input)
     */
    @Override
    public Context<Optional<AuthorityPerson>> acceptPreparedContext(final PrepareNestedContextVisitor visitor, final Input<?> commandInputParameter) {
        return AuthorityPersonCommand.super.acceptPreparedContext(visitor, commandInputParameter);
    }

    // private methods
    private NestedCommand<?> wrap(final AuthorityPersonCommand<?> command) {
        return new PersonInSequenceCommand(command);
    }

    private NestedCommand<?> wrap(final PrincipalProfileCommand<?> command) {
        return new ProfileInSequenceCommand(command);
    }

    private static <T> Context<T> cannotCreateNestedContextFor(RootCommand<T> command) {
        throw new CannotCreateCommandContextException(command.getId());
    }

    // inner classes
    private static class PersonInSequenceCommand extends SequentialMacroCommand.Chained<AuthorityPersonCommand<?>>
            implements AuthorityPersonCommand<Void> {
        private final AuthorityPersonCommand<?> wrappedCommand;

        private PersonInSequenceCommand(AuthorityPersonCommand<?> concreteCommand) {
            this.wrappedCommand = concreteCommand;
        }

        @Override
        public AuthorityPersonCommand<?> unWrap() {
            return wrappedCommand;
        }

        /**
         * To transfer nested command execution result to target nested command context input<BR/>
         * Not used in the main command, used default implementation
         *
         * @param visitor visitor for do transferring result from source to target
         * @param value   result of source command execution
         * @param target  nested command context for the next execution in sequence
         * @param <S>     type of source command execution result
         * @param <N>     type of target command execution result
         * @see TransferTransitionalResultVisitor#transferPreviousExecuteDoResult(AuthorityPersonCommand, Object, Context)
         */
        @Override
        public <S, N> void transferResultTo(TransferTransitionalResultVisitor visitor, S value, Context<N> target) {
            visitor.transferPreviousExecuteDoResult(wrappedCommand, value, target);
        }

        @Override
        public Logger getLog() {
            return wrappedCommand.getLog();
        }

        @Override
        public String getId() {
            return wrappedCommand.getId();
        }

        /**
         * To get mapper for business-message-payload
         *
         * @return mapper instance
         * @see BusinessMessagePayloadMapper
         */
        @Override
        public BusinessMessagePayloadMapper getPayloadMapper() {
            return null;
        }

        /**
         * To execute command with correct context state
         *
         * @param context context of redo execution
         * @see Context
         * @see Context.State#WORK
         * @see Context.State#DONE
         * @see RootCommand#doCommand(Context)
         */
        @Override
        @SuppressWarnings("unchecked")
        public void doCommand(Context context) {
            wrappedCommand.doCommand(context);
        }

        /**
         * To rollback command's execution according to command context
         *
         * @param context context of undo execution
         * @see Context
         * @see Context.State#WORK
         * @see Context.State#UNDONE
         * @see RootCommand#undoCommand(Context)
         */
        @Override
        public void undoCommand(Context<?> context) {
            wrappedCommand.undoCommand(context);
        }
    }

    private static class ProfileInSequenceCommand extends SequentialMacroCommand.Chained<PrincipalProfileCommand<?>>
            implements PrincipalProfileCommand<Void> {
        private final PrincipalProfileCommand<?> wrappedCommand;

        private ProfileInSequenceCommand(PrincipalProfileCommand<?> concreteCommand) {
            this.wrappedCommand = concreteCommand;
        }

        @Override
        public PrincipalProfileCommand<?> unWrap() {
            return wrappedCommand;
        }

        /**
         * To transfer nested command execution result to target nested command context input<BR/>
         * Used in the main command to pass profile-id to the input of create AuthorityPerson nested command
         *
         * @param visitor visitor for do transferring result from source to target
         * @param value   result of source command execution
         * @param target  nested command context for the next execution in sequence
         * @param <S>     type of source command execution result
         * @param <N>     type of target command execution result
         * @see TransferTransitionalResultVisitor#transferPreviousExecuteDoResult(RootCommand, Object, Context)
         * @see CreateAuthorityPersonMacroCommand#transferPreviousExecuteDoResult(PrincipalProfileCommand, Object, Context)
         */
        @Override
        public <S, N> void transferResultTo(final TransferTransitionalResultVisitor visitor, final S value, final Context<N> target) {
            visitor.transferPreviousExecuteDoResult(wrappedCommand, value, target);
        }

        @Override
        public Logger getLog() {
            return wrappedCommand.getLog();
        }

        @Override
        public String getId() {
            return wrappedCommand.getId();
        }

        /**
         * To detach command result data from persistence layer
         *
         * @param result result data to detach
         * @return detached result data
         * @see RootCommand#afterExecuteDo(Context)
         */
        @Override
        public Void detachedResult(Void result) {
            return null;
        }

        /**
         * To get mapper for business-message-payload
         *
         * @return mapper instance
         * @see BusinessMessagePayloadMapper
         */
        @Override
        public BusinessMessagePayloadMapper getPayloadMapper() {
            return null;
        }

        /**
         * To execute command with correct context state
         *
         * @param context context of redo execution
         * @see Context
         * @see Context.State#WORK
         * @see Context.State#DONE
         * @see RootCommand#doCommand(Context)
         */
        @Override
        @SuppressWarnings("unchecked")
        public void doCommand(Context context) {
            wrappedCommand.doCommand(context);
        }

        /**
         * To rollback command's execution according to command context
         *
         * @param context context of undo execution
         * @see Context
         * @see Context.State#WORK
         * @see Context.State#UNDONE
         * @see RootCommand#undoCommand(Context)
         */
        @Override
        public void undoCommand(Context<?> context) {
            wrappedCommand.undoCommand(context);
        }
    }

}
