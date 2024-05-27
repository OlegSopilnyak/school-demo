package oleg.sopilnyak.test.service.command.type.base;

import oleg.sopilnyak.test.service.command.executable.sys.CommandContext;
import oleg.sopilnyak.test.service.command.type.nested.NestedCommandExecutionVisitor;
import oleg.sopilnyak.test.service.command.type.nested.PrepareContextVisitor;
import oleg.sopilnyak.test.service.command.type.nested.TransferResultVisitor;
import org.springframework.lang.NonNull;

/**
 * Type: Nested Command to execute the business-logic action inside MacroCommand
 *
 * @see oleg.sopilnyak.test.service.command.type.CompositeCommand
 * @see oleg.sopilnyak.test.service.command.executable.sys.MacroCommand
 */
public interface NestedCommand extends PrepareContextVisitor.Visitable {
    /**
     * To get unique command-id for the nested command
     *
     * @return value of command-id
     */
    String getId();

    /**
     * To create command's context with doParameter
     *
     * @param input context's doParameter value
     * @param <T>   the type of command result
     * @return context instance
     * @see Context
     * @see Context#getRedoParameter()
     * @see CommandContext
     * @see Context.State#READY
     */
    <T> Context<T> createContext(Object input);

    /**
     * To create command's context without doParameter
     *
     * @param <T> the type of command result
     * @return context instance
     * @see Context
     * @see CommandContext
     * @see Context.State#INIT
     */
    <T> Context<T> createContext();

    /**
     * To get access to command instance as nested one
     *
     * @return the reference to the command instance
     * @see SchoolCommand
     */
//    <C extends SchoolCommand> C asNestedCommand();
    SchoolCommand asNestedCommand();

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
    default <T> Context<T> acceptPreparedContext(@NonNull final PrepareContextVisitor visitor, final Object input) {
        final var nestedCommand = asNestedCommand();
        return visitor.prepareContext(nestedCommand, input);
//        return visitor.prepareContext(this, input);
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
    default <S, T> void transferResultTo(@NonNull final TransferResultVisitor visitor,
                                         final S result, final Context<T> target) {
        final var nestedCommand = asNestedCommand();
        visitor.transferPreviousExecuteDoResult(nestedCommand, result, target);
//        visitor.transferPreviousExecuteDoResult(this, result, target);
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
    default <T> void doAsNestedCommand(@NonNull final NestedCommandExecutionVisitor visitor,
                                       final Context<T> context, final Context.StateChangedListener<T> stateListener) {
        final var nestedCommand = asNestedCommand();
        visitor.doNestedCommand(nestedCommand, context, stateListener);
//        visitor.doNestedCommand(this, context, stateListener);
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
    default <T> Context<T> undoAsNestedCommand(@NonNull final NestedCommandExecutionVisitor visitor,
                                               final Context<T> context) {
        final var nestedCommand = asNestedCommand();
        return visitor.undoNestedCommand(nestedCommand, context);
//        return visitor.undoNestedCommand(this, context);
    }
}
