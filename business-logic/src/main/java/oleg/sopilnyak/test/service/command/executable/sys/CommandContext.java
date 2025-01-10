package oleg.sopilnyak.test.service.command.executable.sys;

import lombok.*;
import oleg.sopilnyak.test.service.command.io.Input;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.command.type.base.RootCommand;
import org.springframework.util.ObjectUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.Objects.nonNull;
import static oleg.sopilnyak.test.service.command.type.base.Context.State.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommandContext<T> implements Context<T> {
    private RootCommand<T> command;
    private Input<?> redoParameter;
    private Input<?> undoParameter;
    //    private Object redoParameter;
//    private Object undoParameter;
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
//    @Override
//    public void setRedoParameter(Object parameter) {
//        redoParameter = parameter;
//    }

    /**
     * To set up parameter value for rollback changes
     *
     * @param parameter the value
     */
//    @Override
//    public void setUndoParameter(Object parameter) {
//        undoParameter = parameter;
//    }

    /**
     * To set up input parameter value for command execution
     *
     * @param parameter the value
     */
//    @Override
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
     */
//    @Override
    public <U> void setUndoParameter(final Input<U> parameter) {
        if (canUndo(state)) {
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
     * To get context's life-cycle activities history
     *
     * @return context's history instance
     */
    @Override
    public LifeCycleHistory getHistory() {
        return history;
    }

    /**
     * To get states of context during context's life-cycle
     *
     * @return list of states
     * @see oleg.sopilnyak.test.service.command.type.base.Context.State
     */
    public List<StateChangedHistoryItem> getStatesHistory() {
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
    private void notifyStateChangedListeners(final State previous, final State current) {
        if (ObjectUtils.isEmpty(listeners)) return;
        // delivery state-changed notification
        listeners.forEach(listener -> listener.stateChanged(this, previous, current));
    }

    private static boolean canUndo(State state) {
        return state == DONE || state == WORK;
    }

    // nested class
    private static class InternalStateChangedListener implements StateChangedListener {
        private static final Set<State> readyToStart = Set.of(READY, DONE);
        private static final Set<State> alreadyFinished = Set.of(DONE, UNDONE, FAIL);

        @Override
        public void stateChanged(final Context<?> context, final State previous, final State current) {
            final CommandContext<?> commandContext = (CommandContext<?>) context;
            if (nonNull(previous) && readyToStart.contains(previous) && current == WORK) {
                commandExecutionStarted(previous, commandContext);
            } else if (alreadyFinished.contains(current) && previous == WORK) {
                commandExecutionFinished(current, commandContext);
            }
            commandContext.history.add(current);
        }

        // private methods
        private static void commandExecutionStarted(final State previous, final CommandContext<?> context) {
            // command execution is started
            final Instant timeMark = Instant.now();
            context.history.add(timeMark, previous);
            context.setStartedAt(timeMark);
        }

        private static void commandExecutionFinished(final State current, final CommandContext<?> context) {
            // command execution is finished
            final Instant startedAt = context.getStartedAt();
            if (nonNull(startedAt)) {
                // store execution duration
                final Duration duration = Duration.between(startedAt, Instant.now());
                context.history.add(duration, current);
                context.setDuration(duration);
            }
        }
    }

    private static class History implements LifeCycleHistory {
        private final List<StateChangedHistoryItem> states = new LinkedList<>();
        private final List<StartedAtHistoryItem> started = new LinkedList<>();
        private final List<WorkedHistoryItem> worked = new LinkedList<>();

        void add(final State state) {
            states.add(new StateChangedHistoryItem(state, Instant.now()));
        }

        void add(final Instant startedAt, final State startedAfter) {
            started.add(new StartedAtHistoryItem(startedAt, startedAfter));
        }

        void add(final Duration duration, final State finishedBy) {
            worked.add(new WorkedHistoryItem(duration, finishedBy));
        }

        /**
         * To get context's states history
         *
         * @return deque of states
         */
        @Override
        public Deque<State> states() {
            return states.stream()
                    .map(item -> item.newContextState)
                    .collect(Collectors.toCollection(LinkedList::new));
        }

        /**
         * To get when context started history (do/undo)
         *
         * @return deque of time-marks
         */
        @Override
        public Deque<Instant> started() {
            return started.stream()
                    .map(item -> item.startedAt)
                    .collect(Collectors.toCollection(LinkedList::new));
        }

        /**
         * To get the duration of context running history (do/undo)
         *
         * @return deque of durations
         */
        @Override
        public Deque<Duration> durations() {
            return worked.stream()
                    .map(item -> item.worked)
                    .collect(Collectors.toCollection(LinkedList::new));
        }
    }

    public record StateChangedHistoryItem(State newContextState, Instant when) {
        @Override
        public String toString() {
            return "New Context State: " + newContextState + " Set Up at '" + when.toString() + "'";
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
