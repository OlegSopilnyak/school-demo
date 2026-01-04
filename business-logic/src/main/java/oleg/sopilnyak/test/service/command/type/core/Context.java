package oleg.sopilnyak.test.service.command.type.core;

import oleg.sopilnyak.test.service.command.io.Input;

import java.io.Serializable;
import java.time.Duration;
import java.time.Instant;
import java.util.Deque;
import java.util.Optional;
import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Type: The context of the command execution
 *
 * @param <T> type of result
 * @see RootCommand
 */
public interface Context<T> extends Serializable {

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
     * To check is context in particular state
     *
     * @param state context's state to check
     * @return true if context in the state
     * @see this#getState()
     */
    default boolean stateIs(final State state) {
        return state == getState();
    }

    /**
     * To check is context is done
     *
     * @return true if done
     * @see this#getState()
     * @see Context.State#DONE
     */
    @JsonIgnore
    default boolean isDone() {
        return stateIs(State.DONE);
    }

    /**
     * To check is context is undone
     *
     * @return true if done
     * @see this#getState()
     * @see Context.State#UNDONE
     */
    @JsonIgnore
    default boolean isUndone() {
        return stateIs(State.UNDONE);
    }

    /**
     * To check is context is ready to do
     *
     * @return true if done
     * @see this#getState()
     * @see Context.State#READY
     */
    @JsonIgnore
    default boolean isReady() {
        return stateIs(State.READY);
    }

    /**
     * To check is context is failed after do or undo
     *
     * @return true if done
     * @see this#getState()
     * @see Context.State#FAIL
     */
    @JsonIgnore
    default boolean isFailed() {
        return stateIs(State.FAIL);
    }

    /**
     * To check is context is before command-do state
     *
     * @return true if done
     * @see this#getState()
     * @see Context.State#WORK
     */
    @JsonIgnore
    default boolean isWorking() {
        return stateIs(State.WORK);
    }

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

    /**
     * To get input parameter value for rollback previous command execution changes
     *
     * @param <U> type of undo-parameter
     * @return the value of parameter as Input
     */
    <U> Input<U> getUndoParameter();

    /**
     * To get the result of command execution
     *
     * @return the value of result
     * @see Optional
     * @see State#DONE
     */
    @JsonIgnore
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
     * Mark context as failed
     *
     * @param exception cause of failure
     * @return failed context instance
     * @see Exception
     */
    Context<T> failed(Exception exception);

    /**
     * To get context's life-cycle activities history
     *
     * @return context's history instance
     */
    LifeCycleHistory getHistory();

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
         * State changed event doingMainLoop method
         *
         * @param context  the context where state was changed
         * @param previous previous context state value
         * @param current  current context state value
         */
        void stateChanged(Context<?> context, State previous, State current);
    }

    /**
     * The history of context's life cycle
     */
    interface LifeCycleHistory extends Serializable {
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
