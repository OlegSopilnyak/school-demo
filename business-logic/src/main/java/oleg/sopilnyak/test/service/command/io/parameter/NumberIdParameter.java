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
import static oleg.sopilnyak.test.service.command.io.IOFieldNames.TYPE_FIELD_NAME;
import static oleg.sopilnyak.test.service.command.io.IOFieldNames.VALUE_FIELD_NAME;

/**
 * Type: I/O school-command Long-id input parameter
 *
 * @see Input
 */
@JsonSerialize(using = NumberIdParameter.Serializer.class)
@JsonDeserialize(using = NumberIdParameter.Deserializer.class)
public record NumberIdParameter<T extends Number>(T value) implements Input<T> {
    /**
     * JSON: Serializer for NumberIdParameter
     *
     * @see StdSerializer
     * @see NumberIdParameter
     */
    static class Serializer extends StdSerializer<NumberIdParameter<?>> {
        public Serializer() {
            this(null);
        }

        protected Serializer(Class<NumberIdParameter<?>> t) {
            super(t);
        }

        @Override
        public void serialize(final NumberIdParameter parameter,
                              final JsonGenerator generator,
                              final SerializerProvider serializerProvider) throws IOException {
            generator.writeStartObject();
            generator.writeStringField(TYPE_FIELD_NAME, NumberIdParameter.class.getName());
            generator.writeNumberField(VALUE_FIELD_NAME, parameter.value().longValue());
            generator.writeEndObject();
        }
    }

    /**
     * JSON: Deserializer for NumberIdParameter
     *
     * @see StdDeserializer
     * @see NumberIdParameter
     */
    static class Deserializer extends StdDeserializer<NumberIdParameter<?>> {

        public Deserializer() {
            this(NumberIdParameter.class);
        }

        protected Deserializer(Class<?> vc) {
            super(vc);
        }

        @Override
        public NumberIdParameter<?> deserialize(final JsonParser jsonParser, final DeserializationContext deserializationContext)
                throws IOException {
            final TreeNode valueNode = jsonParser.readValueAsTree().get(VALUE_FIELD_NAME);
            return new NumberIdParameter<>(isNull(valueNode) ? -1L : longValueOf(valueNode));
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
