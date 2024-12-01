package oleg.sopilnyak.test.service.command.executable.sys;

import lombok.*;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.command.type.base.RootCommand;
import org.springframework.util.ObjectUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static oleg.sopilnyak.test.service.command.type.base.Context.State.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommandContext<T> implements Context<T> {
    private RootCommand<T> command;
    private Object redoParameter;
    private Object undoParameter;
    private T resultData;
    private Exception exception;
    // the time when execution starts
    private Instant startedAt;
    // the value of command execution duration
    private Duration duration;

    @Setter(AccessLevel.NONE)
    private volatile State state;

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    @Builder.Default
    private final History history = new History();

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    @Builder.Default
    private final List<StateChangedListener> listeners =
            Collections.synchronizedList(new LinkedList<>(List.of(new InternalStateChangedListener())));

    /**
     * To get when command's execution is started
     *
     * @return the time when execution starts or null if it doesn't
     * @see Instant
     */
    @Override
    public Instant getStartedAt() {
        return startedAt;
    }

    /**
     * To get command's execution duration
     *
     * @return the value of last command execution duration or null if it doesn't
     * @see Duration
     */
    @Override
    public Duration getDuration() {
        return duration;
    }

    /**
     * To set up current state of the context
     *
     * @param newState new current context's state
     */
    @Override
    public void setState(final State newState) {
        if (newState != this.state) {
            final State previousState = this.state;
            this.state = newState;
            notifyStateChangedListeners(previousState, newState);
        }
    }

    /**
     * To set up parameter value for command execution
     *
     * @param parameter the value
     */
    @Override
    public void setRedoParameter(Object parameter) {
        this.redoParameter = parameter;
        if (state == INIT) {
            setState(READY);
        }
    }

    /**
     * To set up parameter value for rollback changes
     *
     * @param parameter the value
     */
    @Override
    public void setUndoParameter(Object parameter) {
        if (isDone() || isWorking()) {
            undoParameter = parameter;
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
     * To get states of context during context's life-cycle
     *
     * @return list of states
     * @see oleg.sopilnyak.test.service.command.type.base.Context.State
     */
    public List<State> getStatesHistory() {
        return List.copyOf(history.states);
    }

    /**
     * To get history of startedAt command execution items
     *
     * @return list of startedAt items
     * @see StartedAtHistoryItem
     */
    public List<StartedAtHistoryItem> getStartedAtHistory() {
        return List.copyOf(history.started);
    }

    /**
     * To get history of duration command execution items
     *
     * @return list of duration items
     * @see WorkedHistoryItem
     */
    public List<WorkedHistoryItem> getWorkedHistory() {
        return List.copyOf(history.worked);
    }

    /**
     * To add change context state listener
     *
     * @param listener the listener of context-state changes
     * @see StateChangedListener
     */
    @Override
    public void addStateListener(final StateChangedListener listener) {
        listeners.add(listener);
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
    private void notifyStateChangedListeners(final State previous, final State last) {
        if (ObjectUtils.isEmpty(listeners)) return;
        // delivery state-changed notification
        listeners.forEach(listener -> listener.stateChanged(this, previous, last));
    }

    // nested class
    private static class InternalStateChangedListener implements StateChangedListener {
        private static final Set<State> readyToStart = Set.of(READY, DONE);
        private static final Set<State> alreadyFinished = Set.of(DONE, UNDONE, FAIL);

        @Override
        public void stateChanged(Context<?> context, State previous, State current) {
            final CommandContext<?> commandContext = (CommandContext<?>) context;
            if (nonNull(previous) && commandExecutionReadyToStart(previous, current)) {
                commandExecutionStarted(previous, commandContext);
            } else if (commandExecutionAlreadyFinished(previous, current)) {
                commandExecutionFinished(current, commandContext);
            }
            commandContext.history.add(current);

        }

        // private methods
        private static boolean commandExecutionReadyToStart(final State previous, final State current) {
            return readyToStart.contains(previous) && current == WORK;
        }

        private static void commandExecutionStarted(State previous, final CommandContext<?> context) {
            // command execution is started
            final Instant now = Instant.now();
            context.history.add(now, previous);
            context.setStartedAt(now);
        }

        private static boolean commandExecutionAlreadyFinished(final State previous, final State current) {
            return alreadyFinished.contains(current) && previous == WORK;
        }

        private static void commandExecutionFinished(final State current, final CommandContext<?> context) {
            // command execution is finished
            if (isNull(context.getStartedAt())) {
                return;
            }
            // store execution duration
            final Duration duration = Duration.between(context.getStartedAt(), Instant.now());
            context.history.add(duration, current);
            context.setDuration(duration);
        }
    }

    private static class History {
        private final List<State> states = new LinkedList<>();
        private final List<StartedAtHistoryItem> started = new LinkedList<>();
        private final List<WorkedHistoryItem> worked = new LinkedList<>();

        void add(final State state) {
            states.add(state);
        }

        void add(final Instant startedAt, final State startedAfter) {
            started.add(new StartedAtHistoryItem(startedAt, startedAfter));
        }

        void add(final Duration duration, final State finishedBy) {
            worked.add(new WorkedHistoryItem(duration, finishedBy));
        }
    }

    public record StartedAtHistoryItem(Instant startedAt, State startedAfter) {

        @Override
        public String toString() {
            return "Started at '" + startedAt.toString() + "' after :" + startedAfter;
        }
    }

    public record WorkedHistoryItem(Duration worked, State finishedBy) {

        @Override
        public String toString() {
            return "Working " + worked.toMillis() + "ms till :" + finishedBy;
        }
    }
}
