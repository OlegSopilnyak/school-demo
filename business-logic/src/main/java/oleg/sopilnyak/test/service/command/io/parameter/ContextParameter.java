package oleg.sopilnyak.test.service.command.io.parameter;

import static oleg.sopilnyak.test.service.command.io.IOFieldNames.TYPE_FIELD_NAME;
import static oleg.sopilnyak.test.service.command.io.IOFieldNames.VALUE_FIELD_NAME;

import oleg.sopilnyak.test.service.command.io.IOBase;
import oleg.sopilnyak.test.service.command.io.Input;
import oleg.sopilnyak.test.service.command.type.core.Context;

import java.io.IOException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

/**
 * Type: I/O school-command command's execution Context input parameter
 *
 * @see Input
 * @see Context
 */
@JsonSerialize(using = ContextParameter.Serializer.class)
@JsonDeserialize(using = ContextParameter.Deserializer.class)
@SuppressWarnings({"unchecked", "rawtypes"})
public record ContextParameter<R, T extends Context<R>>(T value) implements Input<T> {

    /**
     * JSON: Serializer for ContextParameter
     *
     * @see StdSerializer
     * @see ContextParameter
     */
    static class Serializer extends StdSerializer<ContextParameter> {
        public Serializer() {
            this(ContextParameter.class);
        }

        protected Serializer(Class<ContextParameter> t) {
            super(t);
        }

        @Override
        public void serialize(final ContextParameter parameter, final JsonGenerator generator,
                              final SerializerProvider notUsed) throws IOException {
            generator.writeStartObject();
            generator.writeStringField(TYPE_FIELD_NAME, ContextParameter.class.getName());
            generator.writeFieldName(VALUE_FIELD_NAME);
            generator.writeRawValue(((ObjectMapper) generator.getCodec()).writeValueAsString(parameter.value));
            generator.writeEndObject();
        }
    }

    /**
     * JSON: Deserializer for ContextParameter
     *
     * @see StdDeserializer
     * @see ContextParameter
     */
    static class Deserializer extends StdDeserializer<ContextParameter> {
        public Deserializer() {
            this(ContextParameter.class);
        }

        protected Deserializer(Class<ContextParameter> vc) {
            super(vc);
        }

        @Override
        public ContextParameter deserialize(
                final JsonParser jsonParser, final DeserializationContext notUsed
        ) throws IOException {
            final TreeNode treeNode = jsonParser.readValueAsTree();
            final Class<?> parameterClass = IOBase.restoreIoBaseClass(treeNode, ContextParameter.class);
            if (!parameterClass.isAssignableFrom(ContextParameter.class)) {
                return null;
            }
            final Context<?> context = restoreContext(treeNode.get(VALUE_FIELD_NAME), jsonParser);
            return new ContextParameter(context);
        }

        // private methods
        private static Context<?> restoreContext(final TreeNode valueNode, final JsonParser parser) throws IOException {
            return ((ObjectMapper) parser.getCodec()).readValue(valueNode.toString(), Context.class);
        }
    }
}
