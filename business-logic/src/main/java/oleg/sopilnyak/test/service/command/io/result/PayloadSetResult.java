package oleg.sopilnyak.test.service.command.io.result;

import static oleg.sopilnyak.test.service.command.io.IOFieldNames.NESTED_TYPE_FIELD_NAME;
import static oleg.sopilnyak.test.service.command.io.IOFieldNames.VALUE_FIELD_NAME;

import oleg.sopilnyak.test.school.common.model.BaseType;
import oleg.sopilnyak.test.service.command.io.IOFieldNames;
import oleg.sopilnyak.test.service.command.io.Output;
import oleg.sopilnyak.test.service.message.payload.BasePayload;

import java.io.IOException;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import org.springframework.util.CollectionUtils;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

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

    /**
     * JSON: Serializer for PayloadSetResult
     *
     * @see StdSerializer
     * @see PayloadSetResult
     */
    static class Serializer<T extends BasePayload<? extends BaseType>> extends StdSerializer<PayloadSetResult<T>> {
        public Serializer() {
            this(null);
        }

        protected Serializer(Class<PayloadSetResult<T>> t) {
            super(t);
        }

        @Override
        public void serialize(
                final PayloadSetResult<T> result, final JsonGenerator generator, final SerializerProvider ignored
        ) throws IOException {
            // getting the set to serialize
            final Set<T> resultSet = result.value;
            // checking the types of the set
            final String elementInSetTypeName = checkElementTypes(resultSet).getName();
            // checking the types of the set
            final ObjectMapper mapper = (ObjectMapper) generator.getCodec();
            generator.writeStartObject();
            generator.writeStringField(IOFieldNames.TYPE_FIELD_NAME, PayloadSetResult.class.getName());
            generator.writeStringField(NESTED_TYPE_FIELD_NAME, elementInSetTypeName);
            generator.writeFieldName(VALUE_FIELD_NAME);
            generator.writeRawValue(mapper.writeValueAsString(resultSet));
            generator.writeEndObject();
        }

        // private methods
        private Class<?> checkElementTypes(final Set<T> set) throws IOException {
            if (CollectionUtils.isEmpty(set)) {
                // set is empty, type of elements in collection doesn't matter
                return BasePayload.class;
            }
            // checking the types of the elements in set
            final Class<?> firstElementInSetType = set.iterator().next().getClass();
            for (final T elementInSet : set) {
                if (!Objects.equals(firstElementInSetType, elementInSet.getClass())) {
                    throw new IOException("Payload Set parameter elements types mismatch");
                }
            }
            return firstElementInSetType;
        }
    }

    /**
     * JSON: Deserializer for PayloadSetResult
     *
     * @see StdDeserializer
     * @see PayloadSetResult
     */
    static class Deserializer<T extends BasePayload<? extends BaseType>> extends StdDeserializer<PayloadSetResult<T>> {

        public static final String WRONG_NESTED_TYPE_TREE_NODE = "Wrong nested type tree-node: ";

        public Deserializer() {
            this(PayloadSetResult.class);
        }

        protected Deserializer(Class<?> vc) {
            super(vc);
        }

        @Override
        public PayloadSetResult<T> deserialize(
                final JsonParser jsonParser, final DeserializationContext ignored
        ) throws IOException {
            final TreeNode rootNode = jsonParser.readValueAsTree();
            try {
                final TreeNode node = rootNode.get(NESTED_TYPE_FIELD_NAME);
                final Class<?> elementInSetType;
                if (node instanceof TextNode textNode) {
                    elementInSetType = Class.forName(textNode.asText());
                } else {
                    throw new ClassNotFoundException(WRONG_NESTED_TYPE_TREE_NODE + node.getClass().getName());
                }
                final TreeNode valueTreeNode = rootNode.get(VALUE_FIELD_NAME);
                final ObjectMapper mapper  = (ObjectMapper) jsonParser.getCodec();
                final Set<T> restoredSet = restoreSetFromNodesArray(elementInSetType, valueTreeNode, mapper);
                return new PayloadSetResult<>(restoredSet);
            } catch (ClassNotFoundException e) {
                throw new IOException("Wrong parameter nested type", e);
            }
        }

        // private methods
        private Set<T> restoreSetFromNodesArray(
                final Class<?> elementInSetType, final TreeNode valueTreeNode, final ObjectMapper mapper
        ) throws IOException {
            if (valueTreeNode instanceof ArrayNode arrayNode) {
                if (arrayNode.isEmpty()) {
                    // no elements serialized
                    return new HashSet<>();
                }
                // restore elements to HashSet
                final Set<T> payloadsSet = new HashSet<>();
                final JavaType payloadType = mapper.getTypeFactory().constructType(elementInSetType);
                for (final JsonNode valueNode : arrayNode) {
                    if (valueNode instanceof ObjectNode payloadNode) {
                        payloadsSet.add(mapper.readValue(payloadNode.toString(), payloadType));
                    } else {
                        throw new IOException(WRONG_NESTED_TYPE_TREE_NODE + valueNode.getClass().getName());
                    }
                }
                return payloadsSet;
            } else {
                throw new IOException(WRONG_NESTED_TYPE_TREE_NODE + valueTreeNode.getClass().getName());
            }
        }
    }
}
