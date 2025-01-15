package oleg.sopilnyak.test.service.command.type.base;

import oleg.sopilnyak.test.service.command.executable.sys.CommandContext;
import oleg.sopilnyak.test.service.command.io.Input;
import oleg.sopilnyak.test.service.command.type.nested.NestedCommand;
import oleg.sopilnyak.test.service.command.type.nested.NestedCommandExecutionVisitor;
import oleg.sopilnyak.test.service.command.type.nested.PrepareContextVisitor;
import org.slf4j.Logger;

import static java.util.Objects.isNull;

/**
 * Type: Command to execute the business-logic action
 *
 * @param <T> the type of command execution (do) result
 */
public interface RootCommand<T> extends CommandExecutable<T>, NestedCommand<T> {
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
    @Deprecated
    @SuppressWarnings("unchecked")
    default <P> P commandParameter(Object parameter) {
        return (P) parameter;
    }

    @SuppressWarnings("unchecked")
    default <P> P commandParameter(Input<?> parameter) {
        return (P) parameter.value();
    }

    /**
     * To check nullable of the input parameter
     *
     * @param parameter input value to check
     */
    default void checkNullParameter(Input<?> parameter) {
        if (isNull(parameter)  || parameter.isEmpty()) {
            throw new NullPointerException("Wrong input parameter value null");
        }
    }

    @Deprecated
    default void checkNullParameter(Object parameter) {
        if (isNull(parameter)) {
            throw new NullPointerException("Wrong input parameter value null");
        }
    }

    /**
     * To create command's context without parameters
     *
     * @return context instance
     * @see Context
     * @see CommandContext
     * @see Context.State#INIT
     */
    @Override
    default Context<T> createContext() {
        final Context<T> context = CommandContext.<T>builder().command(this).build();
        context.setState(Context.State.INIT);
        return context;
    }

    /**
     * To create command's context with doParameter
     *
     * @param parameter context's doParameter input value
     * @return context instance
     * @see Input
     * @see Context
     * @see Context#getRedoParameter()
     * @see CommandContext
     * @see Context.State#READY
     */
    @Override
    default Context<T> createContext(Input<?> parameter) {
        final Context<T> context = CommandContext.<T>builder().command(this).redoParameter(parameter).build();
        context.setState(Context.State.INIT);
        context.setState(Context.State.READY);
        return context;
    }

    /**
     * To create initial context fo the nested-command
     *
     * @return instance of initial command-context
     */
    @Override
    default Context<T> createContextInit() {
        return createContext();
    }

    /**
     * To execute command logic with context
     *
     * @param context context of redo execution
     * @see Context
     * @see CommandExecutable#executeDo(Context)
     */
    @Override
    default void doCommand(Context<T> context) {
        if (context.isReady()) {
            // start redo with correct context state
            context.setState(Context.State.WORK);
            executeDo(context);
        } else {
            getLog().warn("Cannot do redo of command {} with context:state '{}'", getId(), context.getState());
            context.setState(Context.State.FAIL);
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
    default void undoCommand(Context<?> context) {
        if (!context.isDone()) {
            getLog().warn("Cannot do undo of command {} with context:state '{}'", getId(), context.getState());
            context.setState(Context.State.FAIL);
        } else {
            // start undo with correct context state
            context.setState(Context.State.WORK);
            executeUndo(context);
        }
    }

    // For commands playing as a Nested Command

    /**
     * To prepare context for nested command using the visitor
     *
     * @param visitor             visitor of prepared contexts
     * @param macroInputParameter Macro-Command call's input
     * @return prepared for nested command context
     * @see PrepareContextVisitor#prepareContext(RootCommand, Input<?>)
     */
    @Override
    default Context<T> acceptPreparedContext(final PrepareContextVisitor visitor, final Input<?> macroInputParameter) {
        return visitor.prepareContext(this, macroInputParameter);
    }

    /**
     * To execute command's Do as a nested command
     *
     * @param visitor       visitor to do nested command execution
     * @param context       context for nested command execution
     * @param stateListener listener of context-state-change
     * @see Context
     * @see Context.StateChangedListener
     * @see NestedCommandExecutionVisitor#doNestedCommand(RootCommand, Context, Context.StateChangedListener)
     */
    @SuppressWarnings("unchecked")
    @Override
    default void doAsNestedCommand(final NestedCommandExecutionVisitor visitor,
                                   final Context<?> context, final Context.StateChangedListener stateListener) {
        visitor.doNestedCommand(this, (Context<T>) context, stateListener);
    }

    /**
     * To execute command Undo as a nested command
     *
     * @param visitor visitor to do nested command execution
     * @param context context for nested command execution
     * @see NestedCommandExecutionVisitor#undoNestedCommand(RootCommand, Context)
     * @see RootCommand#undoCommand(Context)
     */
    @Override
    default Context<?> undoAsNestedCommand(final NestedCommandExecutionVisitor visitor, final Context<?> context) {
        return visitor.undoNestedCommand(this, context);
    }
}
