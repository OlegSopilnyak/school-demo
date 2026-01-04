package oleg.sopilnyak.test.service.command.executable.core.context.history;

import static oleg.sopilnyak.test.service.command.io.IOFieldNames.TYPE_FIELD_NAME;
import static oleg.sopilnyak.test.service.command.io.IOFieldNames.VALUE_FIELD_NAME;

import oleg.sopilnyak.test.service.command.type.core.Context;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

/**
 * The type of the context's history
 *
 * @see Context.LifeCycleHistory
 */
@Data
@Getter(AccessLevel.NONE)
@Setter(AccessLevel.NONE)
@Builder
@JsonSerialize(using = History.Serializer.class)
@JsonDeserialize(using = History.Deserializer.class)
public class History implements Context.LifeCycleHistory {
    public static final String DURATIONS_FILED_NAME = "durations";
    public static final String STARTED_FILED_NAME = "started";
    public static final String STATES_FILED_NAME = "states";
    @Builder.Default
    private transient List<StateChangedHistoryItem> states = new LinkedList<>();
    @Builder.Default
    private transient List<StartedAtHistoryItem> started = new LinkedList<>();
    @Builder.Default
    private transient List<WorkedHistoryItem> worked = new LinkedList<>();

    public void add(final Context.State state) {
        states.add(new StateChangedHistoryItem(state, Instant.now()));
    }

    public void add(final Instant startedAt, final Context.State startedAfter) {
        started.add(new StartedAtHistoryItem(startedAt, startedAfter));
    }

    public void add(final Duration duration, final Context.State finishedBy) {
        worked.add(new WorkedHistoryItem(duration, finishedBy));
    }

    @Override
    public final boolean equals(Object o) {
        return o instanceof History history ? equals(history)
                :
                o instanceof Context.LifeCycleHistory contextHistory && equals(contextHistory);
    }

    public final boolean equals(final History history) {
        return history.states.equals(states) && history.worked.equals(worked) && history.started.equals(started);
    }

    public final boolean equals(final Context.LifeCycleHistory history) {
        return history.states().equals(states()) && history.durations().equals(durations()) && history.started().equals(started());
    }

    @Override
    public int hashCode() {
        int result = states.hashCode();
        result = 31 * result + started.hashCode();
        result = 31 * result + worked.hashCode();
        return result;
    }

