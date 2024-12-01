package oleg.sopilnyak.test.service.command.io.parameter;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.LongNode;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import oleg.sopilnyak.test.service.command.io.Input;

import java.io.IOException;

import static java.util.Objects.isNull;

/**
 * Type: I/O school-command Long-id input parameter
 *
 * @see Input
 */
@JsonSerialize(using = LongIdParameter.Serializer.class)
@JsonDeserialize(using = LongIdParameter.Deserializer.class)
public record LongIdParameter(Long value) implements Input<Long> {
    /**
     * JSON: Serializer for LongIdParameter
     *
     * @see StdSerializer
     * @see LongIdParameter
     */
    static class Serializer extends StdSerializer<LongIdParameter> {
        public Serializer() {
            this(null);
        }

        protected Serializer(Class<LongIdParameter> t) {
            super(t);
        }

        @Override
        public void serialize(final LongIdParameter parameter,
                              final JsonGenerator generator,
                              final SerializerProvider serializerProvider) throws IOException {
            generator.writeStartObject();
            generator.writeStringField(Input.TYPE_FIELD_NAME, LongIdParameter.class.getName());
            generator.writeNumberField(Input.VALUE_FIELD_NAME, parameter.value());
            generator.writeEndObject();
        }
    }

    /**
     * JSON: Deserializer for LongIdParameter
     *
     * @see StdDeserializer
     * @see LongIdParameter
     */
    static class Deserializer extends StdDeserializer<LongIdParameter> {

        public Deserializer() {
            this(LongIdParameter.class);
        }

        protected Deserializer(Class<?> vc) {
            super(vc);
        }

        @Override
        public LongIdParameter deserialize(final JsonParser jsonParser, final DeserializationContext deserializationContext)
                throws IOException {
            final TreeNode valueNode = jsonParser.readValueAsTree().get(Input.VALUE_FIELD_NAME);
            return new LongIdParameter(isNull(valueNode) ? -1L : longValueOf(valueNode));
        }

        private long longValueOf(final TreeNode node) {
            return switch (node.numberType()) {
                case INT -> ((IntNode) node).intValue();
                case LONG -> ((LongNode) node).longValue();
                default -> -1L;
            };
        }
    }
}
