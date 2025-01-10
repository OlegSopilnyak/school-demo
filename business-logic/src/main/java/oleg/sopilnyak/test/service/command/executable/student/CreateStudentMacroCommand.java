package oleg.sopilnyak.test.service.command.executable.student;

import lombok.extern.slf4j.Slf4j;
import oleg.sopilnyak.test.school.common.model.Student;
import oleg.sopilnyak.test.school.common.model.StudentProfile;
import oleg.sopilnyak.test.service.command.executable.profile.student.CreateOrUpdateStudentProfileCommand;
import oleg.sopilnyak.test.service.command.executable.sys.CommandContext;
import oleg.sopilnyak.test.service.command.executable.sys.ParallelMacroCommand;
import oleg.sopilnyak.test.service.command.executable.sys.SequentialMacroCommand;
import oleg.sopilnyak.test.service.command.io.Input;
import oleg.sopilnyak.test.service.command.type.CompositeCommand;
import oleg.sopilnyak.test.service.command.type.StudentCommand;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.command.type.base.RootCommand;
import oleg.sopilnyak.test.service.command.type.nested.NestedCommand;
import oleg.sopilnyak.test.service.command.type.nested.NestedCommandExecutionVisitor;
import oleg.sopilnyak.test.service.command.type.nested.PrepareContextVisitor;
import oleg.sopilnyak.test.service.command.type.nested.TransferResultVisitor;
import oleg.sopilnyak.test.service.command.type.profile.StudentProfileCommand;
import oleg.sopilnyak.test.service.exception.CannotCreateCommandContextException;
import oleg.sopilnyak.test.service.exception.CannotTransferCommandResultException;
import oleg.sopilnyak.test.service.exception.UnableExecuteCommandException;
import oleg.sopilnyak.test.service.mapper.BusinessMessagePayloadMapper;
import oleg.sopilnyak.test.service.message.payload.StudentPayload;
import oleg.sopilnyak.test.service.message.payload.StudentProfilePayload;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import java.util.Deque;
import java.util.Optional;

/**
 * Command-Implementation: command to create the student-person instance with related profile
 *
 * @see Student
 * @see StudentProfile
 * @see SequentialMacroCommand
 * @see StudentCommand
 */
