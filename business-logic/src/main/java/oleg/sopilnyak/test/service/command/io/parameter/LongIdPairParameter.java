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

import java.io.IOException;

import static oleg.sopilnyak.test.service.command.io.IOFieldNames.*;

/**
 * Type: I/O school-command the pair of Long-id input parameter
 *
 * @see PairParameter
 * @see Long
 */
@JsonSerialize(using = LongIdPairParameter.Serializer.class)
@JsonDeserialize(using = LongIdPairParameter.Deserializer.class)
public record LongIdPairParameter(Long first, Long second) implements PairParameter<Long> {
    /**
     * JSON: Serializer for LongIdParameter
     *
     * @see StdSerializer
     * @see NumberIdParameter
     */
    static class Serializer extends StdSerializer<LongIdPairParameter> {
        public Serializer() {
            this(null);
        }

        protected Serializer(Class<LongIdPairParameter> t) {
            super(t);
        }

        @Override
        public void serialize(final LongIdPairParameter parameter,
                              final JsonGenerator generator,
                              final SerializerProvider serializerProvider) throws IOException {
            generator.writeStartObject();
            generator.writeStringField(TYPE_FIELD_NAME, LongIdPairParameter.class.getName());
            generator.writeStringField(NESTED_TYPE_FIELD_NAME, Long.class.getName());
            generator.writeFieldName(VALUE_FIELD_NAME);
            generator.writeStartObject();
            generator.writeNumberField(FIRST_FIELD_NAME, parameter.value().first());
            generator.writeNumberField(SECOND_FIELD_NAME, parameter.value().second());
            generator.writeEndObject();
            generator.writeEndObject();
        }
    }

    /**
     * JSON: Deserializer for LongIdParameter
     *
     * @see StdDeserializer
     * @see NumberIdParameter
     */
    static class Deserializer extends StdDeserializer<LongIdPairParameter> {

        public Deserializer() {
            this(NumberIdParameter.class);
        }

        protected Deserializer(Class<?> vc) {
            super(vc);
        }

        @Override
        public LongIdPairParameter deserialize(final JsonParser jsonParser, final DeserializationContext deserializationContext)
                throws IOException {
            final TreeNode valueNode = jsonParser.readValueAsTree().get(VALUE_FIELD_NAME);
            final long firstId = longValueOf(valueNode.get(FIRST_FIELD_NAME));
            final long secondId = longValueOf(valueNode.get(SECOND_FIELD_NAME));
            return new LongIdPairParameter(firstId, secondId);
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
