package oleg.sopilnyak.test.service.command.io.parameter;

import static oleg.sopilnyak.test.service.command.io.IOFieldNames.NESTED_TYPE_FIELD_NAME;
import static oleg.sopilnyak.test.service.command.io.IOFieldNames.TYPE_FIELD_NAME;
import static oleg.sopilnyak.test.service.command.io.IOFieldNames.VALUE_FIELD_NAME;

import oleg.sopilnyak.test.service.command.io.Input;
import oleg.sopilnyak.test.service.message.payload.BasePayload;

import java.io.IOException;
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

/**
 * Type: I/O school-command Model Type input parameter
 *
 * @see Input
 * @see BasePayload
 */
@JsonSerialize(using = PayloadParameter.Serializer.class)
@JsonDeserialize(using = PayloadParameter.Deserializer.class)
public record PayloadParameter<T extends BasePayload<?>>(T value) implements Input<T> {

    /**
     * JSON: Serializer for PayloadParameter
     *
     * @see StdSerializer
     * @see PayloadParameter
     */
    static class Serializer<T extends BasePayload<?>> extends StdSerializer<PayloadParameter<T>> {
        public Serializer() {
            this(null);
        }

        protected Serializer(Class<PayloadParameter<T>> t) {
            super(t);
        }

        @Override
        public void serialize(final PayloadParameter<T> parameter,
                              final JsonGenerator generator,
                              final SerializerProvider serializerProvider) throws IOException {
            final ObjectMapper mapper = (ObjectMapper) generator.getCodec();
            generator.writeStartObject();
            generator.writeStringField(TYPE_FIELD_NAME, PayloadParameter.class.getName());
            generator.writeStringField(NESTED_TYPE_FIELD_NAME, parameter.value.getClass().getName());
            generator.writeFieldName(VALUE_FIELD_NAME);
            generator.writeRawValue(mapper.writeValueAsString(parameter.value));
            generator.writeEndObject();
        }
    }

    /**
     * JSON: Deserializer for PayloadParameter
     *
     * @see StdDeserializer
     * @see PayloadParameter
     */
    static class Deserializer<T extends BasePayload<?>> extends StdDeserializer<PayloadParameter<T>> {
        public Deserializer() {
            this(PayloadParameter.class);
        }

        protected Deserializer(Class<PayloadParameter> vc) {
            super(vc);
        }

        @Override
        public PayloadParameter<T> deserialize(final JsonParser jsonParser,
                                               final DeserializationContext deserializationContext) throws IOException {
            final TreeNode treeNode = jsonParser.readValueAsTree();
            try {
                final Class<?> nestedClass = restoreNestedClass(treeNode.get(NESTED_TYPE_FIELD_NAME));
                final ObjectMapper mapper = (ObjectMapper) jsonParser.getCodec();
                return new PayloadParameter<>(mapper.readValue(
                        treeNode.get(VALUE_FIELD_NAME).toString(),
                        mapper.getTypeFactory().constructType(nestedClass)
                ));
            } catch (ClassNotFoundException e) {
                throw new IOException("Wrong parameter nested type", e);
            }
        }

        // private methods
        private static Class<?> restoreNestedClass(final TreeNode nestedClassNode) throws ClassNotFoundException {
            if (nestedClassNode instanceof TextNode node) {
                return Class.forName(node.asText());
            } else {
                throw new ClassNotFoundException("Wrong nested type tree-node: " + nestedClassNode.getClass().getName());
            }
        }
    }

}
