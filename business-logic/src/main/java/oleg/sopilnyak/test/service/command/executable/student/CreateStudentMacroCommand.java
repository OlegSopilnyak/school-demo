package oleg.sopilnyak.test.service.command.executable.student;

import lombok.extern.slf4j.Slf4j;
import oleg.sopilnyak.test.school.common.model.Student;
import oleg.sopilnyak.test.school.common.model.StudentProfile;
import oleg.sopilnyak.test.service.command.executable.profile.student.CreateOrUpdateStudentProfileCommand;
import oleg.sopilnyak.test.service.command.executable.sys.ParallelMacroCommand;
import oleg.sopilnyak.test.service.command.executable.sys.SequentialMacroCommand;
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
import oleg.sopilnyak.test.service.message.StudentPayload;
import oleg.sopilnyak.test.service.message.StudentProfilePayload;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import java.util.Deque;
import java.util.Optional;

/**
 * Command-Implementation: command to create the student instance
 *
 * @see Student
 * @see StudentProfile
 * @see SequentialMacroCommand
 */
@Slf4j
@Component
public class CreateStudentMacroCommand extends SequentialMacroCommand implements StudentCommand {
    private final BusinessMessagePayloadMapper payloadMapper;
    @Value("${school.mail.basic.domain:gmail.com}")
    private String emailDomain;

