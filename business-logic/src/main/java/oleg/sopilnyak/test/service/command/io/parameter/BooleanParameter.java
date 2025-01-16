package oleg.sopilnyak.test.service.command.io.parameter;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import oleg.sopilnyak.test.service.command.io.Input;

import java.io.IOException;

import static oleg.sopilnyak.test.service.command.io.IOFieldNames.TYPE_FIELD_NAME;
import static oleg.sopilnyak.test.service.command.io.IOFieldNames.VALUE_FIELD_NAME;

/**
 * Type: I/O school-command boolean input parameter
 *
 * @see Input
 */
@JsonSerialize(using = BooleanParameter.Serializer.class)
@JsonDeserialize(using = BooleanParameter.Deserializer.class)
public record BooleanParameter(Boolean value) implements Input<Boolean> {
    /**
     * JSON: Serializer for BooleanParameter
     *
     * @see StdSerializer
     * @see BooleanParameter
     */
    static class Serializer extends StdSerializer<BooleanParameter> {
        public Serializer() {
            this(null);
        }

        protected Serializer(Class<BooleanParameter> t) {
            super(t);
        }

        @Override
        public void serialize(final BooleanParameter parameter,
                              final JsonGenerator generator,
                              final SerializerProvider serializerProvider) throws IOException {
            generator.writeStartObject();
            generator.writeStringField(TYPE_FIELD_NAME, BooleanParameter.class.getName());
            generator.writeBooleanField(VALUE_FIELD_NAME, parameter.value());
            generator.writeEndObject();
        }
    }

    /**
     * JSON: Deserializer for BooleanParameter
     *
     * @see StdDeserializer
     * @see BooleanParameter
     */
    static class Deserializer extends StdDeserializer<BooleanParameter> {

        public Deserializer() {
            this(BooleanParameter.class);
        }

        protected Deserializer(Class<?> vc) {
            super(vc);
        }

        @Override
        public BooleanParameter deserialize(final JsonParser jsonParser, final DeserializationContext deserializationContext)
                throws IOException {
            final TreeNode valueNode = jsonParser.readValueAsTree().get(VALUE_FIELD_NAME);
            return new BooleanParameter(valueNode instanceof BooleanNode booleanNode && booleanNode.asBoolean());
        }
    }
}
