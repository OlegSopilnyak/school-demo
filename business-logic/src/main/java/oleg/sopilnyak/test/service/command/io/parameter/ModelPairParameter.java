package oleg.sopilnyak.test.service.command.io.parameter;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.node.TextNode;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import oleg.sopilnyak.test.school.common.model.BaseType;
import oleg.sopilnyak.test.service.message.payload.BasePayload;

import java.io.IOException;
import java.util.Objects;

import static oleg.sopilnyak.test.service.command.io.IOFieldNames.*;

/**
 * Type: I/O school-command the pair of BaseType input parameter
 *
 * @see PairParameter
 * @see BasePayload
 */
@JsonSerialize(using = ModelPairParameter.Serializer.class)
@JsonDeserialize(using = ModelPairParameter.Deserializer.class)
public record ModelPairParameter<T extends BaseType>(T first, T second) implements PairParameter<T> {
    /**
     * JSON: Serializer for PayloadPairParameter
     *
     * @see StdSerializer
     * @see ModelPairParameter
     */
    static class Serializer extends StdSerializer<ModelPairParameter<?>> {
        public Serializer() {
            this(null);
        }

        protected Serializer(Class<ModelPairParameter<?>> t) {
            super(t);
        }

        @Override
        public void serialize(final ModelPairParameter<?> parameter,
                              final JsonGenerator generator,
                              final SerializerProvider serializerProvider) throws IOException {
            final Class<?> pairParameterClass = parameter.first.getClass();
            if (!Objects.equals(pairParameterClass, parameter.second.getClass())) {
                throw new IOException("Pair parameter class mismatch");
            }
            // storing JSON
            generator.writeStartObject();
            generator.writeStringField(TYPE_FIELD_NAME, ModelPairParameter.class.getName());
            generator.writeStringField(NESTED_TYPE_FIELD_NAME, pairParameterClass.getName());
            // storing JSON for pair value
            generatePairValue(parameter, generator);
            generator.writeEndObject();
        }

        private static void generatePairValue(final ModelPairParameter<?> parameter,
                                              final JsonGenerator generator) throws IOException {
            final ObjectMapper mapper = (ObjectMapper) generator.getCodec();
            generator.writeFieldName(VALUE_FIELD_NAME);
            generator.writeStartObject();
            generator.writeFieldName(FIRST_FIELD_NAME);
            generator.writeRawValue(mapper.writeValueAsString(parameter.value().first()));
            generator.writeFieldName(SECOND_FIELD_NAME);
            generator.writeRawValue(mapper.writeValueAsString(parameter.value().second()));
            generator.writeEndObject();
        }
    }

    /**
     * JSON: Deserializer for PayloadPairParameter
     *
     * @see StdDeserializer
     * @see ModelPairParameter
     */
    static class Deserializer extends StdDeserializer<ModelPairParameter<?>> {

        public Deserializer() {
            this(ModelPairParameter.class);
        }

        protected Deserializer(Class<ModelPairParameter> vc) {
            super(vc);
        }

        @Override
        public ModelPairParameter<?> deserialize(final JsonParser jsonParser, final DeserializationContext deserializationContext)
                throws IOException {
            final TreeNode treeNode = jsonParser.readValueAsTree();
            try {
                final ObjectMapper mapper = (ObjectMapper) jsonParser.getCodec();
                final Class<?> nestedClass = nestedClass(treeNode.get(NESTED_TYPE_FIELD_NAME));
                final JavaType valueJavaType = mapper.getTypeFactory().constructType(nestedClass);
                final TreeNode valueNode = treeNode.get(VALUE_FIELD_NAME);
                return new ModelPairParameter<>(
                        mapper.readValue(valueNode.get(FIRST_FIELD_NAME).toString(), valueJavaType),
                        mapper.readValue(valueNode.get(SECOND_FIELD_NAME).toString(), valueJavaType)
                );
            } catch (ClassNotFoundException e) {
                throw new IOException("Wrong parameter nested type", e);
            }
        }

        // private methods
        private Class<?> nestedClass(final TreeNode node) throws ClassNotFoundException {
            if (node instanceof TextNode textNode) {
                return Class.forName(textNode.asText());
            } else {
                throw new ClassNotFoundException("Wrong nested type tree-node: " + node.getClass().getName());
            }
        }
    }
}
