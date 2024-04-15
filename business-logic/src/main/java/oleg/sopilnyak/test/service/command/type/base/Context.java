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
    Object getRedoParameter();

    /**
     * To set up parameter value for command execution
     *
     * @param parameter the value
     */
    void setRedoParameter(Object parameter);

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
    Optional getResult();

    /**
     * To set up the result of command execution
     *
     * @param value the value of result
     * @see State#DONE
     */
    void setResult(Object value);

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
     * Mark context as failed
     *
     * @param exception cause of failure
     * @see Exception
     * @see this#setState(State)
     * @see Context.State#FAIL
     * @see this#setException(Exception)
     */
    default void failed(Exception exception) {
        setState(State.FAIL);
        setException(exception);
    }

    /**
     * To check is context is done
     *
     * @return true if done
     * @see this#getState()
     * @see Context.State#DONE
     */
    default boolean isDone() {
        return getState() == State.DONE;
    }

    default boolean isReady() {
        return getState() == State.READY;
    }


    /**
     * To add change-context-state listener
     *
     * @param listener the listener of context-state changes
     * @see StateChangedListener
     */
    void addStateListener(StateChangedListener listener);

    /**
     * To remove change-context-state listener
     *
     * @param listener the listener of context-state changes
     * @see StateChangedListener
     */
    void removeStateListener(StateChangedListener listener);

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
        // further command execution is canceled
        CANCEL,
        // command undo(...) is finished successfully
        UNDONE
    }

    /**
     * The listener of context's state changing
     */
    interface StateChangedListener {
        void stateChanged(Context<?> context, State previous, State newOne);
    }
}
