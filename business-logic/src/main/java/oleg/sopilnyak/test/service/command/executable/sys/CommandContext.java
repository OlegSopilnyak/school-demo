package oleg.sopilnyak.test.service.command.executable.sys;

import static java.util.Objects.nonNull;
import static oleg.sopilnyak.test.service.command.io.IOFieldNames.TYPE_FIELD_NAME;
import static oleg.sopilnyak.test.service.command.io.IOFieldNames.VALUE_FIELD_NAME;
import static oleg.sopilnyak.test.service.command.type.base.Context.State.DONE;
import static oleg.sopilnyak.test.service.command.type.base.Context.State.FAIL;
import static oleg.sopilnyak.test.service.command.type.base.Context.State.INIT;
import static oleg.sopilnyak.test.service.command.type.base.Context.State.READY;
import static oleg.sopilnyak.test.service.command.type.base.Context.State.UNDONE;
import static oleg.sopilnyak.test.service.command.type.base.Context.State.WORK;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import oleg.sopilnyak.test.service.command.io.Input;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.command.type.base.RootCommand;
import org.springframework.util.ObjectUtils;

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

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    @Builder.Default
    private final History history = new History();

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    @Builder.Default
    private final transient List<StateChangedListener> listeners =
            Collections.synchronizedList(new LinkedList<>(List.of(new InternalStateChangedListener())));

    /**
     * To set up current state of the context
     *
     * @param currentState new current context's state
     */
    @Override
    public void setState(final State currentState) {
        if (currentState != this.state) {
            final State previousState = this.state;
            this.state = currentState;
            notifyStateChangedListeners(previousState, currentState);
        }
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

    /**
     * The type of the context's history
     */
    @Data
    @JsonSerialize(using = History.Serializer.class)
//    @JsonDeserialize(using = History.Deserializer.class)
    public static class History implements LifeCycleHistory {
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

        /**
         * JSON: Serializer for History
         *
         * @see StdSerializer
         * @see History
         */
        public static class Serializer extends StdSerializer<LifeCycleHistory> {
            public Serializer() {
                this(null);
            }

            protected Serializer(Class<LifeCycleHistory> t) {
                super(t);
            }

            public void serialize(final LifeCycleHistory parameter,
                                  final JsonGenerator generator,
                                  final SerializerProvider serializerProvider) throws IOException {
                generator.writeStartObject();
                generator.writeStringField(TYPE_FIELD_NAME, parameter.getClass().getName());
                generator.writeFieldName(VALUE_FIELD_NAME);
                generator.writeStartObject();
                serializeStates(parameter.states(), generator);
                serializeStarted(parameter.started(), generator);
                serializeDurations(parameter.durations(), generator);
                generator.writeEndObject();
                generator.writeEndObject();
            }

            private void serializeDurations(Deque<Duration> durations, JsonGenerator generator) throws IOException {
                final ObjectMapper mapper = (ObjectMapper) generator.getCodec();
                generator.writeFieldName("durations");
                generator.writeStartArray();
                for (final Duration duration : durations) {
                    generator.writeString(mapper.writeValueAsString(duration));
                }
                generator.writeEndArray();
            }

            private void serializeStarted(Deque<Instant> started, JsonGenerator generator) throws IOException {
                final ObjectMapper mapper = (ObjectMapper) generator.getCodec();
                generator.writeFieldName("started");
                generator.writeStartArray();
                for (Instant startedAt : started) {
                    generator.writeString(mapper.writeValueAsString(startedAt));
                }
                generator.writeEndArray();
            }

            private void serializeStates(Deque<State> states, JsonGenerator generator) throws IOException {
                generator.writeFieldName("states");
                generator.writeStartArray();
                for (final State state : states) {
                    generator.writeString(state.name());
                }
                generator.writeEndArray();
            }
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
