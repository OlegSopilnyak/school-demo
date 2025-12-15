package oleg.sopilnyak.test.service.command.io.result;

import static oleg.sopilnyak.test.service.command.io.IOFieldNames.NESTED_TYPE_FIELD_NAME;
import static oleg.sopilnyak.test.service.command.io.IOFieldNames.VALUE_FIELD_NAME;

import oleg.sopilnyak.test.school.common.model.BaseType;
import oleg.sopilnyak.test.service.command.io.IOFieldNames;
import oleg.sopilnyak.test.service.command.io.Output;
import oleg.sopilnyak.test.service.command.io.parameter.PayloadParameter;
import oleg.sopilnyak.test.service.message.payload.BasePayload;

import java.io.IOException;
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

/**
 * Type: I/O school-command Model type command execution result
 *
 * @param <P> type of the output result
 * @see BasePayload
 * @see BaseType
 * @see Output
 */
@JsonSerialize(using = PayloadResult.Serializer.class)
@JsonDeserialize(using = PayloadResult.Deserializer.class)
public record PayloadResult<P extends BasePayload<? extends BaseType>>(P value) implements Output<P> {
    /**
     * JSON: Serializer for PayloadParameter
     *
     * @see StdSerializer
     * @see PayloadParameter
     */
    static class Serializer<T extends BasePayload<? extends BaseType>> extends StdSerializer<PayloadResult<T>> {
        public Serializer() {
            this(null);
        }

        protected Serializer(Class<PayloadResult<T>> t) {
            super(t);
        }

        @Override
        public void serialize(
                final PayloadResult<T> parameter, final JsonGenerator generator, final SerializerProvider ignored
        ) throws IOException {
            final ObjectMapper mapper = (ObjectMapper) generator.getCodec();
            generator.writeStartObject();
            generator.writeStringField(IOFieldNames.TYPE_FIELD_NAME, PayloadResult.class.getName());
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
    static class Deserializer<T extends BasePayload<? extends BaseType>> extends StdDeserializer<PayloadResult<T>> {

        public Deserializer() {
            this(PayloadResult.class);
        }

        protected Deserializer(Class<?> vc) {
            super(vc);
        }

        @Override
        public PayloadResult<T> deserialize(
                final JsonParser jsonParser, final DeserializationContext ignored
        ) throws IOException {
            final TreeNode treeNode = jsonParser.readValueAsTree();
            try {
                final Class<?> nestedClass = restoreNestedClass(treeNode.get(NESTED_TYPE_FIELD_NAME));
                final ObjectMapper mapper = (ObjectMapper) jsonParser.getCodec();
                final String payloadJson = treeNode.get(VALUE_FIELD_NAME).toString();
                final JavaType payloadType = mapper.getTypeFactory().constructType(nestedClass);
                return new PayloadResult<>(mapper.readValue(payloadJson, payloadType));
            } catch (ClassNotFoundException e) {
                throw new IOException("Wrong parameter nested type", e);
            }
        }

        // private methods
        private Class<?> restoreNestedClass(final TreeNode node) throws ClassNotFoundException {
            if (node instanceof TextNode textNode) {
                return Class.forName(textNode.asText());
            } else {
                throw new ClassNotFoundException("Wrong nested type tree-node: " + node.getClass().getName());
            }
        }
    }
}
