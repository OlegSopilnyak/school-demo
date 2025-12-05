package oleg.sopilnyak.test.service.message;

import oleg.sopilnyak.test.school.common.business.facade.ActionContext;
import oleg.sopilnyak.test.service.command.io.IOBase;
import oleg.sopilnyak.test.service.command.type.base.Context;

import java.io.IOException;
import java.util.Arrays;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.node.TextNode;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import lombok.AllArgsConstructor;
import lombok.Data;


/**
 * Message: message to commands subsystem (parent of any type of messages)
 *
 * @param <T> type of command execution result
 * @see DoCommandMessage
 */
@Data
@AllArgsConstructor
@JsonSerialize(using = BaseCommandMessage.Serializer.class)
@JsonDeserialize(using = BaseCommandMessage.Deserializer.class)
public abstract class BaseCommandMessage<T> implements CommandMessage<T> {
    public static final BaseCommandMessage<?> EMPTY = new BaseCommandMessage<>(null, null, null) {
        @Override
        public Direction getDirection() {
            return null;
        }
    };
    private static final String CORRELATION_ID_FIELD_NAME = "correlation-id";
    private static final String ACTION_CONTEXT_FIELD_NAME = "processing-context";
    private static final String COMMAND_CONTEXT_FIELD_NAME = "command-context";
    private static final String DIRECTION_FIELD_NAME = "direction";

    // correlation ID of the message
    private String correlationId;
    // the processing context of command's execution
    @JsonDeserialize(using = IOBase.ActionContextDeserializer.class)
    private ActionContext actionContext;
    // the context of command's execution
    private Context<T> context;

    /**
     * To validate message content after build or restore.
     * Throws IllegalArgumentException if validation fails.
     */
    public final void validate() {
        if (correlationId == null || correlationId.isBlank()) {
            throw new IllegalArgumentException("Correlation ID must not be null or empty");
        }
        if (actionContext == null) {
            throw new IllegalArgumentException("Action context must not be null");
        }
        if (context == null) {
            throw new IllegalArgumentException("Command context must not be null");
        }
    }

    /**
     * JSON: Serializer for types extends BaseCommandMessage
     *
     * @see StdSerializer
     */
    static class Serializer<T extends BaseCommandMessage<?>> extends StdSerializer<T> {
        public Serializer() {
            this(null);
        }

        protected Serializer(Class<T> t) {
            super(t);
        }

        @Override
        public void serialize(final T message,
                              final JsonGenerator generator,
                              final SerializerProvider serializerProvider) throws IOException {
            generator.writeStartObject();
            generator.writeStringField(CORRELATION_ID_FIELD_NAME, message.getCorrelationId());
            serialize(message.getActionContext(), generator);
            serialize(message.getContext(), generator);
            generator.writeStringField(DIRECTION_FIELD_NAME, message.getDirection().name());
            generator.writeEndObject();
        }

        private static void serialize(final ActionContext actionContext, JsonGenerator generator) throws IOException {
            generator.writeFieldName(ACTION_CONTEXT_FIELD_NAME);
            if (actionContext != null) {
                final ObjectMapper mapper = (ObjectMapper) generator.getCodec();
                generator.writeRawValue(mapper.writeValueAsString(actionContext));
            } else {
                generator.writeNull();
            }
        }

        private static void serialize(final Context<?> context, JsonGenerator generator) throws IOException {
            generator.writeFieldName(COMMAND_CONTEXT_FIELD_NAME);
            if (context != null) {
                final ObjectMapper mapper = (ObjectMapper) generator.getCodec();
                generator.writeRawValue(mapper.writeValueAsString(context));
            } else {
                generator.writeNull();
            }
        }
    }

    /**
     * JSON: Deserializer for types extends BaseCommandMessage
     *
     * @see StdDeserializer
     */
    static class Deserializer<T extends BaseCommandMessage<?>, R> extends StdDeserializer<T> {
        private final IOBase.ActionContextDeserializer actionContextDeserializer = new IOBase.ActionContextDeserializer();

        public Deserializer() {
            this(BaseCommandMessage.class);
        }

        protected Deserializer(Class<?> vc) {
            super(vc);
        }

        @SuppressWarnings("unchecked")
        @Override
        public T deserialize(final JsonParser jsonParser, final DeserializationContext deserializationContext)
                throws IOException {
            final TreeNode treeNode = jsonParser.readValueAsTree();
            final Direction direction = toDirection(stringValueOf(treeNode.get(DIRECTION_FIELD_NAME)));
            if (direction == null) {
                throw new IllegalArgumentException("Direction field is missing or invalid");
            }
            final String correlationId = stringValueOf(treeNode.get(CORRELATION_ID_FIELD_NAME));
            final ObjectMapper mapper = (ObjectMapper) jsonParser.getCodec();
            final ActionContext actionContext = deserializeActionContext(treeNode.get(ACTION_CONTEXT_FIELD_NAME), mapper);
            final Context<R> commandContext = deserializeCommandContext(treeNode.get(COMMAND_CONTEXT_FIELD_NAME), mapper);
            return (T) switch (direction) {
                case DO -> DoCommandMessage.<R>builder()
                        .correlationId(correlationId).actionContext(actionContext).context(commandContext)
                        .build();
                case UNDO -> UndoCommandMessage.builder()
                        .correlationId(correlationId).actionContext(actionContext).context(commandContext)
                        .build();
                default ->  null;
            };
        }

        @SuppressWarnings("unchecked")
        private Context<R> deserializeCommandContext(final TreeNode node, final ObjectMapper mapper) throws IOException {
            return mapper.readValue(node.toString(), Context.class);
        }

        private ActionContext deserializeActionContext(final TreeNode node, final ObjectMapper mapper) throws IOException {
            return actionContextDeserializer.deserialize(mapper.createParser(node.toString()), null);
        }

        private static Direction toDirection(final String value) {
            return value == null ?
                    null
                    :
                    Arrays.stream(Direction.values()).filter(val -> val.name().equals(value))
                            .findFirst()
                            .orElse(null);
        }

        private static String stringValueOf(final TreeNode node) {
            return node instanceof TextNode textNode ? textNode.asText() : null;
        }
    }
}
