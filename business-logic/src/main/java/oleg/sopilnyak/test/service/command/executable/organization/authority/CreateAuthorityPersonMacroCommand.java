package oleg.sopilnyak.test.service.command.executable.organization.authority;

import lombok.extern.slf4j.Slf4j;
import oleg.sopilnyak.test.school.common.model.AuthorityPerson;
import oleg.sopilnyak.test.school.common.model.PrincipalProfile;
import oleg.sopilnyak.test.service.command.executable.profile.principal.CreateOrUpdatePrincipalProfileCommand;
import oleg.sopilnyak.test.service.command.executable.sys.CommandContext;
import oleg.sopilnyak.test.service.command.executable.sys.ParallelMacroCommand;
import oleg.sopilnyak.test.service.command.executable.sys.SequentialMacroCommand;
import oleg.sopilnyak.test.service.command.io.Input;
import oleg.sopilnyak.test.service.command.type.CompositeCommand;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.command.type.base.RootCommand;
import oleg.sopilnyak.test.service.command.type.nested.NestedCommand;
import oleg.sopilnyak.test.service.command.type.nested.NestedCommandExecutionVisitor;
import oleg.sopilnyak.test.service.command.type.nested.PrepareContextVisitor;
import oleg.sopilnyak.test.service.command.type.nested.TransferResultVisitor;
import oleg.sopilnyak.test.service.command.type.organization.AuthorityPersonCommand;
import oleg.sopilnyak.test.service.command.type.profile.PrincipalProfileCommand;
import oleg.sopilnyak.test.service.exception.CannotCreateCommandContextException;
import oleg.sopilnyak.test.service.exception.CannotTransferCommandResultException;
import oleg.sopilnyak.test.service.exception.UnableExecuteCommandException;
import oleg.sopilnyak.test.service.mapper.BusinessMessagePayloadMapper;
import oleg.sopilnyak.test.service.message.payload.AuthorityPersonPayload;
import oleg.sopilnyak.test.service.message.payload.PrincipalProfilePayload;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import java.security.NoSuchAlgorithmException;
import java.util.Deque;
import java.util.Optional;

/**
 * Command-Implementation: command to create the authority person instance with related profile
 *
 * @see AuthorityPerson
 * @see PrincipalProfile
 * @see SequentialMacroCommand
 * @see AuthorityPersonCommand
 */
