package oleg.sopilnyak.test.service.command.type.base;

import oleg.sopilnyak.test.service.command.executable.sys.CommandContext;
import oleg.sopilnyak.test.service.command.executable.sys.CommandResult;

/**
 * Type: Command to execute the business-logic action
 */
public interface SchoolCommand<T> {
    /**
     * To execute command's business-logic
     *
     * @param parameter command's parameter
     * @return execution's result
     * @deprecated commands are going to work through redo/undo
     * @see this#doCommand(Context)
     * @see this#undoCommand(Context)
     */
    @Deprecated(forRemoval = true)
    CommandResult<T> execute(Object parameter);

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
     * To create command's context without doParameter
     *
     * @return context instance
     * @see Context
     * @see CommandContext
     * @see Context.State#INIT
     */
    default Context<T> createContext() {
        final CommandContext<T> context = CommandContext.<T>builder().command(this).build();
        context.setState(Context.State.INIT);
        return context;
    }

    /**
     * To create command's context with doParameter
     *
     * @param input context's doParameter value
     * @return context instance
     * @see Context
     * @see Context#getRedoParameter()
     * @see CommandContext
     * @see Context.State#READY
     */
    default Context<T> createContext(Object input) {
        final CommandContext<T> context = CommandContext.<T>builder().command(this).redoParameter(input).build();
        context.setState(Context.State.INIT);
        context.setState(Context.State.READY);
        return context;
    }

    /**
     * Before redo context must be in READY state
     *
     * @param context command execution context
     * @return true if redo is allowed
     * @see Context
     * @see Context#getState()
     * @see Context.State#READY
     */
    default boolean isWrongRedoStateOf(Context<?> context) {
        return context.getState() != Context.State.READY;
    }

    /**
     * To do command execution with Context
     *
     * @param context context of redo execution
     * @see Context
     */
    default void doCommand(Context<?> context) {

    }

    /**
     * Before undo context must be in DONE state
     *
     * @param context command execution context
     * @return true if undo is allowed
     * @see Context
     * @see Context#getState()
     * @see Context.State#DONE
     */
    default boolean isWrongUndoStateOf(Context<?> context) {
        return !context.isDone();
    }


    /**
     * To rollback command's execution
     *
     * @param context context of redo execution
     * @see Context
     * @see Context#getUndoParameter()
     */
    default void undoCommand(Context<?> context) {

    }

}
