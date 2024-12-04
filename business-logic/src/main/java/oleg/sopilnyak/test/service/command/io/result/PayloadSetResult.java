package oleg.sopilnyak.test.service.command.io.result;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import oleg.sopilnyak.test.school.common.model.BaseType;
import oleg.sopilnyak.test.service.command.io.IOFieldNames;
import oleg.sopilnyak.test.service.command.io.Output;
import oleg.sopilnyak.test.service.message.payload.BasePayload;

import java.io.IOException;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Type: I/O school-command set of Model-types command execution result
 *
 * @param <P> type of the result's set elements
 * @see Set
 * @see BasePayload
 * @see BaseType
 * @see Output
 */
@JsonSerialize(using = PayloadSetResult.Serializer.class)
@JsonDeserialize(using = PayloadSetResult.Deserializer.class)
public record PayloadSetResult<P extends BasePayload<? extends BaseType>>(Set<P> value) implements Output<Set<P>> {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * JSON: Serializer for PayloadSetResult
     *
     * @see StdSerializer
     * @see PayloadSetResult
     */
    static class Serializer extends StdSerializer<PayloadSetResult<?>> {
        public Serializer() {
            this(null);
        }

        protected Serializer(Class<PayloadSetResult<?>> t) {
            super(t);
        }

        @Override
        public void serialize(final PayloadSetResult<?> parameter,
                              final JsonGenerator generator,
                              final SerializerProvider serializerProvider) throws IOException {
            final Class<?> firstParameterClass = parameter.value.iterator().next().getClass();
            for (final BasePayload<? extends BaseType> basePayload : parameter.value) {
                if (!Objects.equals(firstParameterClass, basePayload.getClass())) {
                    throw new IOException("Payload Set parameter class mismatch");
                }
            }
            generator.writeStartObject();
            generator.writeStringField(IOFieldNames.TYPE_FIELD_NAME, PayloadSetResult.class.getName());
            generator.writeStringField(IOFieldNames.NESTED_TYPE_FIELD_NAME, firstParameterClass.getName());
            generator.writeFieldName(VALUE_FIELD_NAME);
            generator.writeRawValue(MAPPER.writeValueAsString(parameter.value));
            generator.writeEndObject();
        }
    }

    /**
     * JSON: Deserializer for PayloadSetResult
     *
     * @see StdDeserializer
     * @see PayloadSetResult
     */
    static class Deserializer extends StdDeserializer<PayloadSetResult<?>> {

        public static final String WRONG_NESTED_TYPE_TREE_NODE = "Wrong nested type tree-node: ";

        public Deserializer() {
            this(PayloadSetResult.class);
        }

        protected Deserializer(Class<PayloadSetResult> vc) {
            super(vc);
        }

        @Override
        public PayloadSetResult<?> deserialize(final JsonParser jsonParser, final DeserializationContext deserializationContext)
                throws IOException {
            final TreeNode treeNode = jsonParser.readValueAsTree();
            try {
                final Class<?> nestedClass = nestedClass(treeNode.get(IOFieldNames.NESTED_TYPE_FIELD_NAME));
                final JavaType valueJavaType = MAPPER.getTypeFactory().constructType(nestedClass);
                final TreeNode valueNode = treeNode.get(IOFieldNames.VALUE_FIELD_NAME);
                return new PayloadSetResult<>(deserializeNodesArray(valueNode, valueJavaType));
            } catch (ClassNotFoundException e) {
                throw new IOException("Wrong parameter nested type", e);
            }
        }

        // private methods
        private static Set<? extends BasePayload<? extends BaseType>> deserializeNodesArray(final TreeNode node,
                                                                                            final JavaType type
        ) throws IOException {
            if (node instanceof ArrayNode arrayNode) {
                final Set<? extends BasePayload<? extends BaseType>> result = new HashSet<>();
                for (final JsonNode valueNode : arrayNode) {
                    if (valueNode instanceof ObjectNode objectNode) {
                        result.add(MAPPER.readValue(objectNode.toString(), type));
                    } else {
                        throw new IOException(WRONG_NESTED_TYPE_TREE_NODE + valueNode.getClass().getName());
                    }
                }
                return result;
            }
            throw new IOException(WRONG_NESTED_TYPE_TREE_NODE + node.getClass().getName());
        }

        private static Class<?> nestedClass(final TreeNode node) throws ClassNotFoundException {
            if (node instanceof TextNode textNode) {
                return Class.forName(textNode.asText());
            } else {
                throw new ClassNotFoundException(WRONG_NESTED_TYPE_TREE_NODE + node.getClass().getName());
            }
        }
    }
}
