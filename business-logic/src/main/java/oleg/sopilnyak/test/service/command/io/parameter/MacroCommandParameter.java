package oleg.sopilnyak.test.service.command.io.parameter;

import static oleg.sopilnyak.test.service.command.io.IOFieldNames.TYPE_FIELD_NAME;
import static oleg.sopilnyak.test.service.command.io.IOFieldNames.VALUE_FIELD_NAME;

import oleg.sopilnyak.test.service.command.executable.core.MacroCommand;
import oleg.sopilnyak.test.service.command.io.Input;
import oleg.sopilnyak.test.service.command.type.core.Context;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Deque;
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicReference;
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
import lombok.ToString;
import lombok.Value;

/**
 * Type-wrapper: The wrapper of MacroCommand input parameter bean
 *
 * @see Input
 * @see Deque
 * @see MacroCommand#executeDo(Context)
 * @see Context
 * @see Context#getRedoParameter()
 */
@Value
@ToString
@JsonSerialize(using = MacroCommandParameter.Serializer.class)
@JsonDeserialize(using = MacroCommandParameter.Deserializer.class)
public class MacroCommandParameter implements Input<MacroCommandParameter> {
    private static final String MAIN_INPUT_FIELD_NAME = "main-input";
    private static final String NESTED_CONTEXTS_FIELD_NAME = "nested-contexts";
    private static final Input.ParameterDeserializer<?> parameterDeserializer = new Input.ParameterDeserializer<>();
    Input<?> rootInput;
    AtomicReference<Deque<Context<?>>> nestedContexts = new AtomicReference<>();

    public MacroCommandParameter(final Input<?> mainInputParameter, final Deque<Context<?>> nestedContexts) {
        this.rootInput = mainInputParameter;
        this.nestedContexts.getAndSet(nestedContexts);
    }

    public Deque<Context<?>> getNestedContexts() {
        return nestedContexts.get();
    }

    public void updateNestedContexts(final Deque<Context<?>> nestedContexts) {
        this.nestedContexts.getAndSet(nestedContexts);
    }

    /**
     * To get the value of command input-output entity
     *
     * @return value of the parameter
     */
    @Override
    public MacroCommandParameter value() {
        return this;
    }

    /**
     * JSON: Serializer for MacroCommandParameter
     *
     * @see StdSerializer
     */
    static class Serializer extends StdSerializer<MacroCommandParameter> {
        public Serializer() {
            this(null);
        }

        protected Serializer(Class<MacroCommandParameter> t) {
            super(t);
        }

        @Override
        public void serialize(final MacroCommandParameter parameter,
                              final JsonGenerator generator,
                              final SerializerProvider serializerProvider) throws IOException {
            generator.writeStartObject();
            generator.writeStringField(TYPE_FIELD_NAME, parameter.getClass().getName());
            generator.writeFieldName(VALUE_FIELD_NAME);
            // serialize the value start
            generator.writeStartObject();
            serializeRootInputParameter(parameter.getRootInput(), generator);
            serializeNestedContexts(parameter.getNestedContexts(), generator);
            generator.writeEndObject();
            // serialize the value end
            generator.writeEndObject();
        }

        // private methods
        private static void serializeRootInputParameter(final Input<?> mainInput,
                                                        final JsonGenerator generator) throws IOException {
            generator.writeFieldName(MAIN_INPUT_FIELD_NAME);
            generator.writeRawValue(((ObjectMapper) generator.getCodec()).writeValueAsString(mainInput));
        }

        private static void serializeNestedContexts(final Deque<Context<?>> nestedContexts,
                                                    final JsonGenerator generator) throws IOException {
            generator.writeFieldName(NESTED_CONTEXTS_FIELD_NAME);
            final ObjectMapper mapper = (ObjectMapper) generator.getCodec();
            generator.writeStartArray();
            for (final Context<?> context : nestedContexts) {
                generator.writeRawValue(mapper.writeValueAsString(context));
            }
            generator.writeEndArray();
        }
    }


    /**
     * JSON: Deserializer for MacroCommandParameter
     *
     * @see StdDeserializer
     */
    static class Deserializer extends StdDeserializer<MacroCommandParameter> {
        public Deserializer() {
            this(MacroCommandParameter.class);
        }

        protected Deserializer(Class<MacroCommandParameter> vc) {
            super(vc);
        }

        @Override
        public MacroCommandParameter deserialize(final JsonParser jsonParser,
                                                 final DeserializationContext deserializationContext) throws IOException {
            final TreeNode treeNode = jsonParser.readValueAsTree();
            final Class<? extends MacroCommandParameter> parameterClass = restoreParameterClass(treeNode.get(TYPE_FIELD_NAME));
            // restore the parameter value
            final TreeNode valueNode = treeNode.get(VALUE_FIELD_NAME);
            final ObjectMapper mapper = (ObjectMapper) jsonParser.getCodec();
            final Input<?> mainInput = deserializeRootInputParameter(valueNode.get(MAIN_INPUT_FIELD_NAME), mapper);
            final Deque<Context<?>> nestedContexts = deserializeNestedContexts(valueNode.get(NESTED_CONTEXTS_FIELD_NAME), mapper);
            try {
                return parameterClass.getConstructor(Input.class, Deque.class).newInstance(mainInput, nestedContexts);
            } catch (NoSuchMethodException | InstantiationException | IllegalAccessException |
                     InvocationTargetException e) {
                throw new IOException("Cannot construct macro-parameter", e);
            }
        }

        //private methods
        private static Class<? extends MacroCommandParameter> restoreParameterClass(final TreeNode parameterClassNode) throws IOException {
            try {
                if (parameterClassNode instanceof TextNode node) {
                    return Class.forName(node.asText()).asSubclass(MacroCommandParameter.class);
                } else {
                    throw new ClassNotFoundException("Wrong parameter type tree-node: " + parameterClassNode.getClass().getName());
                }
            } catch (ClassNotFoundException e) {
                throw new IOException(parameterClassNode.toString(), e);
            }
        }

        private static Input<?> deserializeRootInputParameter(final TreeNode treeNode,
                                                              final ObjectMapper mapper) throws IOException {
            if (treeNode == null) {
                return Input.empty();
            }
            // restore redo parameter
            final JsonParser parser = mapper.getFactory().createParser(treeNode.toString());
            return parameterDeserializer.deserialize(parser, null);
        }

        private static Deque<Context<?>> deserializeNestedContexts(final TreeNode nestedContextsNode,
                                                                   final ObjectMapper mapper) throws IOException {
            if (nestedContextsNode == null) {
                return new LinkedList<>();
            }
            // restore nested contexts from context-node array
            if (nestedContextsNode instanceof ArrayNode contextsArrayNode) {
                final Deque<Context<?>> contexts = new LinkedList<>();
                for (final JsonNode contextNode : contextsArrayNode) {
                    contexts.add(mapper.readValue(contextNode.toString(), Context.class));
                }
                return contexts;
            }
            throw new IOException("Wrong type of deque node " + nestedContextsNode);
        }
    }

}
