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

import java.io.IOException;

import static oleg.sopilnyak.test.service.command.io.IOFieldNames.*;

/**
 * Type: I/O school-command the pair of String input parameter
 *
 * @see PairParameter
 * @see Long
 */
@JsonSerialize(using = StringPairParameter.Serializer.class)
@JsonDeserialize(using = StringPairParameter.Deserializer.class)
public record StringPairParameter(String first, String second) implements PairParameter<String> {
    /**
     * JSON: Serializer for LongIdParameter
     *
     * @see StdSerializer
     * @see StringPairParameter
     */
    static class Serializer extends StdSerializer<StringPairParameter> {
        public Serializer() {
            this(null);
        }

        protected Serializer(Class<StringPairParameter> t) {
            super(t);
        }

        @Override
        public void serialize(final StringPairParameter parameter,
                              final JsonGenerator generator,
                              final SerializerProvider serializerProvider) throws IOException {
            generator.writeStartObject();
            generator.writeStringField(TYPE_FIELD_NAME, StringPairParameter.class.getName());
            generator.writeStringField(NESTED_TYPE_FIELD_NAME, Long.class.getName());
            generator.writeFieldName(VALUE_FIELD_NAME);
            generator.writeStartObject();
            generator.writeStringField(FIRST_FIELD_NAME, parameter.value().first());
            generator.writeStringField(SECOND_FIELD_NAME, parameter.value().second());
            generator.writeEndObject();
            generator.writeEndObject();
        }
    }

    /**
     * JSON: Deserializer for LongIdParameter
     *
     * @see StdDeserializer
     * @see StringPairParameter
     */
    static class Deserializer extends StdDeserializer<StringPairParameter> {

        public Deserializer() {
            this(StringPairParameter.class);
        }

        protected Deserializer(Class<?> vc) {
            super(vc);
        }

        @Override
        public StringPairParameter deserialize(final JsonParser jsonParser, final DeserializationContext deserializationContext)
                throws IOException {
            final TreeNode valueNode = jsonParser.readValueAsTree().get(VALUE_FIELD_NAME);
            final String firstValue = stringValueOf(valueNode.get(FIRST_FIELD_NAME));
            final String secondValue = stringValueOf(valueNode.get(SECOND_FIELD_NAME));
            return new StringPairParameter(firstValue, secondValue);
        }

        private String stringValueOf(final TreeNode node) {
            return node instanceof TextNode textNode ? textNode.asText() : "";
        }
    }
}
