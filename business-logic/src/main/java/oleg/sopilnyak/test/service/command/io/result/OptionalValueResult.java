package oleg.sopilnyak.test.service.command.io.result;

import static oleg.sopilnyak.test.service.command.io.IOFieldNames.TYPE_FIELD_NAME;
import static oleg.sopilnyak.test.service.command.io.IOFieldNames.VALUE_FIELD_NAME;

import oleg.sopilnyak.test.service.command.io.IOFieldNames;
import oleg.sopilnyak.test.service.command.io.Output;

import java.io.IOException;
import java.util.Optional;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.node.TextNode;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

/**
 * Type: I/O school-command Optional command execution result
 *
 * @see Output
 * @see Optional
 */
@JsonSerialize(using = OptionalValueResult.Serializer.class)
@JsonDeserialize(using = OptionalValueResult.Deserializer.class)
public record OptionalValueResult<T>(Optional<T> value) implements Output<Optional<T>> {
    /**
     * JSON: Serializer for OptionalValueResult
     *
     * @see StdSerializer
     * @see OptionalValueResult
     */
    static class Serializer<T> extends StdSerializer<OptionalValueResult<T>> {
        public Serializer() {
            this(null);
        }

        protected Serializer(Class<OptionalValueResult<T>> t) {
            super(t);
        }

        @Override
        public void serialize(
                final OptionalValueResult<T> result, final JsonGenerator generator, final SerializerProvider ignored
        ) throws IOException {
            generator.writeStartObject();
            // storing type of result
            generator.writeStringField(IOFieldNames.TYPE_FIELD_NAME, OptionalValueResult.class.getName());
            // storing result value
            final ObjectMapper mapper = (ObjectMapper) generator.getCodec();
            generator.writeFieldName(VALUE_FIELD_NAME);
            final Output<?> valueResult = result.value.isEmpty() ? Output.empty() : Output.of(result.value.get());
            final String valueJson = mapper.writeValueAsString(valueResult);
            generator.writeRawValue(valueJson);
            generator.writeEndObject();
        }
    }

    /**
     * JSON: Deserializer for OptionalValueResult
     *
     * @see StdDeserializer
     * @see OptionalValueResult
     */
    static class Deserializer<T> extends StdDeserializer<OptionalValueResult<T>> {

        public Deserializer() {
            this(OptionalValueResult.class);
        }

        protected Deserializer(Class<?> vc) {
            super(vc);
        }

        @Override
        @SuppressWarnings("unchecked")
        public OptionalValueResult<T> deserialize(
                final JsonParser jsonParser, final DeserializationContext ignored
        ) throws IOException {
            final Output<?> valueResult = restoreOutputResult(jsonParser.readValueAsTree().get(VALUE_FIELD_NAME), jsonParser);
            return new OptionalValueResult<>(
                    valueResult.isEmpty() ? Optional.empty() : Optional.of((T) valueResult.value())
            );
        }

        // private methods
        // restore result value from value-field
        private Output<?> restoreOutputResult(final TreeNode rootNode, final JsonParser jsonParser) throws IOException {
            if (rootNode.get(TYPE_FIELD_NAME) instanceof TextNode node) {
                final String valueTypeName = node.textValue();
                final ObjectMapper mapper = (ObjectMapper) jsonParser.getCodec();
                try {
                    //
                    // restore type of result value instance
                    final var valueTypeClass = Class.forName(valueTypeName).asSubclass(Output.class);
                    return mapper.readerFor(valueTypeClass).readValue(rootNode.toString());
                } catch (ClassNotFoundException | ClassCastException e) {
                    throw new IOException("Result Optional Value Type is missing :" + valueTypeName, e);
                }
            } else {
                throw new IOException("Result Optional Value Type is missing :" + rootNode);
            }
        }
    }
}
