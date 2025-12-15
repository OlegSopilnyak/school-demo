package oleg.sopilnyak.test.service.command.io.result;


import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static oleg.sopilnyak.test.service.command.io.IOFieldNames.NESTED_TYPE_FIELD_NAME;
import static oleg.sopilnyak.test.service.command.io.IOFieldNames.TYPE_FIELD_NAME;
import static oleg.sopilnyak.test.service.command.io.IOFieldNames.VALUE_FIELD_NAME;

import oleg.sopilnyak.test.service.command.io.IOFieldNames;
import oleg.sopilnyak.test.service.command.io.Output;

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
 * Type: I/O school-command Number-ID command execution result
 *
 * @see Output
 * @see Number
 */
@JsonSerialize(using = NumberIdResult.Serializer.class)
@JsonDeserialize(using = NumberIdResult.Deserializer.class)
public record NumberIdResult<T extends Number>(T value) implements Output<T> {
    /**
     * JSON: Serializer for NumberIdResult
     *
     * @see StdSerializer
     * @see NumberIdResult
     */
    static class Serializer<T extends Number> extends StdSerializer<NumberIdResult<T>> {
        public Serializer() {
            this(null);
        }

        protected Serializer(Class<NumberIdResult<T>> t) {
            super(t);
        }

        @Override
        public void serialize(
                final NumberIdResult<T> parameter, final JsonGenerator generator, final SerializerProvider ignored
        ) throws IOException {
            generator.writeStartObject();
            generator.writeStringField(IOFieldNames.TYPE_FIELD_NAME, NumberIdResult.class.getName());
            generator.writeFieldName(VALUE_FIELD_NAME);
            serializeParameterValue(parameter.value(), generator);
            generator.writeEndObject();
        }

        // private methods
        private void serializeParameterValue(final T value, final JsonGenerator generator) throws IOException {
            generator.writeStartObject();
            generator.writeStringField(NESTED_TYPE_FIELD_NAME, value.getClass().getName());
            generator.writeStringField(VALUE_FIELD_NAME, value.toString());
            generator.writeEndObject();

        }
    }

    /**
     * JSON: Deserializer for NumberIdResult
     *
     * @see StdDeserializer
     * @see NumberIdResult
     */
    static class Deserializer<T extends Number> extends StdDeserializer<NumberIdResult<T>> {

        public Deserializer() {
            this(NumberIdResult.class);
        }

        protected Deserializer(Class<?> vc) {
            super(vc);
        }

        @Override
        @SuppressWarnings("unchecked")
        public NumberIdResult<T> deserialize(
                final JsonParser jsonParser, final DeserializationContext deserializationContext
        ) throws IOException {
            // preparing root-node
            final TreeNode resultTreeNode = jsonParser.readValueAsTree();
            // restore number-id-result instance
            final TreeNode resultTypeNode = resultTreeNode.get(TYPE_FIELD_NAME);
            if (nonNull(resultTypeNode) && resultTypeNode instanceof TextNode node) {
                final String valueTypeName = node.textValue();
                try {
                    //
                    // restore type of number-id-result instance
                    final var valueTypeClass = Class.forName(valueTypeName).asSubclass(NumberIdResult.class);
                    //
                    // restore value of the number-id-result
                    final TreeNode parameterValueNode = resultTreeNode.get(VALUE_FIELD_NAME);
                    final T restoredValue = deserializeValue(parameterValueNode);
                    //
                    // construct number-id-result instance from class
                    return valueTypeClass.getConstructor(Number.class).newInstance(restoredValue);
                } catch (ClassNotFoundException | NoSuchMethodException |
                         InvocationTargetException | InstantiationException |
                         IllegalAccessException e) {
                    throw new IOException("Result Number Value Type is missing :" + valueTypeName, e);
                }
            } else {
                throw new IOException("Result Number Value Type is missing :" + resultTreeNode);
            }
        }

        // private methods
        @SuppressWarnings("unchecked")
        private T deserializeValue(final TreeNode treeNode) throws IOException {
            if (isNull(treeNode)) {
                throw new IOException("Result Number Value Node is missing (null)");
            }
            //
            // getting value as string
            final String stringValue;
            final TreeNode valueNode = treeNode.get(VALUE_FIELD_NAME);
            if (nonNull(valueNode) && valueNode instanceof TextNode textNode) {
                stringValue = textNode.textValue();
            } else {
                throw new IOException("Result Number Value Node is missing :" + valueNode);
            }
            //
            // deserialize value type
            final TreeNode valueTypeNode = treeNode.get(NESTED_TYPE_FIELD_NAME);
            if (nonNull(valueTypeNode) && valueTypeNode instanceof TextNode textTypeNode) {
                final String valueTypeName = textTypeNode.textValue();
                try {
                    // building value type from value-type class-name
                    final var valueType = Class.forName(valueTypeName).asSubclass(Number.class);
                    // building value instance using one's type and value as string
                    return (T) valueType.getConstructor(String.class).newInstance(stringValue);
                } catch (ClassNotFoundException | NoSuchMethodException |
                         InvocationTargetException | InstantiationException |
                         IllegalAccessException e) {
                    throw new IOException("Result Number Value Type is missing :" + valueTypeName, e);
                }
            }
            throw new IOException("Result Number Value Node is missing :" + valueTypeNode);
        }
    }
}
