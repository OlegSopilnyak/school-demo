package oleg.sopilnyak.test.service.command.io.parameter;

import oleg.sopilnyak.test.service.command.io.IOFieldNames;
import oleg.sopilnyak.test.service.command.io.Input;

import java.io.IOException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

/**
 * Type: I/O school-command empty command input parameter (no results)
 *
 * @see Input
 * @see Void
 */
@JsonSerialize(using = EmptyParameter.Serializer.class)
@JsonDeserialize(using = EmptyParameter.Deserializer.class)
public final class EmptyParameter implements Input<Void> {
    @Override
    public boolean equals(Object obj) {
        // EmptyParameter is a singleton, so we can check by class type
        return obj instanceof EmptyParameter;
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    /**
     * To get the value of command input-output entity
     *
     * @return value of the parameter
     */
    @Override
    public Void value() {
        return null;
    }

    /**
     * To check is result's output value is empty
     *
     * @return true if no data in the output result
     */
    @Override
    public boolean isEmpty() {
        return true;
    }

    /**
     * JSON: Serializer for EmptyParameter
     *
     * @see StdSerializer
     * @see EmptyParameter
     */
    static class Serializer extends StdSerializer<EmptyParameter> {
        public Serializer() {
            this(null);
        }

        protected Serializer(Class<EmptyParameter> t) {
            super(t);
        }

        @Override
        public void serialize(final EmptyParameter parameter,
                              final JsonGenerator generator,
                              final SerializerProvider serializerProvider) throws IOException {
            generator.writeStartObject();
            generator.writeStringField(IOFieldNames.TYPE_FIELD_NAME, EmptyParameter.class.getName());
            generator.writeStringField(IOFieldNames.VALUE_FIELD_NAME, "none");
            generator.writeEndObject();
        }
    }

    /**
     * JSON: Deserializer for EmptyParameter
     *
     * @see StdDeserializer
     * @see EmptyParameter
     */
    static class Deserializer extends StdDeserializer<EmptyParameter> {

        public Deserializer() {
            this(EmptyParameter.class);
        }

        protected Deserializer(Class<?> vc) {
            super(vc);
        }

        @Override
        public EmptyParameter deserialize(final JsonParser jsonParser, final DeserializationContext deserializationContext)
                throws IOException {
            return new EmptyParameter();
        }
    }
}
