package oleg.sopilnyak.test.service.command.io.parameter;

import static oleg.sopilnyak.test.service.command.io.IOFieldNames.TYPE_FIELD_NAME;
import static oleg.sopilnyak.test.service.command.io.IOFieldNames.VALUE_FIELD_NAME;

import oleg.sopilnyak.test.service.command.io.CompositeInput;
import oleg.sopilnyak.test.service.command.io.IOBase;
import oleg.sopilnyak.test.service.command.io.Input;

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
 * Container: I/O school-command array of input parameters as one parameter (for multiple inputs gathered)
 *
 * @see CompositeInput
 */
public abstract class ParametersContainer<T> {
    // empty input parameters container (for deserialization purposes)
    public static final ParametersContainer<?> EMPTY = new ParametersContainer<>(){};
    // the nest of joined input parameters
    protected final Input<T>[] nest;

    @SuppressWarnings("unchecked")
    protected ParametersContainer(Input<?>... inputs) {
        if (ObjectUtils.isEmpty(inputs)) {
            this.nest = new Input[0];
        } else {
            this.nest = new Input[inputs.length];
            System.arraycopy(inputs, 0, this.nest, 0, inputs.length);
        }
    }

    @SuppressWarnings("unchecked")
    protected ParametersContainer(Collection<T> inputs) {
        this.nest = inputs.stream().map(Input::of).filter(o -> !o.isEmpty()).toArray(Input[]::new);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof ParametersContainer<?> that && Arrays.equals(nest, that.nest);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(nest);
    }

    /**
     * JSON: Serializer for ParametersContainer
     *
     * @see StdSerializer
     * @see ParametersContainer
     */
    static class Serializer extends StdSerializer<ParametersContainer<?>> {
        public Serializer() {
            this(null);
        }

        protected Serializer(Class<ParametersContainer<?>> t) {
            super(t);
        }

        @SuppressWarnings("unchecked")
        @Override
        public void serialize(final ParametersContainer parameter, final JsonGenerator generator,
                              final SerializerProvider notUsed) throws IOException {
            checkBeforeSerialize(parameter.nest);
            generator.writeStartObject();
            generator.writeStringField(TYPE_FIELD_NAME, parameter.getClass().getName());
            generator.writeFieldName(VALUE_FIELD_NAME);
            serializeInputsArray(parameter.nest, generator);
            generator.writeEndObject();
        }

        /**
         * To check the nest before serialization
         *
         * @param parameters array of parameters (the nest) to check before
         * @throws IOException if the nest cannot be serialized
         */
        protected <T> void checkBeforeSerialize(final Input<T>[] parameters) throws IOException {
            // Usually does nothing but for Set of Payloads type checks mismatch types issue
        }

        // private methods
        // store inputs array body as JSON
        private void serializeInputsArray(final Input<?>[] inputs, final JsonGenerator generator) throws IOException {
            generator.writeStartArray();
            final ObjectMapper mapper = (ObjectMapper) generator.getCodec();
            for (final Input<?> input : inputs) {
                generator.writeRawValue(mapper.writeValueAsString(input));
            }
            generator.writeEndArray();
        }
    }

    /**
     * JSON: Deserializer for ResultsContainer
     *
     * @see StdDeserializer
     * @see ParametersContainer
     */
    static class Deserializer<T> extends StdDeserializer<ParametersContainer<T>> {
        public Deserializer() {
            this(ParametersContainer.class);
        }

        protected Deserializer(Class<ParametersContainer> vc) {
            super(vc);
        }

        @Override
        @SuppressWarnings({"unchecked", "rawtypes"})
        public ParametersContainer<T> deserialize(final JsonParser jsonParser, final DeserializationContext notUsed)
                throws IOException {
            final TreeNode treeNode = jsonParser.readValueAsTree();
            final Class<Input> typeClass = IOBase.restoreIoBaseClass(treeNode, Input.class);
            final List<T> parameters = Arrays.stream(
                    deserializeInputsArray(treeNode.get(VALUE_FIELD_NAME), (ObjectMapper) jsonParser.getCodec())
            ).map(IOBase::value).toList();
            try {
                return (ParametersContainer<T>) typeClass.getConstructor(Collection.class).newInstance(parameters);
            } catch (NoSuchMethodException e) {
                throw new IOException("No suitable constructor found for " + typeClass.getName(), e);
            } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
                throw new IOException("Cannot build instance for " + typeClass.getName(), e);
            }
        }

        // private methods
        // restore inputs array from JSON
        @SuppressWarnings("unchecked")
        private Input<T>[] deserializeInputsArray(final TreeNode valueNode, final ObjectMapper mapper) throws IOException {
            if (valueNode instanceof ArrayNode arrayNode) {
                final List<Input<?>> parameter = new LinkedList<>();
                for (final JsonNode node : arrayNode) {
                    parameter.add(deserializeParameter(node, mapper));
                }
                return parameter.toArray(Input[]::new);
            } else {
                throw new IOException("Wrong type of inputs array node " + valueNode.toString());
            }
        }

        // restore nested parameter from JSON
        @SuppressWarnings("unchecked")
        private Input<T> deserializeParameter(final JsonNode parameterNode, final ObjectMapper mapper) throws IOException {
            final var parameterClass = IOBase.restoreIoBaseClass(parameterNode, Input.class);
            return mapper.readValue(parameterNode.toString(), parameterClass);
        }

    }
}
