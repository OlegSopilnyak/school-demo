package oleg.sopilnyak.test.service.command.type.base;

import java.util.Optional;

/**
 * Type: The context of the command execution
 *
 * @see SchoolCommand
 */
public interface Context<T> {
    /**
     * To get the command associated with the context
     *
     * @return command instance
     * @see SchoolCommand
     */
    SchoolCommand<T> getCommand();

    /**
     * To get the state of the context
     *
     * @return current context's state
     */
    State getState();

    /**
     * To set up current state of the context
     *
     * @param state new current context's state
     */
    void setState(State state);

    /**
     * To get parameter value for command execution
     *
     * @return the value of parameter
     */
    Object getDoParameter();

    /**
     * To set up parameter value for command execution
     *
     * @param parameter the value
     */
    void setDoParameter(Object parameter);

    /**
     * To get parameter value for rollback previous command execution changes
     *
     * @return the value of parameter
     */
    Object getUndoParameter();

    /**
     * To set up parameter value for rollback changes
     *
     * @param parameter the value
     */
    void setUndoParameter(Object parameter);

    /**
     * To get the result of command execution
     *
     * @return the value of result
     * @see State#DONE
     */
    Optional<T> getResult();

    /**
     * To set up the result of command execution
     *
     * @param value the value of result
     * @see State#DONE
     */
    void setResult(T value);

    /**
     * To get exception occurring during command execution
     *
     * @return thrown exception instance
     * @see Exception
     */
    Exception getException();
    /**
     * To set up exception occurring during command execution
     *
     * @param exception thrown exception instance
     * @see Exception
     */
    void setException(Exception exception);

    /**
     * Command execution for context is failed
     *
     * @param exception thrown exception during command execution
     * @see Exception
     * @see Context##setState(State)
     * @see Context.State#FAIL
     */
    default void failed(Exception exception) {
        setState(State.FAIL);
        setException(exception);
    }

    /**
     * The enumeration of context's state
     */
    enum State {
        // context is built
        INIT,
        // context is ready to command redo(...)
        READY,
        // command execution is in progress
        WORK,
        // command redo(...) is finished successfully
        DONE,
        // command execution is finished unsuccessfully
        FAIL,
        // command undo(...) is finished successfully
        UNDONE
    }
}
