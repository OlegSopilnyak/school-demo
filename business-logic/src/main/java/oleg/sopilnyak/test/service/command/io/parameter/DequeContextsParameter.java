package oleg.sopilnyak.test.service.command.io.parameter;

import static oleg.sopilnyak.test.service.command.io.IOFieldNames.TYPE_FIELD_NAME;
import static oleg.sopilnyak.test.service.command.io.IOFieldNames.VALUE_FIELD_NAME;

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
import java.io.IOException;
import java.util.Deque;
import java.util.LinkedList;
import oleg.sopilnyak.test.service.command.executable.sys.MacroCommand;
import oleg.sopilnyak.test.service.command.io.Input;
import oleg.sopilnyak.test.service.command.type.base.Context;


/**
 * Type: I/O school-command contexts deque input parameter for undo composite command
 *
 * @see Input
 * @see Deque
 * @see Context
 * @see MacroCommand#rollbackNestedDone(Input)
 */
@JsonSerialize(using = DequeContextsParameter.Serializer.class)
@JsonDeserialize(using = DequeContextsParameter.Deserializer.class)
public class DequeContextsParameter implements Input<Deque<Context<?>>> {
    private final Deque<Context<?>> deque;

    public DequeContextsParameter(Deque<Context<?>> contextDeque) {
        this.deque = new LinkedList<>(contextDeque);
    }

    @Override
    public final boolean equals(Object o) {
        return o instanceof DequeContextsParameter that && deque.equals(that.deque);
    }

    @Override
    public int hashCode() {
        return deque.hashCode();
    }

    @Override
    public Deque<Context<?>> value() {
        return deque;
    }

    /**
     * JSON: Serializer for DequeContextsParameter
     *
     * @see StdSerializer
     * @see DequeContextsParameter
     */
    static class Serializer extends StdSerializer<DequeContextsParameter> {
        public Serializer() {
            this(null);
        }

        protected Serializer(Class<DequeContextsParameter> t) {
            super(t);
        }

        @Override
        public void serialize(final DequeContextsParameter parameter,
                              final JsonGenerator generator,
                              final SerializerProvider serializerProvider) throws IOException {
            generator.writeStartObject();
            generator.writeStringField(TYPE_FIELD_NAME, DequeContextsParameter.class.getName());
            generator.writeFieldName(VALUE_FIELD_NAME);
            serializeDeque(parameter.deque, generator);
            generator.writeEndObject();
        }

        private static void serializeDeque(final Deque<Context<?>> deque, final JsonGenerator generator) throws IOException {
            final ObjectMapper mapper = (ObjectMapper) generator.getCodec();
            generator.writeStartArray();
            for (final Context<?> context : deque) {
                generator.writeRawValue(mapper.writeValueAsString(context));
            }
            generator.writeEndArray();
        }
    }

    /**
     * JSON: Deserializer for PayloadParameter
     *
     * @see StdDeserializer
     * @see PayloadParameter
     */
    static class Deserializer extends StdDeserializer<DequeContextsParameter> {
        public Deserializer() {
            this(PayloadParameter.class);
        }

        protected Deserializer(Class<PayloadParameter> vc) {
            super(vc);
        }

        @Override
        public DequeContextsParameter deserialize(final JsonParser jsonParser,
                                                  final DeserializationContext deserializationContext) throws IOException {
            final TreeNode treeNode = jsonParser.readValueAsTree();
            try {
                final Class<?> parameterClass = restoreParameterClass(treeNode.get(TYPE_FIELD_NAME));
                if (!parameterClass.isAssignableFrom(DequeContextsParameter.class)) {
                    return null;
                }
                final Deque<Context<?>> contexts = restoreContextsDeque(treeNode.get(VALUE_FIELD_NAME), jsonParser);
                return new DequeContextsParameter(contexts);
            } catch (ClassNotFoundException e) {
                throw new IOException("Wrong parameter nested type", e);
            }
        }

        // private methods
        private Deque<Context<?>> restoreContextsDeque(final TreeNode dequeNode, JsonParser parser) throws IOException {
            if (dequeNode instanceof ArrayNode arrayNode) {
                final Deque<Context<?>> contexts = new LinkedList<>();
                final ObjectMapper mapper = (ObjectMapper) parser.getCodec();
                for(final JsonNode node : arrayNode) {
                    contexts.add(mapper.readValue(node.toString(), Context.class));
                }
                return contexts;
            }
            throw new IOException("Wrong type of deque node " + dequeNode.toString());
        }

        private static Class<?> restoreParameterClass(final TreeNode nestedClassNode) throws ClassNotFoundException {
            if (nestedClassNode instanceof TextNode node) {
                return Class.forName(node.asText()).asSubclass(DequeContextsParameter.class);
            } else {
                throw new ClassNotFoundException("Wrong nested type tree-node: " + nestedClassNode.getClass().getName());
            }
        }
    }

}
