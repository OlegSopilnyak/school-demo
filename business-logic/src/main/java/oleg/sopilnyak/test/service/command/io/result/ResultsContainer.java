package oleg.sopilnyak.test.service.command.io.result;

import static oleg.sopilnyak.test.service.command.io.IOFieldNames.TYPE_FIELD_NAME;
import static oleg.sopilnyak.test.service.command.io.IOFieldNames.VALUE_FIELD_NAME;

import oleg.sopilnyak.test.service.command.io.CompositeOutput;
import oleg.sopilnyak.test.service.command.io.IOBase;
import oleg.sopilnyak.test.service.command.io.Output;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Collection;
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
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

/**
 * Container: I/O school-command array of output results as one parameter (for multiple outputs gathered)
 *
 * @see CompositeOutput
 */
public abstract class ResultsContainer<T> {
    // empty output results (for deserialization purposes)
    public static final ResultsContainer<?> EMPTY = new ResultsContainer<>(){};
    // the nest of joined output results
    protected final Output<T>[] nest;

    @SuppressWarnings("unchecked")
    protected ResultsContainer(Output<?>... outputs) {
        if (ObjectUtils.isEmpty(outputs)) {
            this.nest = new Output[0];
        } else {
            this.nest = new Output[outputs.length];
            System.arraycopy(outputs, 0, this.nest, 0, outputs.length);
        }
    }

    @SuppressWarnings("unchecked")
    protected ResultsContainer(Collection<T> outputs) {
        this.nest = outputs.stream().map(Output::of).filter(o -> !o.isEmpty()).toArray(Output[]::new);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof ResultsContainer<?> that && Arrays.equals(nest, that.nest);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(nest);
    }

    /**
     * JSON: Serializer for ResultsContainer
     *
     * @see StdSerializer
     * @see ResultsContainer
     */
    static class Serializer extends StdSerializer<ResultsContainer<?>> {
        public Serializer() {
            this(null);
        }

        protected Serializer(Class<ResultsContainer<?>> t) {
            super(t);
        }

        @SuppressWarnings("unchecked")
        @Override
        public void serialize(final ResultsContainer parameter, final JsonGenerator generator,
                              final SerializerProvider notUsed) throws IOException {
            checkBeforeSerialize(parameter.nest);
            generator.writeStartObject();
            generator.writeStringField(TYPE_FIELD_NAME, parameter.getClass().getName());
            generator.writeFieldName(VALUE_FIELD_NAME);
            serializeOutputsArray(parameter.nest, generator);
            generator.writeEndObject();
        }

        /**
         * To check the nest before serialization
         *
         * @param results array of results (the nest) to check before
         * @throws IOException if the nest cannot be serialized
         */
        protected <T> void checkBeforeSerialize(final Output<T>[] results) throws IOException {
            // Usually does nothing but for Set of Payloads type checks mismatch types issue
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
     * JSON: Deserializer for ResultsContainer
     *
     * @see StdDeserializer
     * @see ResultsContainer
     */
    static class Deserializer<T> extends StdDeserializer<ResultsContainer<T>> {
        public Deserializer() {
            this(ResultsContainer.class);
        }

        protected Deserializer(Class<ResultsContainer> vc) {
            super(vc);
        }

        @Override
        @SuppressWarnings({"unchecked", "rawtypes"})
        public ResultsContainer<T> deserialize(final JsonParser jsonParser, final DeserializationContext notUsed)
                throws IOException {
            final TreeNode treeNode = jsonParser.readValueAsTree();
            final Class<Output> typeClass = IOBase.restoreIoBaseClass(treeNode, Output.class);
            final List<T> results = Arrays.stream(
                    deserializeOutputsArray(treeNode.get(VALUE_FIELD_NAME), (ObjectMapper) jsonParser.getCodec())
            ).map(IOBase::value).toList();
            try {
                return (ResultsContainer<T>) typeClass.getConstructor(Collection.class).newInstance(results);
            } catch (NoSuchMethodException e) {
                throw new IOException("No suitable constructor found for " + typeClass.getName(), e);
            } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
                throw new IOException("Cannot build instance for " + typeClass.getName(), e);
            }
        }

        // private methods
        // restore inputs array from JSON
        @SuppressWarnings("unchecked")
        private Output<T>[] deserializeOutputsArray(final TreeNode valueNode, final ObjectMapper mapper) throws IOException {
            if (valueNode instanceof ArrayNode arrayNode) {
                final List<Output<?>> result = new LinkedList<>();
                for (final JsonNode node : arrayNode) {
                    result.add(deserializeResult(node, mapper));
                }
                return result.toArray(Output[]::new);
            } else {
                throw new IOException("Wrong type of outputs array node " + valueNode.toString());
            }
        }

        // restore nested result from JSON
        @SuppressWarnings("unchecked")
        private Output<T> deserializeResult(final JsonNode parameterNode, final ObjectMapper mapper) throws IOException {
            final var resultParameterClass = IOBase.restoreIoBaseClass(parameterNode, Output.class);
            return mapper.readValue(parameterNode.toString(), resultParameterClass);
        }

    }
}
