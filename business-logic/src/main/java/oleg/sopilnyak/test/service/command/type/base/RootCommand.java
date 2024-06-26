package oleg.sopilnyak.test.service.command.type.base;

import oleg.sopilnyak.test.service.command.executable.sys.CommandContext;
import oleg.sopilnyak.test.service.command.type.nested.NestedCommandExecutionVisitor;
import oleg.sopilnyak.test.service.command.type.nested.PrepareContextVisitor;
import oleg.sopilnyak.test.service.command.type.nested.TransferResultVisitor;
import org.springframework.lang.NonNull;

import static java.util.Objects.isNull;

/**
 * Type: Command to execute the business-logic action
 */
public interface RootCommand {

    /**
     * To get unique command-id for the command
     *
     * @return value of command-id
     */
    String getId();

    /**
     * Cast parameter to particular type
     *
     * @param parameter actual parameter
     * @param <P>       type of the parameter
     * @return parameter cast to particular type
     */
    @SuppressWarnings("unchecked")
    default <P> P commandParameter(Object parameter) {
        return (P) parameter;
    }

    /**
     * To check nullable of the input parameter
     *
     * @param parameter value to check
     */
    default void check(Object parameter) {
        if (isNull(parameter)) {
            throw new NullPointerException("Wrong input parameter value null");
        }
    }

    /**
     * To create command's context without doParameter
     *
     * @param <T> the type of command result
     * @return context instance
     * @see Context
     * @see CommandContext
     * @see Context.State#INIT
     */
    default <T> Context<T> createContext() {
        final CommandContext<T> context = CommandContext.<T>builder().command(this).build();
        context.setState(Context.State.INIT);
        return context;
    }

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
    default <T> Context<T> createContext(Object input) {
        final CommandContext<T> context = CommandContext.<T>builder().command(this).redoParameter(input).build();
        context.setState(Context.State.INIT);
        context.setState(Context.State.READY);
        return context;
    }

    /**
     * Before redo context must be in READY state
     *
     * @param context command execution context
     * @param <T>     the type of command result
     * @return true if redo is allowed
     * @see Context
     * @see Context#getState()
     * @see Context.State#READY
     */
    default <T> boolean isWrongRedoStateOf(Context<T> context) {
        return !context.isReady();
    }

    /**
     * To do command execution with Context
     *
     * @param context context of redo execution
     * @param <T>     the type of command result
     * @see Context
     */
    default <T> void doCommand(Context<T> context) {

    }

    /**
     * To execute command redo with correct context state
     *
     * @param context context of redo execution
     * @param <T>     the type of command result
     * @see Context
     * @see Context.State#WORK
     */
    default <T> void executeDo(Context<T> context) {
        context.setState(Context.State.DONE);
    }

    /**
     * Before undo context must be in DONE state
     *
     * @param context command execution context
     * @param <T>     the type of command result
     * @return true if undo is allowed
     * @see Context
     * @see Context#getState()
     * @see Context.State#DONE
     */
    default <T> boolean isWrongUndoStateOf(Context<T> context) {
        return !context.isDone();
    }


    /**
     * To rollback command's execution
     *
     * @param context context of redo execution
     * @param <T>     the type of command result
     * @see Context
     * @see Context#getUndoParameter()
     */
    default <T> void undoCommand(Context<T> context) {

    }

    /**
     * To rollback command's execution with correct context state
     *
     * @param context context of redo execution
     * @param <T>     the type of command result
     * @see Context
     * @see Context#getUndoParameter()
     */
    default <T> void executeUndo(Context<T> context) {
        context.setState(Context.State.UNDONE);
    }

// For commands playing Nested Command Role

    /**
     * To prepare context for nested command using the visitor
     *
     * @param visitor visitor of prepared contexts
     * @param input   Macro-Command call's input
     * @param <T>     type of command result
     * @return prepared for nested command context
     * @see PrepareContextVisitor#prepareContext(RootCommand, Object)
     * @see oleg.sopilnyak.test.service.command.executable.sys.MacroCommand#createContext(Object)
     */
    default <T> Context<T> acceptPreparedContext(@NonNull final PrepareContextVisitor visitor, final Object input) {
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
     * @see TransferResultVisitor#transferPreviousExecuteDoResult(RootCommand, Object, Context)
     * @see Context#setRedoParameter(Object)
     */
    default <S, T> void transferResultTo(@NonNull final TransferResultVisitor visitor,
                                         final S result, final Context<T> target) {
        visitor.transferPreviousExecuteDoResult(this, result, target);
    }

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
    default <T> void doAsNestedCommand(@NonNull final NestedCommandExecutionVisitor visitor,
                                       final Context<T> context, final Context.StateChangedListener<T> stateListener) {
        visitor.doNestedCommand(this, context, stateListener);
    }

    /**
     * To execute command Undo as a nested command
     *
     * @param visitor visitor to do nested command execution
     * @param context context for nested command execution
     * @param <T>     type of command execution result
     * @see NestedCommandExecutionVisitor#undoNestedCommand(RootCommand, Context)
     * @see RootCommand#undoCommand(Context)
     */
    default <T> Context<T> undoAsNestedCommand(@NonNull final NestedCommandExecutionVisitor visitor,
                                               final Context<T> context) {
        return visitor.undoNestedCommand(this, context);
    }
}