    public CreateStudentMacroCommand(final CreateOrUpdateStudentCommand studentCommand,
                                     final CreateOrUpdateStudentProfileCommand profileCommand,
                                     final BusinessMessagePayloadMapper payloadMapper) {
        addToNest(profileCommand);
        addToNest(studentCommand);
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
     * @param mainInput macro-command input parameter
     * @return built context of the command for input parameter
     * @see Student
     * @see StudentCommand
     * @see this#createStudentContext(StudentCommand, Student)
     * @see Context
     */
    @Override
    public <T> Context<T> prepareContext(@NonNull final StudentCommand command, final Object mainInput) {
        return mainInput instanceof Student student && StudentCommand.CREATE_OR_UPDATE.equals(command.getId()) ?
                createStudentContext(command, student) : cannotCreateNestedContextFor(command);
    }

    /**
     * To prepare context for particular type of the nested command
     *
     * @param command   nested command instance
     * @param mainInput macro-command input parameter
     * @param <T>       type of create-or-update student profile command result
     * @return built context of the command for input parameter
     * @see Student
     * @see StudentProfileCommand
     * @see this#createStudentProfileContext(StudentProfileCommand, Student)
     * @see Context
     */
    @Override
    public <T> Context<T> prepareContext(@NonNull final StudentProfileCommand command, final Object mainInput) {
        return mainInput instanceof Student student && StudentProfileCommand.CREATE_OR_UPDATE.equals(command.getId()) ?
                createStudentProfileContext(command, student) : cannotCreateNestedContextFor(command);
    }

    /**
     * To create context for create-or-update student command
     *
     * @param command   create-or-update student command instance
     * @param parameter input parameter of student to create
     * @param <T>       type of create-or-update student command result
     * @return built context of the command for input student
     * @see BusinessMessagePayloadMapper#toPayload(Student)
     * @see StudentPayload
     */
    public <T> Context<T> createStudentContext(@NonNull final StudentCommand command, final Student parameter) {
        final StudentPayload payload = parameter instanceof StudentPayload studentPayload ?
                studentPayload : payloadMapper.toPayload(parameter);
        payload.setId(null);
        return command.createContext(payload);
    }

    /**
     * To create context for create-or-update student profile command
     *
     * @param command   create-or-update student profile command instance
     * @param parameter input parameter of student to create
     * @param <T>       type of create-or-update student profile command result
     * @return built context of the command for input parameter
     * @see StudentProfilePayload
     */
    public <T> Context<T> createStudentProfileContext(final StudentProfileCommand command, final Student parameter) {
        final String emailPrefix = parameter.getFirstName().toLowerCase() + "." + parameter.getLastName().toLowerCase();
        final StudentProfilePayload payload = StudentProfilePayload.builder()
                .id(null)
                .phone("Not-Exists-Yet")
                .email(emailPrefix + "@" + emailDomain)
                .build();
        return command.createContext(payload);
    }

// for command do activities as nested command

    /**
     * To transfer result from current command to next command context.<BR/>
     * Send create-profile command result (profile-id) to create-student command input
     *
     * @param command successfully executed command
     * @param result  the result of successful command execution
     * @param target  next command context to execute command's redo
     * @see StudentProfileCommand#doCommand(Context)
     * @see Context#setRedoParameter(Object)
     * @see SequentialMacroCommand#doNestedCommands(Deque, Context.StateChangedListener)
     * @see CreateStudentMacroCommand#transferProfileIdToStudentInput(Long, Context)
     * @see CannotTransferCommandResultException
     */
    @Override
    public <S, T> void transferPreviousExecuteDoResult(@NonNull final StudentProfileCommand command,
                                                       @NonNull final S result,
                                                       @NonNull final Context<T> target) {
        if (result instanceof Optional<?> opt &&
                opt.orElseThrow() instanceof StudentProfile profile &&
                StudentCommand.CREATE_OR_UPDATE.equals(target.getCommand().getId())) {
            // send create-profile result (profile-id) to create-student input (StudentPayload#setProfileId)
            transferProfileIdToStudentInput(profile.getId(), target);
        } else {
            throw new CannotTransferCommandResultException(command.getId());
        }
    }

    /**
     * To transfer profile-id to create-student command input
     *
     * @param profileId the id of profile created before student
     * @param target    create-student command context
     * @see Context#getRedoParameter()
     * @see Context#setRedoParameter(Object)
     * @see StudentPayload#setProfileId(Long)
     */
    public void transferProfileIdToStudentInput(final Long profileId, @NonNull final Context<?> target) {
        final StudentPayload student = target.getRedoParameter();
        log.debug("Transferring profile id: {} to student: {}", profileId, student);
        student.setProfileId(profileId);
        target.setRedoParameter(student);
    }

    /**
     * To prepare context for nested command using the visitor
     *
     * @param visitor visitor of prepared contexts
     * @param input   Macro-Command call's input
     * @param <T>     type of command result
     * @return prepared for nested command context
     * @see PrepareContextVisitor#prepareContext(SequentialMacroCommand, Object)
     * @see PrepareContextVisitor#prepareContext(ParallelMacroCommand, Object)
     * @see oleg.sopilnyak.test.service.command.executable.sys.MacroCommand#createContext(Object)
     */
    @Override
    public <T> Context<T> acceptPreparedContext(@NonNull final PrepareContextVisitor visitor, final Object input) {
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
    public <T> void doAsNestedCommand(@NonNull final NestedCommandExecutionVisitor visitor,
                                      @NonNull final Context<T> context,
                                      @NonNull final Context.StateChangedListener<T> stateListener) {
        super.doAsNestedCommand(visitor, context, stateListener);
    }

    /**
     * To execute command Undo as a nested command
     *
     * @param visitor visitor to do nested command execution
     * @param context context for nested command execution
     * @param <T>     type of command execution result
     * @see NestedCommandExecutionVisitor#undoNestedCommand(RootCommand, Context)
     * @see CompositeCommand#undoCommand(Context)
     */
    @Override
    public <T> Context<T> undoAsNestedCommand(@NonNull final NestedCommandExecutionVisitor visitor,
                                              @NonNull final Context<T> context) {
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
    public NestedCommand wrap(final NestedCommand command) {
        if (command instanceof StudentCommand studentCommand) {
            return wrap(studentCommand);
        } else if (command instanceof StudentProfileCommand studentProfileCommand) {
            return wrap(studentProfileCommand);
        }
        throw new UnableExecuteCommandException(((RootCommand) command).getId());
    }

    // private methods
    private NestedCommand wrap(final StudentCommand command) {
        return new StudentInSequenceCommand(command);
    }

    private NestedCommand wrap(final StudentProfileCommand command) {
        return new StudentProfileInSequenceCommand(command);
    }

    private static <T> Context<T> cannotCreateNestedContextFor(RootCommand command) {
        throw new CannotCreateCommandContextException(command.getId());
    }

    // inner classes
    private static class StudentInSequenceCommand extends SequentialMacroCommand.Chained<StudentCommand> implements StudentCommand {
        private final StudentCommand command;

        private StudentInSequenceCommand(StudentCommand command) {
            this.command = command;
        }

        @Override
        public StudentCommand unWrap() {
            return command;
        }

        @Override
        public <S, T> void transferResultTo(final TransferResultVisitor visitor,
                                            final S resultValue,
                                            final Context<T> target) {
            visitor.transferPreviousExecuteDoResult(command, resultValue, target);
        }
    }

    private static class StudentProfileInSequenceCommand extends SequentialMacroCommand.Chained<StudentProfileCommand> implements StudentProfileCommand {
        private final StudentProfileCommand command;

        private StudentProfileInSequenceCommand(StudentProfileCommand command) {
            this.command = command;
        }

        @Override
        public StudentProfileCommand unWrap() {
            return command;
        }

        @Override
        public <S, T> void transferResultTo(final TransferResultVisitor visitor,
                                            final S resultValue,
                                            final Context<T> target) {
            visitor.transferPreviousExecuteDoResult(command, resultValue, target);
        }
    }

}
