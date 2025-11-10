package oleg.sopilnyak.test.service.command.type.base;

import oleg.sopilnyak.test.service.command.io.Input;

/**
 * Type: Command to execute command business-logic
 *
 * @param <T> the type of command execution result
 */
public interface CommandExecutable<T> {
    /**
     * To create command's execution context without input parameter
     *
     * @return context instance
     * @see Context
     * @see CommandExecutable#executeDo(Context)
     * @see CommandExecutable#executeUndo(Context)
     */
    Context<T> createContext();

    /**
     * To create command's execution context with input parameter
     *
     * @param input context's doParameter input value
     * @return context instance
     * @see Input
     * @see CommandExecutable#executeDo(Context)
     * @see CommandExecutable#executeUndo(Context)
     */
    Context<T> createContext(Input<?> input);

    /**
     * Reference to the current command for operations with the command's entities in transaction possibility
     *
     * @return the reference to the current command from spring beans factory
     * @see RootCommand#doCommand(Context)
     * @see RootCommand#undoCommand(Context)
     */
    CommandExecutable<T> self();
    /**
     * To execute command do with correct context state (default implementation)
     *
     * @param context context of redo execution
     * @see Context.State#DONE
     * @see Context#setState(Context.State)
     * @see RootCommand#doCommand(Context)
     */
    default void executeDo(Context<T> context) {
        context.setState(Context.State.DONE);
    }

    /**
     * To do command execution with command context
     *
     * @param context context of redo execution
     * @see Context
     */
    void doCommand(Context<T> context);

    /**
     * To rollback command's execution with correct context state (default implementation)
     * <BR/> the type of command result doesn't matter
     *
     * @param context context of redo execution
     * @see Context.State#DONE
     * @see Context#setState(Context.State)
     * @see RootCommand#executeUndo(Context)
     */
    default void executeUndo(Context<?> context) {
        context.setState(Context.State.UNDONE);
    }

    /**
     * To rollback command's execution
     * <BR/> the type of command result doesn't matter
     *
     * @param context context of redo execution
     * @see Context
     * @see Context#getUndoParameter()
     * @see RootCommand#undoCommand(Context)
     */
    void undoCommand(Context<?> context);
}
