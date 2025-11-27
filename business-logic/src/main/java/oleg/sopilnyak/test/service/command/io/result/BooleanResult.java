package oleg.sopilnyak.test.service.command.io.result;

import static java.util.Objects.isNull;

import oleg.sopilnyak.test.service.command.io.IOFieldNames;
import oleg.sopilnyak.test.service.command.io.Output;

import java.io.IOException;
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

/**
 * Type: I/O school-command Boolean command execution result
 *
 * @see Output
 * @see Boolean
 */
@JsonSerialize(using = BooleanResult.Serializer.class)
@JsonDeserialize(using = BooleanResult.Deserializer.class)
public record BooleanResult(Boolean value) implements Output<Boolean> {
    /**
     * JSON: Serializer for BooleanResult
     *
     * @see StdSerializer
     * @see BooleanResult
     */
    static class Serializer extends StdSerializer<BooleanResult> {
        public Serializer() {
            this(null);
        }

        protected Serializer(Class<BooleanResult> t) {
            super(t);
        }

        @Override
        public void serialize(final BooleanResult parameter,
                              final JsonGenerator generator,
                              final SerializerProvider serializerProvider) throws IOException {
            generator.writeStartObject();
            generator.writeStringField(IOFieldNames.TYPE_FIELD_NAME, BooleanResult.class.getName());
            generator.writeBooleanField(IOFieldNames.VALUE_FIELD_NAME, parameter.value());
            generator.writeEndObject();
        }
    }

    /**
     * JSON: Deserializer for BooleanResult
     *
     * @see StdDeserializer
     * @see BooleanResult
     */
    static class Deserializer extends StdDeserializer<BooleanResult> {

        public Deserializer() {
            this(BooleanResult.class);
        }

        protected Deserializer(Class<?> vc) {
            super(vc);
        }

        @Override
        public BooleanResult deserialize(final JsonParser jsonParser, final DeserializationContext deserializationContext)
                throws IOException {
            final TreeNode valueNode = jsonParser.readValueAsTree().get(IOFieldNames.VALUE_FIELD_NAME);
            return new BooleanResult(!isNull(valueNode) && booleanValueOf(valueNode));
        }

        private static boolean booleanValueOf(final TreeNode node) {
            return node instanceof BooleanNode booleanNode ? booleanNode.booleanValue() : Boolean.FALSE;
        }
    }
}
