package oleg.sopilnyak.test.service.command.type.base;

import oleg.sopilnyak.test.service.command.executable.sys.CommandContext;

/**
 * Type: Command to execute command business-logic
 */
public interface CommandExecutable {
    /**
     * To create command's execution context without input parameter
     *
     * @param <T> the type of command result
     * @return context instance
     * @see Context
     * @see CommandContext
     * @see Context.State#INIT
     * @see CommandExecutable#executeDo(Context)
     * @see CommandExecutable#executeUndo(Context)
     */
    <T> Context<T> createContext();

    /**
     * To create command's execution context with input parameter
     *
     * @param input context's doParameter value
     * @param <T>   the type of command result
     * @return context instance
     * @see Context
     * @see Context#getRedoParameter()
     * @see CommandContext
     * @see Context.State#READY
     * @see CommandExecutable#executeDo(Context)
     * @see CommandExecutable#executeUndo(Context)
     */
    <T> Context<T> createContext(Object input);


    /**
     * To execute command do with correct context state (default implementation)
     *
     * @param context context of redo execution
     * @param <T>     the type of command result
     * @see Context
     * @see Context.State#WORK
     * @see RootCommand#doCommand(Context)
     */
    default <T> void executeDo(Context<T> context) {
        context.setState(Context.State.DONE);
    }

    /**
     * To do command execution with command context
     *
     * @param context context of redo execution
     * @param <T>     the type of command result
     * @see Context
     */
    <T> void doCommand(Context<T> context);

    /**
     * To rollback command's execution with correct context state (default implementation)
     *
     * @param context context of redo execution
     * @param <T>     the type of command result
     * @see Context
     * @see Context#getUndoParameter()
     */
    default <T> void executeUndo(Context<T> context) {
        context.setState(Context.State.UNDONE);
    }

    /**
     * To rollback command's execution
     *
     * @param context context of redo execution
     * @see Context
     * @see Context#getUndoParameter()
     * @see RootCommand#undoCommand(Context)
     */
    <T> void undoCommand(Context<T> context);
}
