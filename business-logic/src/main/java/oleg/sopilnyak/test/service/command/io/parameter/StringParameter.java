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
 * Type: I/O school-command string type input parameter
 *
 * @see Input
 */
@JsonSerialize(using = StringParameter.Serializer.class)
@JsonDeserialize(using = StringParameter.Deserializer.class)
public record StringParameter(String value) implements Input<String> {
    /**
     * JSON: Serializer for StringParameter
     *
     * @see StdSerializer
     * @see StringParameter
     */
    static class Serializer extends StdSerializer<StringParameter> {
        public Serializer() {
            this(null);
        }

        protected Serializer(Class<StringParameter> t) {
            super(t);
        }

        @Override
        public void serialize(final StringParameter parameter,
                              final JsonGenerator generator,
                              final SerializerProvider serializerProvider) throws IOException {
            generator.writeStartObject();
            generator.writeStringField(TYPE_FIELD_NAME, StringParameter.class.getName());
            generator.writeStringField(VALUE_FIELD_NAME, parameter.value());
            generator.writeEndObject();
        }
    }

    /**
     * JSON: Deserializer for StringParameter
     *
     * @see StdDeserializer
     * @see StringParameter
     */
    static class Deserializer extends StdDeserializer<StringParameter> {

        public Deserializer() {
            this(StringParameter.class);
        }

        protected Deserializer(Class<?> vc) {
            super(vc);
        }

        @Override
        public StringParameter deserialize(final JsonParser jsonParser, final DeserializationContext deserializationContext)
                throws IOException {
            final TreeNode valueNode = jsonParser.readValueAsTree().get(VALUE_FIELD_NAME);
            return new StringParameter(valueNode instanceof TextNode textNode ? textNode.asText() : "");
        }
    }
}
