package oleg.sopilnyak.test.service.command.io.parameter;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.node.TextNode;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import oleg.sopilnyak.test.service.command.io.Input;

import java.io.IOException;

/**
 * Type: I/O school-command String-id input parameter
 *
 * @see Input
 */
@JsonSerialize(using = StringIdParameter.Serializer.class)
@JsonDeserialize(using = StringIdParameter.Deserializer.class)
public record StringIdParameter(String value) implements Input<String> {
    /**
     * JSON: Serializer for StringIdParameter
     *
     * @see StdSerializer
     * @see StringIdParameter
     */
    static class Serializer extends StdSerializer<StringIdParameter> {
        public Serializer() {
            this(null);
        }

        protected Serializer(Class<StringIdParameter> t) {
            super(t);
        }

        @Override
        public void serialize(final StringIdParameter parameter,
                              final JsonGenerator generator,
                              final SerializerProvider serializerProvider) throws IOException {
            generator.writeStartObject();
            generator.writeStringField(Input.TYPE_FIELD_NAME, StringIdParameter.class.getName());
            generator.writeStringField(Input.VALUE_FIELD_NAME, parameter.value());
            generator.writeEndObject();
        }
    }

    /**
     * JSON: Deserializer for LongIdParameter
     *
     * @see StdDeserializer
     * @see StringIdParameter
     */
    static class Deserializer extends StdDeserializer<StringIdParameter> {

        public Deserializer() {
            this(LongIdParameter.class);
        }

        protected Deserializer(Class<?> vc) {
            super(vc);
        }

        @Override
        public StringIdParameter deserialize(final JsonParser jsonParser, final DeserializationContext deserializationContext)
                throws IOException {
            final TreeNode valueNode = jsonParser.readValueAsTree().get(Input.VALUE_FIELD_NAME);
            return new StringIdParameter(valueNode instanceof TextNode textNode ? textNode.asText() : "");
        }
    }
}
