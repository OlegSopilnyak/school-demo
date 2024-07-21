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
import oleg.sopilnyak.test.service.command.type.nested.NestedCommandExecutionVisitor;
import oleg.sopilnyak.test.service.command.type.nested.PrepareContextVisitor;
import oleg.sopilnyak.test.service.command.type.nested.TransferResultVisitor;
import oleg.sopilnyak.test.service.command.type.profile.StudentProfileCommand;
import oleg.sopilnyak.test.service.exception.CannotCreateCommandContextException;
import oleg.sopilnyak.test.service.mapper.BusinessMessagePayloadMapper;
import oleg.sopilnyak.test.service.message.StudentPayload;
import oleg.sopilnyak.test.service.message.StudentProfilePayload;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

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
    private final CreateOrUpdateStudentCommand studentCommand;
    private final CreateOrUpdateStudentProfileCommand profileCommand;
    private final BusinessMessagePayloadMapper payloadMapper;
    @Value("${school.mail.basic.domain:gmail.com}")
    private String emailDomain;

    public CreateStudentMacroCommand(final CreateOrUpdateStudentCommand studentCommand,
                                     final CreateOrUpdateStudentProfileCommand profileCommand,
                                     final BusinessMessagePayloadMapper payloadMapper) {
        this.studentCommand = studentCommand;
        this.profileCommand = profileCommand;
        addToNest(profileCommand);
        addToNest(studentCommand);
        this.payloadMapper = payloadMapper;
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
    public <T> Context<T> prepareContext(StudentCommand command, Object mainInput) {
        return mainInput instanceof Student student && StudentCommand.CREATE_OR_UPDATE.equals(command.getId()) ?
                createStudentContext(command, student) : cannotCreateNestedContextFor(command);
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
    public <T> Context<T> createStudentContext(StudentCommand command, Student parameter) {
        final StudentPayload payload = parameter instanceof StudentPayload studentPayload ?
                studentPayload : payloadMapper.toPayload(parameter);
        payload.setId(null);
        return command.createContext(payload);
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
    public <T> Context<T> prepareContext(final StudentProfileCommand command, final Object mainInput) {
        return mainInput instanceof Student student && StudentProfileCommand.CREATE_OR_UPDATE.equals(command.getId()) ?
                createStudentProfileContext(command, student) : cannotCreateNestedContextFor(command);
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
    public <T> Context<T> createStudentProfileContext(StudentProfileCommand command, Student parameter) {
        final String emailPrefix = parameter.getFirstName().toLowerCase() + "." + parameter.getLastName().toLowerCase();
        final StudentProfilePayload payload = StudentProfilePayload.builder()
                .id(null)
                .phone("Not-Exists-Yet")
                .email(emailPrefix + "@" + emailDomain)
                .build();
        return command.createContext(payload);
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
     * To transfer command execution result to next command context
     *
     * @param visitor     visitor for transfer result
     * @param resultValue result of command execution
     * @param target      command context for next execution
     * @param <S>         type of current command execution result
     * @param <T>         type of next command execution result
     * @see TransferResultVisitor#transferPreviousExecuteDoResult(CompositeCommand, Object, Context)
     * @see Context#setRedoParameter(Object)
     */
    @Override
    public <S, T> void transferResultTo(@NonNull final TransferResultVisitor visitor,
                                        final S resultValue, final Context<T> target) {
        super.transferResultTo(visitor, resultValue, target);
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
                                      final Context<T> context, final Context.StateChangedListener<T> stateListener) {
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
                                              final Context<T> context) {
        return super.undoAsNestedCommand(visitor, context);
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

// private methods

    private static <T> Context<T> cannotCreateNestedContextFor(RootCommand command) {
        throw new CannotCreateCommandContextException(command.getId());
    }
}
