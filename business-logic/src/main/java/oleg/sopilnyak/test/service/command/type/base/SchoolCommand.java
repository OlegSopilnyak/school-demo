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
        return CommandContext.<T>builder()
                .command(this)
                .state(Context.State.INIT)
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
        return CommandContext.<T>builder()
                .command(this)
                .doParameter(input)
                .state(Context.State.READY)
                .build();
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
     * To rollback command's execution
     *
     * @param context context of redo execution
     * @see Context
     * @see Context#getUndoParameter()
     */
    default void undo(Context<T> context) {

    }
}
