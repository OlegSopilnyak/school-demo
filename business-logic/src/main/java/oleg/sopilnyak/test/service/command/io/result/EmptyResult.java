package oleg.sopilnyak.test.service.command.io.result;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import oleg.sopilnyak.test.service.command.io.IOFieldNames;
import oleg.sopilnyak.test.service.command.io.Output;

import java.io.IOException;

/**
 * Type: I/O school-command empty command execution result (no results)
 *
 * @see Output
 * @see Void
 */
@JsonSerialize(using = EmptyResult.Serializer.class)
@JsonDeserialize(using = EmptyResult.Deserializer.class)
public class EmptyResult<T> implements Output<T> {
    /**
     * To get the value of command execution result
     *
     * @return value of the result
     */
    @Override
    public T value() {
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
     * JSON: Serializer for EmptyResult
     *
     * @see StdSerializer
     * @see EmptyResult
     */
    static class Serializer extends StdSerializer<EmptyResult<?>> {
        public Serializer() {
            this(null);
        }

        protected Serializer(Class<EmptyResult<?>> t) {
            super(t);
        }

        @Override
        public void serialize(final EmptyResult parameter,
                              final JsonGenerator generator,
                              final SerializerProvider serializerProvider) throws IOException {
            generator.writeStartObject();
            generator.writeStringField(IOFieldNames.TYPE_FIELD_NAME, EmptyResult.class.getName());
            generator.writeStringField(IOFieldNames.VALUE_FIELD_NAME, "none");
            generator.writeEndObject();
        }
    }

    /**
     * JSON: Deserializer for EmptyResult
     *
     * @see StdDeserializer
     * @see EmptyResult
     */
    static class Deserializer extends StdDeserializer<EmptyResult<?>> {

        public Deserializer() {
            this(EmptyResult.class);
        }

        protected Deserializer(Class<?> vc) {
            super(vc);
        }

        @Override
        public EmptyResult<?> deserialize(final JsonParser jsonParser, final DeserializationContext deserializationContext)
                throws IOException {
            return new EmptyResult<>();
        }
    }
}
