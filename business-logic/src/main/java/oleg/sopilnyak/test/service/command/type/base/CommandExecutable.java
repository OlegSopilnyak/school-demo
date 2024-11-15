package oleg.sopilnyak.test.service.command.type.base;

import oleg.sopilnyak.test.service.command.executable.sys.CommandContext;

/**
 * Type: Command to execute command business-logic
 * @param <T> the type of command execution (do) result
 */
public interface CommandExecutable<T> {
    /**
     * To create command's execution context without input parameter
     *
//     * @param <T> the type of command result
     * @return context instance
     * @see Context
     * @see CommandContext
     * @see Context.State#INIT
     * @see CommandExecutable#executeDo(Context)
     * @see CommandExecutable#executeUndo(Context)
     */
//    <T> Context<T> createContext();
    Context<T> createContext();

    /**
     * To create command's execution context with input parameter
     *
     * @param input context's doParameter value
//     * @param <T>   the type of command result
     * @return context instance
     * @see Context
     * @see Context#getRedoParameter()
     * @see CommandContext
     * @see Context.State#READY
     * @see CommandExecutable#executeDo(Context)
     * @see CommandExecutable#executeUndo(Context)
     */
//    <T> Context<T> createContext(Object input);
    Context<T> createContext(Object input);


    /**
     * To execute command do with correct context state (default implementation)
     *
     * @param context context of redo execution
//     * @param <T>     the type of command result
     * @see Context
     * @see Context.State#WORK
     * @see RootCommand#doCommand(Context)
     */
    default void executeDo(Context<T> context) {
//        default <T> void executeDo(Context<T> context) {
        context.setState(Context.State.DONE);
    }

    /**
     * To do command execution with command context
     *
     * @param context context of redo execution
//     * @param <T>     the type of command result
     * @see Context
     */
//    <T> void doCommand(Context<T> context);
    void doCommand(Context<T> context);

    /**
     * To rollback command's execution with correct context state (default implementation)
     * <BR/> the type of command result doesn't matter
     *
     * @param context context of redo execution
//     * @param <T>     the type of command result
     * @see Context
     * @see Context#getUndoParameter()
     */
//    default <T> void executeUndo(Context<T> context) {
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
//    <T> void undoCommand(Context<T> context);
    void undoCommand(Context<?> context);
}