@Slf4j
@Component
public class CreateAuthorityPersonMacroCommand extends SequentialMacroCommand<Optional<AuthorityPerson>>
        implements AuthorityPersonCommand<Optional<AuthorityPerson>> {

    private final BusinessMessagePayloadMapper payloadMapper;
    @Value("${school.mail.basic.domain:gmail.com}")
    private String emailDomain;

    public CreateAuthorityPersonMacroCommand(final CreateOrUpdateAuthorityPersonCommand personCommand,
                                             final CreateOrUpdatePrincipalProfileCommand profileCommand,
                                             final BusinessMessagePayloadMapper payloadMapper) {
        addToNest(profileCommand);
        addToNest(personCommand);
        this.payloadMapper = payloadMapper;
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
     * @param <N>     type of create-or-update person nested command result
     * @return built context of the command for input parameter
     * @see Input
     * @see AuthorityPerson
     * @see AuthorityPersonCommand
     * @see CreateAuthorityPersonMacroCommand#createPersonContext(AuthorityPersonCommand, AuthorityPerson)
     * @see Context
     */
    @Override
    public <N> Context<N> prepareContext(final AuthorityPersonCommand<N> command, final Input<?> macroInputParameter) {
        return macroInputParameter instanceof AuthorityPerson person && AuthorityPersonCommand.CREATE_OR_UPDATE.equals(command.getId()) ?
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
     * @param command   nested command instance
     * @param macroInputParameter macro-command input parameter
     * @param <N>     type of create-or-update principal-profile nested command result
     * @return built context of the command for input parameter
     * @see Input
     * @see AuthorityPerson
     * @see AuthorityPersonCommand
     * @see CreateAuthorityPersonMacroCommand#createProfileContext(PrincipalProfileCommand, AuthorityPerson)
     * @see Context
     */
    @Override
    public <N> Context<N> prepareContext(final PrincipalProfileCommand<N> command, final Input<?> macroInputParameter) {
        return macroInputParameter instanceof AuthorityPerson person && PrincipalProfileCommand.CREATE_OR_UPDATE.equals(command.getId()) ?
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
     * @see SequentialMacroCommand#addToNest(NestedCommand)
     */
    @Override
    public NestedCommand<?> wrap(final NestedCommand<?> command) {
        if (command instanceof AuthorityPersonCommand<?> personCommand) {
            return wrap(personCommand);
        } else if (command instanceof PrincipalProfileCommand<?> profileCommand) {
            return wrap(profileCommand);
        }
        throw new UnableExecuteCommandException(((RootCommand<?>) command).getId());
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
     * @see SequentialMacroCommand#doNestedCommands(Deque, Context.StateChangedListener)
     * @see CreateAuthorityPersonMacroCommand#transferProfileIdToStudentInput(Long, Context)
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
            transferProfileIdToStudentInput(profile.getId(), target);
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
    public void transferProfileIdToStudentInput(final Long profileId, @NonNull final Context<?> target) {
        final AuthorityPersonPayload personPayload = target.<AuthorityPersonPayload>getRedoParameter().value();
        log.debug("Transferring profile id: {} to person: {}", profileId, personPayload);
        personPayload.setProfileId(profileId);
        if (target instanceof CommandContext commandContext) {
            commandContext.setRedoParameter(Input.of(personPayload));
            log.debug("Transferred to student changed input parameter: {}", personPayload);
        }
//        target.setRedoParameter(personPayload);
    }

    /**
     * To prepare context for nested command using the visitor
     *
     * @param visitor               visitor of prepared contexts
     * @param commandInputParameter Macro-Command call's input parameter
     * @return prepared for nested command context
     * @see PrepareContextVisitor#prepareContext(SequentialMacroCommand, Input)
     * @see PrepareContextVisitor#prepareContext(ParallelMacroCommand, Input)
     * @see oleg.sopilnyak.test.service.command.executable.sys.MacroCommand#createContext(Input)
     */
    @Override
    public Context<Optional<AuthorityPerson>> acceptPreparedContext(final PrepareContextVisitor visitor, final Input<?> commandInputParameter) {
        return super.acceptPreparedContext(visitor, commandInputParameter);
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
        private final AuthorityPersonCommand<?> command;

        private PersonInSequenceCommand(AuthorityPersonCommand<?> command) {
            this.command = command;
        }

        @Override
        public AuthorityPersonCommand<?> unWrap() {
            return command;
        }

        /**
         * To transfer command execution result to next command context
         *
         * @param visitor     visitor for transfer result
         * @param resultValue result of command execution
         * @param target      command context for next execution
         * @see TransferResultVisitor#transferPreviousExecuteDoResult(RootCommand, Object, Context)
         * @see CommandContext#setRedoParameter(Input)
         */
        @Override
        public <S> void transferResultTo(TransferResultVisitor visitor, S resultValue, Context<?> target) {
            visitor.transferPreviousExecuteDoResult(command, resultValue, target);
        }

        @Override
        public Logger getLog() {
            return command.getLog();
        }

        @Override
        public String getId() {
            return command.getId();
        }

        @Override
        public void doAsNestedCommand(final NestedCommandExecutionVisitor visitor,
                                      final Context<?> context, final Context.StateChangedListener stateListener) {
            command.doAsNestedCommand(visitor, context, stateListener);
        }

        @Override
        public Context<?> undoAsNestedCommand(final NestedCommandExecutionVisitor visitor, final Context<?> context) {
            return unWrap().undoAsNestedCommand(visitor, context);
        }
    }

    private static class ProfileInSequenceCommand extends SequentialMacroCommand.Chained<PrincipalProfileCommand<?>>
            implements PrincipalProfileCommand<Void> {
        private final PrincipalProfileCommand<?> command;

        private ProfileInSequenceCommand(PrincipalProfileCommand<?> command) {
            this.command = command;
        }

        @Override
        public PrincipalProfileCommand<?> unWrap() {
            return command;
        }

        /**
         * To transfer command execution result to next command context
         *
         * @param visitor     visitor for transfer result
         * @param resultValue result of command execution
         * @param target      command context for next execution
         * @see TransferResultVisitor#transferPreviousExecuteDoResult(RootCommand, Object, Context)
         * @see CommandContext#setRedoParameter(Input)
         */
        @Override
        public <S> void transferResultTo(TransferResultVisitor visitor, S resultValue, Context<?> target) {
            visitor.transferPreviousExecuteDoResult(command, resultValue, target);
        }

        @Override
        public Logger getLog() {
            return command.getLog();
        }

        @Override
        public String getId() {
            return command.getId();
        }

        @Override
        public void doAsNestedCommand(final NestedCommandExecutionVisitor visitor,
                                      final Context<?> context, final Context.StateChangedListener stateListener) {
            command.doAsNestedCommand(visitor, context, stateListener);
        }

        @Override
        public Context<?> undoAsNestedCommand(final NestedCommandExecutionVisitor visitor, final Context<?> context) {
            return unWrap().undoAsNestedCommand(visitor, context);
        }
    }

}
