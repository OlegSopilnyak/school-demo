package oleg.sopilnyak.test.service.command.io.parameter;

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
import oleg.sopilnyak.test.service.command.io.Input;
import oleg.sopilnyak.test.service.message.payload.BasePayload;

import java.io.IOException;
import java.util.Objects;

/**
 * Type: I/O school-command the pair of BaseType input parameter
 *
 * @see PairParameter
 * @see BasePayload
 */
@JsonSerialize(using = PayloadPairParameter.Serializer.class)
@JsonDeserialize(using = PayloadPairParameter.Deserializer.class)
public record PayloadPairParameter<T extends BasePayload<?>>(T first, T second) implements PairParameter<T> {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * JSON: Serializer for PayloadPairParameter
     *
     * @see StdSerializer
     * @see PayloadPairParameter
     */
    static class Serializer extends StdSerializer<PayloadPairParameter<?>> {
        public Serializer() {
            this(null);
        }

        protected Serializer(Class<PayloadPairParameter<?>> t) {
            super(t);
        }

        @Override
        public void serialize(final PayloadPairParameter<?> parameter,
                              final JsonGenerator generator,
                              final SerializerProvider serializerProvider) throws IOException {
            final Class<?> firstParameterClass = parameter.first.getClass();
            final Class<?> secondParameterClass = parameter.second.getClass();
            if (!Objects.equals(firstParameterClass, secondParameterClass)) {
                throw new IOException("Pair parameter class mismatch");
            }
            generator.writeStartObject();
            generator.writeStringField(Input.TYPE_FIELD_NAME, PayloadPairParameter.class.getName());
            generator.writeStringField(Input.NESTED_TYPE_FIELD_NAME, firstParameterClass.getName());
            generator.writeFieldName(VALUE_FIELD_NAME);
            generator.writeStartObject();
            generator.writeFieldName(FIRST_FIELD_NAME);
            generator.writeRawValue(MAPPER.writeValueAsString(parameter.value().first()));
            generator.writeFieldName(SECOND_FIELD_NAME);
            generator.writeRawValue(MAPPER.writeValueAsString(parameter.value().second()));
            generator.writeEndObject();
            generator.writeEndObject();
        }
    }

    /**
     * JSON: Deserializer for PayloadPairParameter
     *
     * @see StdDeserializer
     * @see PayloadPairParameter
     */
    static class Deserializer extends StdDeserializer<PayloadPairParameter<?>> {

        public Deserializer() {
            this(PayloadPairParameter.class);
        }

        protected Deserializer(Class<PayloadPairParameter> vc) {
            super(vc);
        }

        @Override
        public PayloadPairParameter<?> deserialize(final JsonParser jsonParser, final DeserializationContext deserializationContext)
                throws IOException {
            final TreeNode treeNode = jsonParser.readValueAsTree();
            try {
                final Class<?> nestedClass = nestedClass(treeNode.get(Input.NESTED_TYPE_FIELD_NAME));
                final JavaType valueJavaType = MAPPER.getTypeFactory().constructType(nestedClass);
                final TreeNode valueNode = treeNode.get(Input.VALUE_FIELD_NAME);
                return new PayloadPairParameter<>(
                        MAPPER.readValue(valueNode.get(FIRST_FIELD_NAME).toString(), valueJavaType),
                        MAPPER.readValue(valueNode.get(SECOND_FIELD_NAME).toString(), valueJavaType)
                );
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
