package oleg.sopilnyak.test.service.command.type.base;

import oleg.sopilnyak.test.service.command.io.Input;

import java.time.Duration;
import java.time.Instant;
import java.util.Deque;
import java.util.Optional;

/**
 * Type: The context of the command execution
 *
 * @param <T> type of result
 * @see RootCommand
 */
public interface Context<T> {
    /**
     * To get the command associated with the context
     *
     * @return command instance
     * @see RootCommand
     */
    RootCommand<T> getCommand();

    /**
     * To get when command's execution is started
     *
     * @return the time when execution starts or null if it doesn't
     * @see Instant
     */
    Instant getStartedAt();

    /**
     * To get command's execution duration
     *
     * @return the value of last command execution duration or null if it doesn't
     * @see Duration
     */
    Duration getDuration();

    /**
     * To get the state of the context
     *
     * @return current context's state
     * @see State
     */
    State getState();

    /**
     * To set up current state of the context
     *
     * @param state new current context's state
     * @see State
     */
    void setState(State state);

    /**
     * To get input parameter value for do command execution
     *
     * @param <R> type of do-input-parameter
     * @return the value of parameter as Input
     * @see Input
     */
    <R> Input<R> getRedoParameter();
//    <R> R getRedoParameter();

    /**
     * To set up parameter value for command execution
     *
     * @param parameter the value
     */
//    void setRedoParameter(Object parameter);

    /**
     * To get input parameter value for rollback previous command execution changes
     *
     * @param <U> type of undo-parameter
     * @return the value of parameter as Input
     */
    <U> Input<U> getUndoParameter();
//    <U> U getUndoParameter();

    /**
     * To set up parameter value for rollback changes
     *
     * @param parameter the value
     */
//    void setUndoParameter(Object parameter);

    /**
     * To get the result of command execution
     *
     * @return the value of result
     * @see Optional
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
     * To get context's life-cycle activities history
     *
     * @return context's history instance
     */
    LifeCycleHistory getHistory();

    /**
     * Mark context as failed
     *
     * @param exception cause of failure
     * @return failed context instance
     * @see Exception
     * @see this#setState(State)
     * @see Context.State#FAIL
     * @see this#setException(Exception)
     */
    default Context<T> failed(Exception exception) {
        setState(State.FAIL);
        setException(exception);
        return this;
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

    /**
     * To check is context is undone
     *
     * @return true if done
     * @see this#getState()
     * @see Context.State#UNDONE
     */
    default boolean isUndone() {
        return getState() == State.UNDONE;
    }

    /**
     * To check is context is ready to do
     *
     * @return true if done
     * @see this#getState()
     * @see Context.State#READY
     */
    default boolean isReady() {
        return getState() == State.READY;
    }

    /**
     * To check is context is failed after do or undo
     *
     * @return true if done
     * @see this#getState()
     * @see Context.State#FAIL
     */
    default boolean isFailed() {
        return getState() == State.FAIL;
    }

    /**
     * To check is context is before command-do state
     *
     * @return true if done
     * @see this#getState()
     * @see Context.State#WORK
     */
    default boolean isWorking() {
        return getState() == State.WORK;
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
        // command do execution is finished unsuccessfully
        FAIL,
        // further command execution is canceled
        CANCEL,
        // command undo execution is finished successfully
        UNDONE
    }

    /**
     * The listener of context's state changing
     */
    interface StateChangedListener {
        /**
         * State changed event processing method
         *
         * @param context the context where state was changed
         * @param previous previous context state value
         * @param current new context state value
         */
        void stateChanged(Context<?> context, State previous, State current);
    }

    /**
     * The history of context's life cycle
     */
    interface LifeCycleHistory {
        /**
         * To get context's states history
         *
         * @return deque of states
         */
        Deque<State> states();

        /**
         * To get when context started history (do/undo)
         *
         * @return deque of time-marks
         */
        Deque<Instant> started();

        /**
         * To get the duration of context running history (do/undo)
         *
         * @return deque of durations
         */
        Deque<Duration> durations();
    }
}
