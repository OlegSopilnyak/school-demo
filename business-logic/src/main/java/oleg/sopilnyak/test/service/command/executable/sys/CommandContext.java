package oleg.sopilnyak.test.service.command.executable.sys;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static oleg.sopilnyak.test.service.command.type.base.Context.State.DONE;
import static oleg.sopilnyak.test.service.command.type.base.Context.State.FAIL;
import static oleg.sopilnyak.test.service.command.type.base.Context.State.INIT;
import static oleg.sopilnyak.test.service.command.type.base.Context.State.READY;
import static oleg.sopilnyak.test.service.command.type.base.Context.State.UNDONE;
import static oleg.sopilnyak.test.service.command.type.base.Context.State.WORK;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Predicate;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import oleg.sopilnyak.test.service.command.executable.sys.context.History;
import oleg.sopilnyak.test.service.command.io.Input;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.command.type.base.RootCommand;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommandContext<T> implements Context<T> {
    private RootCommand<T> command;
    @JsonDeserialize(using = Input.ParameterDeserializer.class)
    private Input<?> redoParameter;
    @JsonDeserialize(using = Input.ParameterDeserializer.class)
    private Input<?> undoParameter;
    private T resultData;
    private Exception exception;
    // the time when execution starts
    private Instant startedAt;
    // the value of command execution duration
    private Duration duration;

    @Setter(AccessLevel.NONE)
    private volatile State state;

    @Setter(AccessLevel.NONE)
    @Builder.Default
    private final History history = History.builder().build();

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    @Builder.Default
    private final Queue<StateChangedListener> listeners = new ConcurrentLinkedQueue<>(Set.of(new InternalStateChangedListener()));

    /**
     * To set up current state of the context
     *
     * @param currentState new current context's state
     */
    @Override
    public void setState(final State currentState) {
        if (currentState != this.state) {
            final State previousState = this.state;
            listeners.forEach(listener -> listener.stateChanged(this, previousState, currentState));
            this.state = currentState;
        }
    }

    /**
     * To save to context-history the current state of context (just changed)
     *
     * @param state current state for history
     * @see Context#getState()
     * @see LifeCycleHistory
     */
    @Override
    public void stateChangedTo(final State state) {
        history.add(state);
    }

    /**
     * To save to context-history time when command started execution with context
     *
     * @param startedAt    when command start execution
     * @param startedAfter which state was before
     * @see Context#getState()
     * @see Context#getStartedAt()
     * @see LifeCycleHistory
     * @see State#WORK
     * @see RootCommand#executeDo(Context)
     * @see RootCommand#executeUndo(Context)
     */
    @Override
    public void commandExecutionStarted(final Instant startedAt, final State startedAfter) {
        history.add(startedAt, startedAfter);
        setStartedAt(startedAt);
    }

    /**
     * To save to context-history duration of command execution with context
     *
     * @param finishedBy which state finishes the command execution
     * @see Context#getState()
     * @see Context#getDuration()
     * @see LifeCycleHistory
     * @see State#DONE
     * @see State#UNDONE
     * @see State#FAIL
     * @see RootCommand#executeDo(Context)
     * @see RootCommand#executeUndo(Context)
     */
    @Override
    public void commandExecutionFinishedBy(final State finishedBy) {
        if (isNull(startedAt)) {
            return;
        }
        // store execution duration
        final Duration lastDuration = Duration.between(startedAt, justNow());
        history.add(lastDuration, finishedBy);
        setDuration(lastDuration);
    }

    /**
     * To set up input parameter value for command execution
     *
     * @param parameter the value
     * @param <R>       type of do input parameter
     */
    public <R> void setRedoParameter(final Input<R> parameter) {
        this.redoParameter = parameter;
        if (state == INIT) {
            setState(READY);
        }
    }

    /**
     * To set up input parameter value for rollback changes
     *
     * @param parameter the value
     * @param <U>       type of undo input parameter
     */
    public <U> void setUndoParameter(final Input<U> parameter) {
        if (Set.of(DONE, WORK).contains(state)) {
            undoParameter = nonNull(parameter) ? parameter : Input.empty();
        }
    }

    /**
     * To get the result of command execution
     *
     * @return the value of result
     * @see State#DONE
     */
    @Override
    public Optional<T> getResult() {
        return Optional.ofNullable(resultData);
    }

    /**
     * To set up the result of command execution
     *
     * @param result the value of result
     * @see State#DONE
     */
    @Override
    public void setResult(T result) {
        if (isWorking()) {
            this.resultData = result;
            setState(DONE);
        }
    }

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
    @Override
    public Context<T> failed(Exception exception) {
        setState(State.FAIL);
        setException(exception);
        return this;
    }

    /**
     * To add change context state listener
     *
     * @param listener the listener of context-state changes
     * @see StateChangedListener
     */
    @Override
    public void addStateListener(final StateChangedListener listener) {
        listeners.offer(listener);
    }

    /**
     * To remove change-context-state listener
     *
     * @param listener the listener of context-state changes
     * @see StateChangedListener
     */
    @Override
    public void removeStateListener(final StateChangedListener listener) {
        listeners.remove(listener);
    }

    // private methods
    // to get current time mark
    private static Instant justNow() {
        return Instant.now();
    }

    // nested class
    private static class InternalStateChangedListener implements StateChangedListener {
        private final Predicate<State> isReady = Set.of(READY, DONE)::contains;
        private final Predicate<State> isWorks = WORK::equals;
        private final Predicate<State> isFinished = Set.of(DONE, UNDONE, FAIL)::contains;

        @Override
        public void stateChanged(final Context<?> context, final State previous, final State current) {
            if (nonNull(previous) && isReady.test(previous) && isWorks.test(current)) {
                // command execution is started
                context.commandExecutionStarted(justNow(), previous);
            } else if (isFinished.test(current) && isWorks.test(previous)) {
                // command execution is finished
                context.commandExecutionFinishedBy(current);
            }
            context.stateChangedTo(current);
        }
    }
}
