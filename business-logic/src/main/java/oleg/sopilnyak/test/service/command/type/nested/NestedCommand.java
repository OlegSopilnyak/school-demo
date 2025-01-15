package oleg.sopilnyak.test.service.command.type.nested;

import oleg.sopilnyak.test.service.command.io.Input;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.command.type.base.RootCommand;

/**
 * Type: Command to execute the business-logic action as a nested-command of CompositeCommand
 *
 * @param <T> the type of command execution (do) result
 * @see oleg.sopilnyak.test.service.command.type.CompositeCommand
 */
public interface NestedCommand<T> {
    /**
     * To prepare context for nested command using the visitor
     *
     * @param visitor visitor of prepared contexts
     * @param macroInputParameter   Macro-Command call's input
     * @return prepared for nested command context
     * @see PrepareContextVisitor#prepareContext(NestedCommand, Input)
     * @see oleg.sopilnyak.test.service.command.executable.sys.MacroCommand#createContext(Input)
     */
    Context<T> acceptPreparedContext(PrepareContextVisitor visitor, Input<?> macroInputParameter);

    /**
     * To execute command Do as a nested command
     *
     * @param visitor       visitor to do nested command execution
     * @param context       context for nested command execution
     * @param stateListener listener of context-state-change
     * @see NestedCommandExecutionVisitor#doNestedCommand(RootCommand, Context, Context.StateChangedListener)
     */
    void doAsNestedCommand(NestedCommandExecutionVisitor visitor,
                           Context<?> context,
                           Context.StateChangedListener stateListener);

    /**
     * To execute command Undo as a nested command
     *
     * @param visitor visitor to do nested command execution
     * @param context context for nested command execution
     * @see NestedCommandExecutionVisitor#undoNestedCommand(RootCommand, Context)
     * @see RootCommand#undoCommand(Context)
     */
    Context<?> undoAsNestedCommand(NestedCommandExecutionVisitor visitor, Context<?> context);

    /**
     * To create initial context fo the nested-command
     *
     * @return instance of initial command-context
     */
    Context<?> createContextInit();

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
         * @see TransferResultVisitor#transferPreviousExecuteDoResult(RootCommand, Object, Context)
         * @see oleg.sopilnyak.test.service.command.executable.sys.CommandContext#setRedoParameter(Input)
         */
        <S> void transferResultTo(TransferResultVisitor visitor, S resultValue, Context<?> target);
    }
}
