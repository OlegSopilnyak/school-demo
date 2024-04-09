package oleg.sopilnyak.test.service.command.type.base;

import org.slf4j.Logger;

import java.util.Collection;

/**
 * Type: Command to execute the couple of commands
 */
public interface CompositeCommand<T> extends SchoolCommand<T> {

    /**
     * To get reference to command's logger
     *
     * @return reference to the logger
     */
    Logger getLog();

    /**
     * To get the collection of commands used it composite
     *
     * @return collection of included commands
     */
    Collection<SchoolCommand> commands();

    /**
     * To add the command
     *
     * @param command the instance to add
     */
    void add(SchoolCommand command);

    /**
     * To create the execution context (make contexts for each command)
     *
     * @param parameter command's input parameter value
     * @return built context
     * @see CompositeCommand#commands()
     */
    default Context<T> prepareContexts(Object parameter) {
        return createContext(parameter);
    }

    /**
     * To execute command
     *
     * @param context context of redo execution
     * @see Context
     */
    @Override
    default void redo(Context<T> context) {
        if (isWrongRedoStateOf(context)) {
            getLog().warn("Cannot do redo of command {} with context:state '{}'", getId(), context.getState());
            context.setState(Context.State.FAIL);
        } else {
            // start redo with correct context state
            context.setState(Context.State.WORK);
            doRedo(context);
        }
    }

    /**
     * To execute command redo with correct context state
     *
     * @param context context of redo execution
     * @see Context
     * @see Context.State#WORK
     */
    default void doRedo(Context<T> context) {
        context.setState(Context.State.DONE);
    }

    /**
     * To rollback command's execution
     *
     * @param context context of redo execution
     * @see Context
     * @see Context#getUndoParameter()
     */
    @Override
    default void undo(Context<T> context) {
        if (isWrongUndoStateOf(context)) {
            getLog().warn("Cannot do undo of command {} with context:state '{}'", getId(), context.getState());
            context.setState(Context.State.FAIL);
        } else {
            // start undo with correct context state
            context.setState(Context.State.WORK);
            doUndo(context);
        }
    }

    /**
     * To rollback command's execution with correct context state
     *
     * @param context context of redo execution
     * @see Context
     * @see Context#getUndoParameter()
     */
    default void doUndo(Context<T> context) {
        context.setState(Context.State.UNDONE);
    }
}
