package oleg.sopilnyak.test.service.command.io.parameter;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static oleg.sopilnyak.test.service.command.io.IOFieldNames.NESTED_TYPE_FIELD_NAME;
import static oleg.sopilnyak.test.service.command.io.IOFieldNames.TYPE_FIELD_NAME;
import static oleg.sopilnyak.test.service.command.io.IOFieldNames.VALUE_FIELD_NAME;

import oleg.sopilnyak.test.service.command.io.Input;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
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
 * Type: I/O school-command Long-id input parameter
 *
 * @see Input
 */
@JsonSerialize(using = NumberIdParameter.Serializer.class)
@JsonDeserialize(using = NumberIdParameter.Deserializer.class)
public record NumberIdParameter<T extends Number>(T value) implements Input<T> {
    /**
     * JSON: Serializer for NumberIdParameter
     *
     * @see StdSerializer
     * @see NumberIdParameter
     */
    static class Serializer extends StdSerializer<NumberIdParameter<?>> {
        public Serializer() {
            this(null);
        }

        protected Serializer(Class<NumberIdParameter<?>> t) {
            super(t);
        }

        @Override
        public void serialize(final NumberIdParameter parameter,
                              final JsonGenerator generator,
                              final SerializerProvider serializerProvider) throws IOException {
            generator.writeStartObject();
            generator.writeStringField(TYPE_FIELD_NAME, NumberIdParameter.class.getName());
            serializeValue(parameter.value(), generator);
            generator.writeEndObject();
        }

    }

    /**
     * JSON: Deserializer for NumberIdParameter
     *
     * @see StdDeserializer
     * @see NumberIdParameter
     */
    static class Deserializer extends StdDeserializer<NumberIdParameter<?>> {

        public Deserializer() {
            super(NumberIdParameter.class);
        }

        @Override
        public NumberIdParameter<?> deserialize(final JsonParser jsonParser,
                                                final DeserializationContext context) throws IOException {
            // restore number-id-input instance
            return deserializeParameterInput(jsonParser.readValueAsTree());
        }
    }

    // private methods
    private static void serializeValue(final Number value, final JsonGenerator generator) throws IOException {
        generator.writeFieldName(VALUE_FIELD_NAME);
        generator.writeStartObject();
        generator.writeStringField(NESTED_TYPE_FIELD_NAME, value.getClass().getName());
        generator.writeStringField(VALUE_FIELD_NAME, value.toString());
        generator.writeEndObject();

    }

    private static NumberIdParameter<?> deserializeParameterInput(final TreeNode inputTreeNode) throws IOException {
        final TreeNode inputTypeNode = inputTreeNode.get(TYPE_FIELD_NAME);
        if (nonNull(inputTypeNode) && inputTypeNode instanceof TextNode textTypeNode) {
            final String inputClassName = textTypeNode.textValue();
            try {
                //
                // restore type of number-id-input instance
                final Class<? extends NumberIdParameter> valueTypeClass = Class.forName(inputClassName).asSubclass(NumberIdParameter.class);
                //
                // restore value of the number-id-input
                final Number restoredValue = deserializeValue(inputTreeNode.get(VALUE_FIELD_NAME));
                //
                // construct number-id-input instance from class
                return valueTypeClass.getConstructor(Number.class).newInstance(restoredValue);
            } catch (ClassNotFoundException | NoSuchMethodException |
                     InvocationTargetException | InstantiationException | IllegalAccessException e) {
                throw new IOException("Input Value Type is missing :" + inputClassName, e);
            }
        } else {
            throw new IOException("Input Value Type is missing :" + inputTreeNode);
        }
    }

    private static Number deserializeValue(final TreeNode treeNode) throws IOException {
        if (isNull(treeNode)) {
            throw new IOException("Input Value Node is missing (null)");
        }
        //
        // deserialize string-type value
        final String stringValue;
        final TreeNode valueNode = treeNode.get(VALUE_FIELD_NAME);
        if (nonNull(valueNode) && valueNode instanceof TextNode textNode) {
            stringValue = textNode.textValue();
        } else {
            throw new IOException("Input Value Node is missing :" + valueNode);
        }
        //
        // deserialize value type
        final TreeNode valueTypeNode = treeNode.get(NESTED_TYPE_FIELD_NAME);
        if (nonNull(valueTypeNode) && valueTypeNode instanceof TextNode textTypeNode) {
            final String valueClassName = textTypeNode.textValue();
            try {
                final Class<? extends Number> valueTypeClass = Class.forName(valueClassName).asSubclass(Number.class);
                return valueTypeClass.getConstructor(String.class).newInstance(stringValue);
            } catch (ClassNotFoundException | NoSuchMethodException |
                     InvocationTargetException | InstantiationException | IllegalAccessException e) {
                throw new IOException("Input Value Type is missing :" + valueClassName, e);
            }
        }
        throw new IOException("Input Value Node is missing :" + valueTypeNode);
    }
}
