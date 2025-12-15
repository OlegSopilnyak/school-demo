package oleg.sopilnyak.test.service.command.io.parameter;

import static oleg.sopilnyak.test.service.command.io.IOFieldNames.TYPE_FIELD_NAME;
import static oleg.sopilnyak.test.service.command.io.IOFieldNames.VALUE_FIELD_NAME;

import oleg.sopilnyak.test.service.command.io.Input;

import java.io.IOException;
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

/**
 * Type: I/O school-command String-ID input parameter
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
            generator.writeStringField(TYPE_FIELD_NAME, StringIdParameter.class.getName());
            generator.writeStringField(VALUE_FIELD_NAME, parameter.value());
            generator.writeEndObject();
        }
    }

    /**
     * JSON: Deserializer for StringIdParameter
     *
     * @see StdDeserializer
     * @see StringIdParameter
     */
    static class Deserializer extends StdDeserializer<StringIdParameter> {

        public Deserializer() {
            this(StringIdParameter.class);
        }

        protected Deserializer(Class<?> vc) {
            super(vc);
        }

        @Override
        public StringIdParameter deserialize(final JsonParser jsonParser, final DeserializationContext deserializationContext)
                throws IOException {
            final TreeNode valueNode = jsonParser.readValueAsTree().get(VALUE_FIELD_NAME);
            return new StringIdParameter(valueNode instanceof TextNode textNode ? textNode.asText() : "");
        }
    }
}
