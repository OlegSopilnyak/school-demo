package oleg.sopilnyak.test.service.command.type.base;

import oleg.sopilnyak.test.service.command.executable.sys.CommandContext;
import oleg.sopilnyak.test.service.command.type.nested.NestedCommand;
import oleg.sopilnyak.test.service.command.type.nested.NestedCommandExecutionVisitor;
import oleg.sopilnyak.test.service.command.type.nested.PrepareContextVisitor;
import org.slf4j.Logger;

import static java.util.Objects.isNull;

/**
 * Type: Command to execute the business-logic action
 */
public interface RootCommand extends CommandExecutable, NestedCommand {
    /**
     * To get reference to command's logger
     *
     * @return reference to the logger
     */
    Logger getLog();

    /**
     * To get unique command-id for the command
     *
     * @return value of command-id
     */
    String getId();

    /**
     * Cast input parameter to particular type
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
     * @param parameter input value to check
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
    @Override
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
    @Override
    default <T> Context<T> createContext(Object input) {
        final CommandContext<T> context = CommandContext.<T>builder().command(this).redoParameter(input).build();
        context.setState(Context.State.INIT);
        context.setState(Context.State.READY);
        return context;
    }

    /**
     * To execute command logic with context
     *
     * @param context context of redo execution
     * @see Context
     * @see CommandExecutable#executeDo(Context)
     */
    @Override
    default <T> void doCommand(Context<T> context) {
        if (!context.isReady()) {
            getLog().warn("Cannot do redo of command {} with context:state '{}'", getId(), context.getState());
            context.setState(Context.State.FAIL);
        } else {
            // start redo with correct context state
            context.setState(Context.State.WORK);
            executeDo(context);
        }
    }

    /**
     * To rollback command's execution according to command context
     *
     * @param context context of redo execution
     * @see Context
     * @see Context#getUndoParameter()
     * @see CommandExecutable#executeUndo(Context)
     */
    @Override
    default <T> void undoCommand(Context<T> context) {
        if (!context.isDone()) {
            getLog().warn("Cannot do undo of command {} with context:state '{}'", getId(), context.getState());
            context.setState(Context.State.FAIL);
        } else {
            // start undo with correct context state
            context.setState(Context.State.WORK);
            executeUndo(context);
        }
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
    @Override
    default <T> Context<T> acceptPreparedContext(final PrepareContextVisitor visitor,
                                                 final Object input) {
        return visitor.prepareContext(this, input);
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
    @Override
    default <T> void doAsNestedCommand(final NestedCommandExecutionVisitor visitor,
                                       final Context<T> context,
                                       final Context.StateChangedListener<T> stateListener) {
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
    @Override
    default <T> Context<T> undoAsNestedCommand(final NestedCommandExecutionVisitor visitor,
                                               final Context<T> context) {
        return visitor.undoNestedCommand(this, context);
    }
}
