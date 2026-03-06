package oleg.sopilnyak.test.service.command.io.result;

import static oleg.sopilnyak.test.service.command.io.IOFieldNames.TYPE_FIELD_NAME;
import static oleg.sopilnyak.test.service.command.io.IOFieldNames.VALUE_FIELD_NAME;

import oleg.sopilnyak.test.service.command.io.CompositeOutput;
import oleg.sopilnyak.test.service.command.io.IOBase;
import oleg.sopilnyak.test.service.command.io.Output;

import java.io.IOException;
import java.util.Arrays;
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
 * @see Output
 */
@JsonSerialize(using = CompositeOutputParameter.Serializer.class)
@JsonDeserialize(using = CompositeOutputParameter.Deserializer.class)
public final class CompositeOutputParameter<T> implements CompositeOutput<T> {
    // emptyValue composite output parameter
    public static final CompositeOutputParameter EMPTY = new CompositeOutputParameter<>();
    // the nest of gathered outputs
    private final Output<T>[] nest;

    @Override
    public boolean equals(Object o) {
        return o instanceof CompositeOutputParameter<?> that && Arrays.equals(nest, that.nest);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(nest);
    }

    @SuppressWarnings("unchecked")
    public CompositeOutputParameter(Output<?>... outputs) {
        if (ObjectUtils.isEmpty(outputs)) {
            this.nest = new Output[0];
        } else {
            this.nest = new Output[outputs.length];
            System.arraycopy(outputs, 0, this.nest, 0, outputs.length);
        }
    }

    /**
     * To get the value of command input-output entity
     *
     * @return value of the parameter
     */
    @Override
    public Output<T>[] value() {
        return nest;
    }

    /**
     * To check is result's output value is emptyValue
     *
     * @return true if no data in the output result
     */
    @Override
    public boolean isEmpty() {
        return ObjectUtils.isEmpty(nest);
    }

    /**
     * JSON: Serializer for CompositeOutputParameter
     *
     * @see StdSerializer
     * @see CompositeOutputParameter
     */
    static class Serializer extends StdSerializer<CompositeOutputParameter<?>> {
        public Serializer() {
            this(null);
        }

        protected Serializer(Class<CompositeOutputParameter<?>> t) {
            super(t);
        }

        @Override
        public void serialize(final CompositeOutputParameter parameter, final JsonGenerator generator,
                              final SerializerProvider notUsed) throws IOException {
            generator.writeStartObject();
            generator.writeStringField(TYPE_FIELD_NAME, CompositeOutputParameter.class.getName());
            generator.writeFieldName(VALUE_FIELD_NAME);
            serializeOutputsArray(parameter.nest, generator);
            generator.writeEndObject();
        }

        // private methods
        // store inputs array body as JSON
        private void serializeOutputsArray(final Output<?>[] outputs, final JsonGenerator generator) throws IOException {
            generator.writeStartArray();
            final ObjectMapper mapper = (ObjectMapper) generator.getCodec();
            for (final Output<?> output : outputs) {
                generator.writeRawValue(mapper.writeValueAsString(output));
            }
            generator.writeEndArray();
        }
    }

    /**
     * JSON: Deserializer for CompositeOutputParameter
     *
     * @see StdDeserializer
     * @see CompositeOutputParameter
     */
    static class Deserializer<T> extends StdDeserializer<CompositeOutputParameter<T>> {
        public Deserializer() {
            this(CompositeOutputParameter.class);
        }

        protected Deserializer(Class<CompositeOutputParameter> vc) {
            super(vc);
        }

        @Override
        @SuppressWarnings("unchecked")
        public CompositeOutputParameter<T> deserialize(
                final JsonParser jsonParser, final DeserializationContext notUsed
        ) throws IOException {
            final TreeNode treeNode = jsonParser.readValueAsTree();
            final Class<?> parameterClass = IOBase.restoreIoBaseClass(treeNode, CompositeOutput.class);
            if (!parameterClass.isAssignableFrom(CompositeOutputParameter.class)) {
                return EMPTY;
            }
            final Output<T>[] outputs = deserializeOutputsArray(
                    treeNode.get(VALUE_FIELD_NAME), (ObjectMapper) jsonParser.getCodec()
            );
            return new CompositeOutputParameter<>(outputs);
        }

        // private methods
        // restore inputs array from JSON
        @SuppressWarnings("unchecked")
        private Output<T>[] deserializeOutputsArray(final TreeNode valueNode, final ObjectMapper mapper) throws IOException {
            if (valueNode instanceof ArrayNode arrayNode) {
                final List<Output<?>> result = new LinkedList<>();
                for (final JsonNode node : arrayNode) {
                    result.add(deserializeOutput(node, mapper));
                }
                return result.toArray(Output[]::new);
            } else {
                throw new IOException("Wrong type of outputs array node " + valueNode.toString());
            }
        }

        // restore input from JSON
        @SuppressWarnings("unchecked")
        private Output<T> deserializeOutput(final JsonNode parameterNode, final ObjectMapper mapper) throws IOException {
            final var inputParameterClass = IOBase.restoreIoBaseClass(parameterNode, Output.class);
            return mapper.readValue(parameterNode.toString(), inputParameterClass);
        }

    }
}