@Slf4j
@Component
public class CreateStudentMacroCommand extends SequentialMacroCommand<Optional<Student>>
        implements StudentCommand<Optional<Student>> {
    private final BusinessMessagePayloadMapper payloadMapper;
    @Value("${school.mail.basic.domain:gmail.com}")
    private String emailDomain;

    public CreateStudentMacroCommand(final CreateOrUpdateStudentCommand personCommand,
                                     final CreateOrUpdateStudentProfileCommand profileCommand,
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
     * @param command   nested command instance
     * @param macroInputParameter macro-command input parameter
     * @param <N>     type of create-or-update student nested command result
     * @return built context of the command for input parameter
     * @see Student
     * @see StudentCommand
     * @see CreateStudentMacroCommand#createPersonContext(StudentCommand, Student)
     * @see Context
     */
    @Override
    public <N> Context<N> prepareContext(final StudentCommand<N> command, final Input<?> macroInputParameter) {
        return macroInputParameter.value() instanceof Student person && StudentCommand.CREATE_OR_UPDATE.equals(command.getId()) ?
                createPersonContext(command, person) : cannotCreateNestedContextFor(command);
    }

    /**
     * To create context for create-or-update person command
     *
     * @param command   create-or-update person command instance
     * @param person input parameter of person to create
     * @param <N>       type of create-or-update person command result
     * @return built context of the command for input person
     * @see BusinessMessagePayloadMapper#toPayload(Student)
     * @see StudentPayload
     */
    public <N> Context<N> createPersonContext(final StudentCommand<N> command, final Student person) {
        final StudentPayload payload =
                person instanceof StudentPayload personPayload ? personPayload : payloadMapper.toPayload(person);
        // prepare entity for create person sequence
        payload.setId(null);
        // create command-context with parameter by default
        return command.createContext(Input.of(payload));
    }

    /**
     * To prepare context for particular type of the nested command
     *
     * @param command   nested command instance
     * @param mainInput macro-command input parameter
     * @param <N>       type of create-or-update student profile command result
     * @return built context of the command for input parameter
     * @see Student
     * @see StudentProfileCommand
     * @see CreateStudentMacroCommand#createProfileContext(StudentProfileCommand, Student)
     * @see Context
     */
    @Override
    public <N> Context<N> prepareContext(@NonNull final StudentProfileCommand<N> command, final Input<?> mainInput) {
        return mainInput.value() instanceof Student person && StudentProfileCommand.CREATE_OR_UPDATE.equals(command.getId()) ?
                createProfileContext(command, person) : cannotCreateNestedContextFor(command);
    }

    /**
     * To create context for create-or-update person profile command
     *
     * @param command   create-or-update person profile command instance
     * @param person input parameter of person to create
     * @param <N>     type of create-or-update student-profile nested command result
     * @return built context of the command for input parameter
     * @see StudentProfilePayload
     */
    public <N> Context<N> createProfileContext(final StudentProfileCommand<N> command, final Student person) {
        final String emailPrefix = person.getFirstName().trim().toLowerCase() + "." + person.getLastName().trim().toLowerCase();
        final StudentProfilePayload payload = StudentProfilePayload.builder()
                .id(null).phone("Not-Exists-Yet").email(emailPrefix + "@" + emailDomain)
                .build();
        // create command-context with created parameter by default
        return command.createContext(Input.of(payload));
    }

// for command do activities as nested command

    /**
     * To transfer result from current command to next command context.<BR/>
     * Send create-profile command result (profile-id) to create-person command input
     *
     * @param command successfully executed command
     * @param result  the result of successful command execution
     * @param target  next command context to execute command's redo
     * @see StudentProfileCommand#doCommand(Context)
     * @see oleg.sopilnyak.test.service.command.executable.sys.CommandContext#setRedoParameter(Input)
     * @see SequentialMacroCommand#doNestedCommands(Deque, Context.StateChangedListener)
     * @see CreateStudentMacroCommand#transferProfileIdToStudentInput(Long, Context)
     * @see CannotTransferCommandResultException
     */
    @Override
    public <S, T> void transferPreviousExecuteDoResult(@NonNull final StudentProfileCommand<?> command,
                                                       @NonNull final S result,
                                                       @NonNull final Context<T> target) {
        if (result instanceof Optional<?> opt &&
                opt.orElseThrow() instanceof StudentProfile profile &&
                StudentCommand.CREATE_OR_UPDATE.equals(target.getCommand().getId())) {
            // send create-profile result (profile-id) to create-person input (StudentPayload#setProfileId)
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
     * @see StudentPayload#setProfileId(Long)
     */
    public void transferProfileIdToStudentInput(final Long profileId, @NonNull final Context<?> target) {
        final StudentPayload personPayload = target.<StudentPayload>getRedoParameter().value();
        log.debug("Transferring profile id: {} to person: {}", profileId, personPayload);
        personPayload.setProfileId(profileId);
        if (target instanceof CommandContext commandContext) {
            commandContext.setRedoParameter(Input.of(personPayload));
            log.debug("Transferred to student changed input parameter: {}", personPayload);
        }
//        personPayload.setProfileId(profileId);
//        target.setRedoParameter(personPayload);
    }

    /**
     * To prepare context for nested command using the visitor
     *
     * @param visitor visitor of prepared contexts
     * @param input   Macro-Command call's input
     * @return prepared for nested command context
     * @see PrepareContextVisitor#prepareContext(SequentialMacroCommand, Input)
     * @see PrepareContextVisitor#prepareContext(ParallelMacroCommand, Input)
     * @see oleg.sopilnyak.test.service.command.executable.sys.MacroCommand#createContext(Input)
     */
    @Override
    public Context<Optional<Student>> acceptPreparedContext(final PrepareContextVisitor visitor, final Input<?> input) {
        return super.acceptPreparedContext(visitor, input);
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

    /**
     * To prepare command for sequential macro-command
     *
     * @param command nested command to wrap
     * @return wrapped nested command
     * @see SequentialMacroCommand#addToNest(NestedCommand)
     */
    @Override
    public NestedCommand<?> wrap(final NestedCommand<?> command) {
        if (command instanceof StudentCommand<?> personCommand) {
            return wrap(personCommand);
        } else if (command instanceof StudentProfileCommand<?> profileCommand) {
            return wrap(profileCommand);
        }
        throw new UnableExecuteCommandException(((RootCommand<?>) command).getId());
    }

    // private methods
    private NestedCommand<?> wrap(final StudentCommand<?> command) {
        return new PersonInSequenceCommand(command);
    }

    private NestedCommand<?> wrap(final StudentProfileCommand<?> command) {
        return new ProfileInSequenceCommand(command);
    }

    private static <T> Context<T> cannotCreateNestedContextFor(RootCommand<?> command) {
        throw new CannotCreateCommandContextException(command.getId());
    }

    // inner classes
    private static class PersonInSequenceCommand extends SequentialMacroCommand.Chained<StudentCommand<?>>
            implements StudentCommand<Void> {
        private final StudentCommand<?> command;

        private PersonInSequenceCommand(StudentCommand<?> command) {
            this.command = command;
        }

        @Override
        public StudentCommand<?> unWrap() {
            return command;
        }

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
            return command.undoAsNestedCommand(visitor, context);
        }
    }

    private static class ProfileInSequenceCommand extends SequentialMacroCommand.Chained<StudentProfileCommand<?>>
            implements StudentProfileCommand<Void> {
        private final StudentProfileCommand<?> command;

        private ProfileInSequenceCommand(StudentProfileCommand<?> command) {
            this.command = command;
        }

        @Override
        public StudentProfileCommand<?> unWrap() {
            return command;
        }


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
            return command.undoAsNestedCommand(visitor, context);
        }
    }

}
