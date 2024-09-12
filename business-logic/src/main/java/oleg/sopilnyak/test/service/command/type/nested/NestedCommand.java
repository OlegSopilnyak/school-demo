package oleg.sopilnyak.test.service.command.type.nested;

import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.command.type.base.RootCommand;

/**
 * Type: Command to execute the business-logic action as a nested-command of CompositeCommand
 *
 * @see oleg.sopilnyak.test.service.command.type.CompositeCommand
 */
public interface NestedCommand {
    /**
     * To prepare context for nested command using the visitor
     *
     * @param visitor visitor of prepared contexts
     * @param input   Macro-Command call's input
     * @param <T>     type of command result
     * @return prepared for nested command context
     * @see PrepareContextVisitor#prepareContext(NestedCommand, Object)
     * @see oleg.sopilnyak.test.service.command.executable.sys.MacroCommand#createContext(Object)
     */
    <T> Context<T> acceptPreparedContext(PrepareContextVisitor visitor, Object input);

    /**
     * To execute command Do as a nested command
     *
     * @param visitor       visitor to do nested command execution
     * @param context       context for nested command execution
     * @param stateListener listener of context-state-change
     * @param <T>           type of command execution result
     * @see NestedCommandExecutionVisitor#doNestedCommand(RootCommand, Context, Context.StateChangedListener)
     * @see Context#addStateListener(Context.StateChangedListener)
     * @see RootCommand#doCommand(Context)
     * @see Context#removeStateListener(Context.StateChangedListener)
     * @see Context.StateChangedListener#stateChanged(Context, Context.State, Context.State)
     */
    <T> void doAsNestedCommand(NestedCommandExecutionVisitor visitor,
                               Context<T> context,
                               Context.StateChangedListener<T> stateListener);

    /**
     * To execute command Undo as a nested command
     *
     * @param visitor visitor to do nested command execution
     * @param context context for nested command execution
     * @param <T>     type of command execution result
     * @see NestedCommandExecutionVisitor#undoNestedCommand(RootCommand, Context)
     * @see RootCommand#undoCommand(Context)
     */
    <T> Context<T> undoAsNestedCommand(NestedCommandExecutionVisitor visitor,
                                       Context<T> context);

    /**
     * For nested command in the sequential macro-command
     *
     * @see oleg.sopilnyak.test.service.command.executable.sys.SequentialMacroCommand
     */
    interface InSequence {
        /**
         * To transfer command execution result to next command context
         *
         * @param visitor     visitor for transfer result
         * @param resultValue result of command execution
         * @param target      command context for next execution
         * @param <S>         type of current command execution result
         * @param <T>         type of next command execution result
         * @see TransferResultVisitor#transferPreviousExecuteDoResult(RootCommand, Object, Context)
         * @see Context#setRedoParameter(Object)
         */
        <S, T> void transferResultTo(TransferResultVisitor visitor,
                                     S resultValue,
                                     Context<T> target);
    }
}