    /**
     * To get context's states history
     *
     * @return deque of states
     */
    @Override
    public Deque<Context.State> states() {
        return states.stream()
                .map(item -> item.state)
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
    static class Serializer extends StdSerializer<History> {
        public Serializer() {
            this(null);
        }

        protected Serializer(Class<History> t) {
            super(t);
        }

        public void serialize(final History parameter,
                              final JsonGenerator generator,
                              final SerializerProvider serializerProvider) throws IOException {
            generator.writeStartObject();
            generator.writeStringField(TYPE_FIELD_NAME, parameter.getClass().getName());
            generator.writeFieldName(VALUE_FIELD_NAME);
            generator.writeStartObject();
            serializeStates(parameter, generator);
            serializeStarted(parameter, generator);
            serializeDurations(parameter, generator);
            generator.writeEndObject();
            generator.writeEndObject();
        }

        private void serializeStates(final History history, final JsonGenerator generator) throws IOException {
            final ObjectMapper mapper = (ObjectMapper) generator.getCodec();
            generator.writeFieldName(STATES_FILED_NAME);
            generator.writeStartArray();
            for (var state : history.states) {
                generator.writeRawValue(mapper.writeValueAsString(state));
            }
            generator.writeEndArray();
        }

        private void serializeStarted(final History history, final JsonGenerator generator) throws IOException {
            final ObjectMapper mapper = (ObjectMapper) generator.getCodec();
            generator.writeFieldName(STARTED_FILED_NAME);
            generator.writeStartArray();
            for (var startedAt : history.started) {
                generator.writeRawValue(mapper.writeValueAsString(startedAt));
            }
            generator.writeEndArray();
        }

        private void serializeDurations(final History history, final JsonGenerator generator) throws IOException {
            final ObjectMapper mapper = (ObjectMapper) generator.getCodec();
            generator.writeFieldName(DURATIONS_FILED_NAME);
            generator.writeStartArray();
            for (final var duration : history.worked) {
                generator.writeRawValue(mapper.writeValueAsString(duration));
            }
            generator.writeEndArray();
        }

    }

    static class Deserializer extends StdDeserializer<Context.LifeCycleHistory> {
        public Deserializer() {
            super(Context.LifeCycleHistory.class);
        }

        @Override
        public Context.LifeCycleHistory deserialize(JsonParser jsonParser, DeserializationContext ctxt) throws IOException {
            return restoreHistoryBy(jsonParser.readValueAsTree(), jsonParser);
        }

        private Context.LifeCycleHistory restoreHistoryBy(final TreeNode historyNode,
                                                          final JsonParser jsonParser) throws IOException {
            restoreHistoryClass(historyNode);
            return restoreHistoryInstance(jsonParser, historyNode.get(VALUE_FIELD_NAME));
        }

        private static void restoreHistoryClass(final TreeNode historyNode) throws IOException {
            final TreeNode typeNode = historyNode.get(TYPE_FIELD_NAME);
            if (typeNode instanceof TextNode typeTextNode) {
                try {
                    Class.forName(typeTextNode.asText()).asSubclass(Context.LifeCycleHistory.class);
                } catch (ClassNotFoundException | ClassCastException e) {
                    // class not found or class is not Context.History
                    throw new IOException("Wrong type name of history class " + typeTextNode.asText(), e);
                }
            } else {
                throw new IOException("Wrong node-type of history's type: " + typeNode.getClass().getName());
            }
        }

        private Context.LifeCycleHistory restoreHistoryInstance(JsonParser jsonParser, TreeNode treeNode) throws IOException {
            final HistoryBuilder historyBuilder = History.builder();
            deserializeStates(historyBuilder, jsonParser, treeNode.get(STATES_FILED_NAME));
            deserializeStarted(historyBuilder, jsonParser, treeNode.get(STARTED_FILED_NAME));
            deserializeDurations(historyBuilder, jsonParser, treeNode.get(DURATIONS_FILED_NAME));
            return historyBuilder.build();
        }

        private void deserializeStates(final HistoryBuilder historyBuilder,
                                       final JsonParser jsonParser, final TreeNode statesNode) throws IOException {
            final List<StateChangedHistoryItem> items = new LinkedList<>();
            historyBuilder.states(items);
            if (statesNode instanceof ArrayNode arrayNode) {
                final ObjectMapper mapper = (ObjectMapper) jsonParser.getCodec();
                for (final JsonNode node : arrayNode) {
                    items.add(mapper.readValue(node.toString(), StateChangedHistoryItem.class));
                }
                return;
            }
            throw new IOException("Wrong type of states node " + statesNode.toString());
        }

        private void deserializeStarted(final HistoryBuilder historyBuilder,
                                        final JsonParser jsonParser, final TreeNode startedNode) throws IOException {
            final List<StartedAtHistoryItem> items = new LinkedList<>();
            historyBuilder.started(items);
            if (startedNode instanceof ArrayNode arrayNode) {
                final ObjectMapper mapper = (ObjectMapper) jsonParser.getCodec();
                for (final JsonNode node : arrayNode) {
                    items.add(mapper.readValue(node.toString(), StartedAtHistoryItem.class));
                }
                return;
            }
            throw new IOException("Wrong type of started node " + startedNode.toString());
        }

        private void deserializeDurations(final HistoryBuilder historyBuilder,
                                          final JsonParser jsonParser, final TreeNode startedNode) throws IOException {
            final List<WorkedHistoryItem> items = new LinkedList<>();
            historyBuilder.worked(items);
            if (startedNode instanceof ArrayNode arrayNode) {
                final ObjectMapper mapper = (ObjectMapper) jsonParser.getCodec();
                for (final JsonNode node : arrayNode) {
                    items.add(mapper.readValue(node.toString(), WorkedHistoryItem.class));
                }
                return;
            }
            throw new IOException("Wrong type of worked node " + startedNode.toString());
        }
    }

    /**
     * Item of context history of states
     *
     * @see Context.LifeCycleHistory#states()
     */
    record StateChangedHistoryItem(Context.State state, Instant setup) {
        @Override
        public String toString() {
            return "ContextState:" + state + " Setup at '" + setup.toString() + "'";
        }
    }

    /**
     * Item of context history of started time marks
     *
     * @param startedAt when it started
     * @param state     after which state it happened
     * @see Context.LifeCycleHistory#started()
     */
    record StartedAtHistoryItem(Instant startedAt, Context.State state) {
        @Override
        public String toString() {
            return "Started at '" + startedAt.toString() + "' After :" + state;
        }
    }

    /**
     * Item of context history of durations command executions
     *
     * @param worked how long command worked
     * @param state  which state led to finish
     * @see Context.LifeCycleHistory#durations()
     */
    record WorkedHistoryItem(Duration worked, Context.State state) {
        @Override
        public String toString() {
            return "Worked " + worked.toNanos() + " ns Till State:" + state;
        }
    }
}
