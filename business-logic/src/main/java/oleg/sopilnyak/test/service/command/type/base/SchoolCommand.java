package oleg.sopilnyak.test.service.command.type.base;

import oleg.sopilnyak.test.service.command.executable.sys.CommandContext;
import oleg.sopilnyak.test.service.command.executable.sys.CommandResult;

import java.util.ArrayList;
import java.util.List;

/**
 * Type: Command to execute the business-logic action
 */
public interface SchoolCommand<T> {
    /**
     * To execute command's business-logic
     *
     * @param parameter command's parameter
     * @return execution's result
     */
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
        final Context.State startedFrom = Context.State.INIT;
        return CommandContext.<T>builder()
                .command(this)
                .states(new ArrayList<>(List.of(startedFrom)))
                .state(startedFrom)
                .build();
    }

    /**
     * To create command's context with doParameter
     *
     * @param input context's doParameter value
     * @return context instance
     * @see Context
     * @see Context#getDoParameter()
     * @see CommandContext
     * @see Context.State#READY
     */
    default Context<T> createContext(Object input) {
        final Context.State startedFrom = Context.State.READY;
        return CommandContext.<T>builder()
                .command(this)
                .doParameter(input)
                .states(new ArrayList<>(List.of(startedFrom)))
                .state(startedFrom)
                .build();
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
    default boolean isWrongRedoStateOf(Context<T> context) {
        return context.getState() != Context.State.READY;
    }

    /**
     * To execute command
     *
     * @param context context of redo execution
     * @see Context
     */
    default void redo(Context<T> context) {

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
    default boolean isWrongUndoStateOf(Context<T> context) {
        return context.getState() != Context.State.DONE;
    }


    /**
     * To rollback command's execution
     *
     * @param context context of redo execution
     * @see Context
     * @see Context#getUndoParameter()
     */
    default void undo(Context<T> context) {

    }

}
