package oleg.sopilnyak.test.service.command.io.result;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.node.TextNode;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import oleg.sopilnyak.test.school.common.model.BaseType;
import oleg.sopilnyak.test.service.command.io.IOFieldNames;
import oleg.sopilnyak.test.service.command.io.Output;
import oleg.sopilnyak.test.service.command.io.parameter.PayloadParameter;
import oleg.sopilnyak.test.service.message.payload.BasePayload;

import java.io.IOException;

import static oleg.sopilnyak.test.service.command.io.IOFieldNames.VALUE_FIELD_NAME;

/**
 * Type: I/O school-command Model type command execution result
 *
 * @param <T> type of the output result
 * @see BasePayload
 * @see BaseType
 * @see Output
 */
@JsonSerialize(using = PayloadResult.Serializer.class)
@JsonDeserialize(using = PayloadResult.Deserializer.class)
public record PayloadResult<T extends BasePayload<? extends BaseType>>(T value) implements Output<T> {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    /**
     * JSON: Serializer for PayloadParameter
     *
     * @see StdSerializer
     * @see PayloadParameter
     */
    static class Serializer extends StdSerializer<PayloadResult<?>> {
        public Serializer() {
            this(null);
        }

        protected Serializer(Class<PayloadResult<?>> t) {
            super(t);
        }

        @Override
        public void serialize(final PayloadResult<?> parameter,
                              final JsonGenerator generator,
                              final SerializerProvider serializerProvider) throws IOException {
            generator.writeStartObject();
            generator.writeStringField(IOFieldNames.TYPE_FIELD_NAME, PayloadParameter.class.getName());
            generator.writeStringField(IOFieldNames.NESTED_TYPE_FIELD_NAME, parameter.value.getClass().getName());
            generator.writeFieldName(VALUE_FIELD_NAME);
            generator.writeRawValue(MAPPER.writeValueAsString(parameter.value));
            generator.writeEndObject();
        }
    }

    /**
     * JSON: Deserializer for PayloadParameter
     *
     * @see StdDeserializer
     * @see PayloadParameter
     */
    static class Deserializer extends StdDeserializer<PayloadResult<?>> {

        public Deserializer() {
            this(PayloadResult.class);
        }

        protected Deserializer(Class<PayloadResult> vc) {
            super(vc);
        }

        @Override
        public PayloadResult<?> deserialize(final JsonParser jsonParser, final DeserializationContext deserializationContext)
                throws IOException {
            final TreeNode treeNode = jsonParser.readValueAsTree();
            try {
                final Class<?> nestedClass = nestedClass(treeNode.get(IOFieldNames.NESTED_TYPE_FIELD_NAME));
                final JavaType valueJavaType = MAPPER.getTypeFactory().constructType(nestedClass);
                final TreeNode valueNode = treeNode.get(VALUE_FIELD_NAME);
                return new PayloadResult<>(MAPPER.readValue(valueNode.toString(), valueJavaType));
            } catch (ClassNotFoundException e) {
                throw new IOException("Wrong parameter nested type", e);
            }
        }

        // private methods
        private Class<?> nestedClass(final TreeNode node) throws ClassNotFoundException {
            if (node instanceof TextNode textNode) {
                return Class.forName(textNode.asText());
            } else {
                throw new ClassNotFoundException("Wrong nested type tree-node: " + node.getClass().getName());
            }
        }
    }
}
