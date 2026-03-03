package oleg.sopilnyak.test.service.command.io.parameter;

import static oleg.sopilnyak.test.service.command.io.IOFieldNames.TYPE_FIELD_NAME;
import static oleg.sopilnyak.test.service.command.io.IOFieldNames.VALUE_FIELD_NAME;

import oleg.sopilnyak.test.service.command.io.CompositeInput;
import oleg.sopilnyak.test.service.command.io.IOBase;
import oleg.sopilnyak.test.service.command.io.Input;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import org.springframework.util.ObjectUtils;
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
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

/**
 * Type: I/O school-command the composite array of inputs parameter
 *
 * @see Input
 */
@JsonSerialize(using = CompositeInputParameter.Serializer.class)
@JsonDeserialize(using = CompositeInputParameter.Deserializer.class)
public final class CompositeInputParameter implements CompositeInput {
    // the nest of gathered inputs
    private final Input<?>[] nest;

    public CompositeInputParameter(Input<?>... inputs) {
        if (ObjectUtils.isEmpty(inputs)) {
            this.nest = new Input[0];
        } else {
            this.nest = new Input[inputs.length];
            System.arraycopy(inputs, 0, this.nest, 0, inputs.length);
        }
    }

    /**
     * To get the value of command input-output entity
     *
     * @return value of the parameter
     */
    @Override
    public Input<?>[] value() {
        return nest;
    }

    /**
     * To check is result's output value is empty
     *
     * @return true if no data in the output result
     */
    @Override
    public boolean isEmpty() {
        return ObjectUtils.isEmpty(nest);
    }

    /**
     * JSON: Serializer for CompositeInputParameter
     *
     * @see StdSerializer
     * @see CompositeInputParameter
     */
    static class Serializer extends StdSerializer<CompositeInputParameter> {
        public Serializer() {
            this(null);
        }

        protected Serializer(Class<CompositeInputParameter> t) {
            super(t);
        }

        @Override
        public void serialize(final CompositeInputParameter parameter, final JsonGenerator generator,
                              final SerializerProvider serializerProvider) throws IOException {
            generator.writeStartObject();
            generator.writeStringField(TYPE_FIELD_NAME, CompositeInputParameter.class.getName());
            generator.writeFieldName(VALUE_FIELD_NAME);
            serializeInputsArray(parameter.nest, generator);
            generator.writeEndObject();
        }

        // private methods
        // store inputs array body as JSON
        private static void serializeInputsArray(final Input<?>[] inputs, final JsonGenerator generator) throws IOException {
            final ObjectMapper mapper = (ObjectMapper) generator.getCodec();
            generator.writeStartArray();
            for (final Input<?> input : inputs) {
                generator.writeRawValue(mapper.writeValueAsString(input));
            }
            generator.writeEndArray();
        }
    }

    /**
     * JSON: Deserializer for CompositeInputParameter
     *
     * @see StdDeserializer
     * @see CompositeInputParameter
     */
    static class Deserializer extends StdDeserializer<CompositeInputParameter> {
        public Deserializer() {
            this(CompositeInputParameter.class);
        }

        protected Deserializer(Class<CompositeInputParameter> vc) {
            super(vc);
        }

        @Override
        public CompositeInputParameter deserialize(
                final JsonParser jsonParser, final DeserializationContext notUsed
        ) throws IOException {
            final TreeNode treeNode = jsonParser.readValueAsTree();
            final Class<?> parameterClass = IOBase.restoreIoBaseClass(treeNode, CompositeInput.class);
            if (!parameterClass.isAssignableFrom(CompositeInputParameter.class)) {
                return null;
            }
            final Input<?>[] contexts = deserializeInputsArray(
                    treeNode.get(VALUE_FIELD_NAME), (ObjectMapper) jsonParser.getCodec()
            );
            return new CompositeInputParameter(contexts);
        }

        // private methods
        // restore inputs array from JSON
        private Input<?>[] deserializeInputsArray(final TreeNode valueNode, final ObjectMapper mapper) throws IOException {
            if (valueNode instanceof ArrayNode arrayNode) {
                final List<Input<?>> result = new LinkedList<>();
                for (final JsonNode node : arrayNode) {
                    result.add(deserializeInput(node, mapper));
                }
                return result.toArray(Input[]::new);
            } else {
                throw new IOException("Wrong type of inputs array node " + valueNode.toString());
            }
        }

        // restore input from JSON
        private Input<?> deserializeInput(final JsonNode parameterNode, final ObjectMapper mapper) throws IOException {
            final var inputParameterClass = IOBase.restoreIoBaseClass(parameterNode, Input.class);
            return mapper.readValue(parameterNode.toString(), inputParameterClass);
        }

    }
}
