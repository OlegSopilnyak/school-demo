package oleg.sopilnyak.test.service.command.type.base;

import oleg.sopilnyak.test.service.command.type.nested.NestedCommandExecutionVisitor;
import oleg.sopilnyak.test.service.command.type.nested.PrepareContextVisitor;
import oleg.sopilnyak.test.service.command.type.nested.TransferResultVisitor;
import org.springframework.lang.NonNull;

/**
 * Type: Nested Command to execute the business-logic action inside MacroCommand (not SchoolCommand)
 *
 * @see oleg.sopilnyak.test.service.command.type.CompositeCommand
 * @see oleg.sopilnyak.test.service.command.executable.sys.MacroCommand
 */
public interface CanBeNested extends NestedCommand {

    /**
     * To prepare context for nested command using the visitor
     *
     * @param visitor visitor of prepared contexts
     * @param input   Macro-Command call's input
     * @param <T>     type of command result
     * @return prepared for nested command context
     * @see PrepareContextVisitor#prepareContext(SchoolCommand, Object)
     * @see oleg.sopilnyak.test.service.command.executable.sys.MacroCommand#createContext(Object)
     */
    @Override
    default <T> Context<T> acceptPreparedContext(@NonNull final PrepareContextVisitor visitor, final Object input) {
        final var nestedCommand = asNestedCommand();
//        return visitor.prepareContext(nestedCommand, input);
        return visitor.prepareContext(this, input);
    }

    /**
     * To transfer command execution result to next command context
     *
     * @param visitor visitor for transfer result
     * @param result  result of command execution
     * @param target  command context for next execution
     * @param <S>     type of current command execution result
     * @param <T>     type of next command execution result
     * @see TransferResultVisitor#transferPreviousExecuteDoResult(SchoolCommand, Object, Context)
     * @see Context#setRedoParameter(Object)
     */
    @Override
    default <S, T> void transferResultTo(@NonNull final TransferResultVisitor visitor,
                                         final S result, final Context<T> target) {
        final var nestedCommand = asNestedCommand();
        visitor.transferPreviousExecuteDoResult(nestedCommand, result, target);
    }

    /**
     * To execute command Do as a nested command
     *
     * @param visitor       visitor to do nested command execution
     * @param context       context for nested command execution
     * @param stateListener listener of context-state-change
     * @param <T>           type of command execution result
     * @see NestedCommandExecutionVisitor#doNestedCommand(SchoolCommand, Context, Context.StateChangedListener)
     * @see Context#addStateListener(Context.StateChangedListener)
     * @see SchoolCommand#doCommand(Context)
     * @see Context#removeStateListener(Context.StateChangedListener)
     * @see Context.StateChangedListener#stateChanged(Context, Context.State, Context.State)
     */
    @Override
    default <T> void doAsNestedCommand(@NonNull final NestedCommandExecutionVisitor visitor,
                                       final Context<T> context, final Context.StateChangedListener<T> stateListener) {
        final var nestedCommand = asNestedCommand();
        visitor.doNestedCommand(nestedCommand, context, stateListener);
    }

    /**
     * To execute command Undo as a nested command
     *
     * @param visitor visitor to do nested command execution
     * @param context context for nested command execution
     * @param <T>     type of command execution result
     * @see NestedCommandExecutionVisitor#undoNestedCommand(SchoolCommand, Context)
     * @see SchoolCommand#undoCommand(Context)
     */
    @Override
    default <T> Context<T> undoAsNestedCommand(@NonNull final NestedCommandExecutionVisitor visitor,
                                               final Context<T> context) {
        final var nestedCommand = asNestedCommand();
        return visitor.undoNestedCommand(nestedCommand, context);
    }
}
