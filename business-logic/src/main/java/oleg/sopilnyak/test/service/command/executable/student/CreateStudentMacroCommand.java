package oleg.sopilnyak.test.service.command.executable.student;

import lombok.extern.slf4j.Slf4j;
import oleg.sopilnyak.test.school.common.model.Student;
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
import org.slf4j.Logger;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

/**
 * Command-Implementation: command to create the student instance
 *
 * @see Student
 */
@Slf4j
@Component
public class CreateStudentMacroCommand extends SequentialMacroCommand implements StudentCommand {
    private final CreateOrUpdateStudentCommand studentCommand;
    private final CreateOrUpdateStudentProfileCommand profileCommand;

    public CreateStudentMacroCommand(final CreateOrUpdateStudentCommand studentCommand,
                                     final CreateOrUpdateStudentProfileCommand profileCommand) {
        this.studentCommand = studentCommand;
        this.profileCommand = profileCommand;
        addToNest(profileCommand);
        addToNest(studentCommand);
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
}
