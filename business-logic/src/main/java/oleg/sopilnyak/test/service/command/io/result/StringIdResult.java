package oleg.sopilnyak.test.service.command.io.result;

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
import com.fasterxml.jackson.databind.node.TextNode;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

/**
 * Type: I/O school-command String-ID command execution result
 *
 * @see Output
 * @see String
 */
@JsonSerialize(using = StringIdResult.Serializer.class)
@JsonDeserialize(using = StringIdResult.Deserializer.class)
public record StringIdResult(String value) implements Output<String> {
    /**
     * JSON: Serializer for StringIdResult
     *
     * @see StdSerializer
     * @see StringIdResult
     */
    static class Serializer extends StdSerializer<StringIdResult> {
        public Serializer() {
            this(null);
        }

        protected Serializer(Class<StringIdResult> t) {
            super(t);
        }

        @Override
        public void serialize(
                final StringIdResult parameter, final JsonGenerator generator, final SerializerProvider ignored
        ) throws IOException {
            generator.writeStartObject();
            generator.writeStringField(IOFieldNames.TYPE_FIELD_NAME, StringIdResult.class.getName());
            generator.writeStringField(IOFieldNames.VALUE_FIELD_NAME, parameter.value());
            generator.writeEndObject();
        }
    }

    /**
     * JSON: Deserializer for StringIdResult
     *
     * @see StdDeserializer
     * @see StringIdResult
     */
    static class Deserializer extends StdDeserializer<StringIdResult> {

        public Deserializer() {
            this(StringIdResult.class);
        }

        protected Deserializer(Class<?> vc) {
            super(vc);
        }

        @Override
        public StringIdResult deserialize(
                final JsonParser jsonParser, final DeserializationContext deserializationContext
        ) throws IOException {
            final TreeNode valueNode = jsonParser.readValueAsTree().get(IOFieldNames.VALUE_FIELD_NAME);
            return new StringIdResult(valueNode instanceof TextNode textNode ? textNode.asText() : "");
        }
    }
}
