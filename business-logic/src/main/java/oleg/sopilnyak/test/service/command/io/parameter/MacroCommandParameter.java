package oleg.sopilnyak.test.service.command.io.parameter;

import static oleg.sopilnyak.test.service.command.io.IOFieldNames.TYPE_FIELD_NAME;
import static oleg.sopilnyak.test.service.command.io.IOFieldNames.VALUE_FIELD_NAME;

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
import java.io.IOException;
import java.util.Deque;
import java.util.LinkedList;
import lombok.ToString;
import lombok.Value;
import oleg.sopilnyak.test.service.command.io.Input;
import oleg.sopilnyak.test.service.command.type.base.Context;

/**
 * Type-wrapper: The wrapper of MacroCommand input parameter bean
 */
@Value
//@Data
@ToString
@JsonSerialize(using = MacroCommandParameter.Serializer.class)
@JsonDeserialize(using = MacroCommandParameter.Deserializer.class)
public class MacroCommandParameter implements Input<MacroCommandParameter> {
    Input<?> rootInput;
    Deque<Context<?>> nestedContexts = new LinkedList<>();

    public MacroCommandParameter(final Input<?> macroOriginalParameter, final Deque<Context<?>> nestedContexts) {
        this.rootInput = macroOriginalParameter;
        this.nestedContexts.addAll(nestedContexts);
    }

    /**
     * To get the value of command input-output entity
     *
     * @return value of the parameter
     */
    @Override
    public MacroCommandParameter value() {
        return this;
    }

    /**
     * JSON: Serializer for PayloadParameter
     *
     * @see StdSerializer
     * @see PayloadParameter
     */
    static class Serializer extends StdSerializer<MacroCommandParameter> {
        public Serializer() {
            this(null);
        }

        protected Serializer(Class<MacroCommandParameter> t) {
            super(t);
        }

        @Override
        public void serialize(final MacroCommandParameter parameter,
                              final JsonGenerator generator,
                              final SerializerProvider serializerProvider) throws IOException {
            final ObjectMapper mapper = (ObjectMapper) generator.getCodec();
            generator.writeStartObject();
            generator.writeStringField(TYPE_FIELD_NAME, parameter.getClass().getName());
//            generator.writeStringField(NESTED_TYPE_FIELD_NAME, parameter.getClass().getName());
            generator.writeFieldName(VALUE_FIELD_NAME);
            generator.writeRawValue(mapper.writeValueAsString(parameter));
            generator.writeEndObject();
        }
    }

    /**
     * JSON: Deserializer for PayloadParameter
     *
     * @see StdDeserializer
     * @see PayloadParameter
     */
    static class Deserializer extends StdDeserializer<MacroCommandParameter> {
        public Deserializer() {
            this(MacroCommandParameter.class);
        }

        protected Deserializer(Class<MacroCommandParameter> vc) {
            super(vc);
        }

        @Override
        public MacroCommandParameter deserialize(final JsonParser jsonParser,
                                                 final DeserializationContext deserializationContext) throws IOException {
            final TreeNode treeNode = jsonParser.readValueAsTree();
//            try {
//                final Class<?> nestedClass = restoreNestedClass(treeNode.get(NESTED_TYPE_FIELD_NAME));
            final ObjectMapper mapper = (ObjectMapper) jsonParser.getCodec();
            return null;
//                return new PayloadParameter<>(mapper.readValue(
//                        treeNode.get(VALUE_FIELD_NAME).toString(),
//                        mapper.getTypeFactory().constructType(nestedClass)
//                ));
//            } catch (ClassNotFoundException e) {
//                throw new IOException("Wrong parameter nested type", e);
//            }
        }

        // private methods
        private static Class<?> restoreNestedClass(final TreeNode nestedClassNode) throws ClassNotFoundException {
            if (nestedClassNode instanceof TextNode node) {
                return Class.forName(node.asText());
            } else {
                throw new ClassNotFoundException("Wrong nested type tree-node: " + nestedClassNode.getClass().getName());
            }
        }
    }

}
