package oleg.sopilnyak.test.service.command.type.nested;

import java.io.Serializable;
import oleg.sopilnyak.test.service.command.io.Input;
import oleg.sopilnyak.test.service.command.type.base.CompositeCommand;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.command.type.base.RootCommand;

/**
 * Type: Command to execute the business-logic action as a nested-command of CompositeCommand
 *
 * @param <T> the type of command execution (do) result
 * @see CompositeCommand
 */
public interface NestedCommand<T> extends Serializable {
    /**
     * To prepare context for nested command using the visitor
     *
     * @param visitor             visitor of prepared contexts
     * @param macroInputParameter Macro-Command call's input
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
     * To create failed context for the nested-command
     *
     * @param cause cause of fail
     * @return instance of failed command-context
     */
    Context<T> createFailedContext(Exception cause);

    /**
     * For nested command in the sequential macro-command
     *
     * @see oleg.sopilnyak.test.service.command.executable.sys.SequentialMacroCommand
     */
    interface InSequence {
        /**
         * To transfer nested command execution result to target nested command context input
         *
         * @param visitor visitor for do transferring result from source to target
         * @param value   result of source command execution
         * @param target  nested command context for the next execution in sequence
         * @param <S>     type of source command execution result
         * @param <N>     type of target command execution result
         * @see TransferResultVisitor#transferPreviousExecuteDoResult(RootCommand, Object, Context)
         * @see oleg.sopilnyak.test.service.command.executable.sys.CommandContext#setRedoParameter(Input)
         */
        <S, N> void transferResultTo(TransferResultVisitor visitor, S value, Context<N> target);
    }
}
