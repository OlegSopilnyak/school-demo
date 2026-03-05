package oleg.sopilnyak.test.service.command.io.parameter;

import static oleg.sopilnyak.test.service.command.io.IOFieldNames.TYPE_FIELD_NAME;
import static oleg.sopilnyak.test.service.command.io.IOFieldNames.VALUE_FIELD_NAME;

import oleg.sopilnyak.test.school.common.model.authentication.Role;
import oleg.sopilnyak.test.service.command.io.IOBase;
import oleg.sopilnyak.test.service.command.io.Input;

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
 * Type: I/O school-command string type input parameter
 *
 * @see Input
 */
@JsonSerialize(using = StaffRoleParameter.Serializer.class)
@JsonDeserialize(using = StaffRoleParameter.Deserializer.class)
public record StaffRoleParameter(Role value) implements Input<Role> {
    /**
     * JSON: Serializer for StringParameter
     *
     * @see StdSerializer
     * @see StaffRoleParameter
     */
    static class Serializer extends StdSerializer<StaffRoleParameter> {
        public Serializer() {
            this(null);
        }

        protected Serializer(Class<StaffRoleParameter> t) {
            super(t);
        }

        @Override
        public void serialize(final StaffRoleParameter parameter, final JsonGenerator generator,
                              final SerializerProvider notUsed) throws IOException {
            generator.writeStartObject();
            generator.writeStringField(TYPE_FIELD_NAME, StaffRoleParameter.class.getName());
            generator.writeStringField(VALUE_FIELD_NAME, parameter.value().name());
            generator.writeEndObject();
        }
    }

    /**
     * JSON: Deserializer for StringParameter
     *
     * @see StdDeserializer
     * @see StaffRoleParameter
     */
    static class Deserializer extends StdDeserializer<StaffRoleParameter> {

        public Deserializer() {
            this(StaffRoleParameter.class);
        }

        protected Deserializer(Class<?> vc) {
            super(vc);
        }

        @Override
        public StaffRoleParameter deserialize(final JsonParser jsonParser, final DeserializationContext deserializationContext)
                throws IOException {
            final TreeNode treeNode = jsonParser.readValueAsTree();
            IOBase.restoreIoBaseClass(treeNode, StaffRoleParameter.class);
            return new StaffRoleParameter(fromJson(treeNode.get(VALUE_FIELD_NAME)));
        }

        // private methods
        private Role fromJson(final TreeNode valueNode) throws IOException {
            if (valueNode instanceof TextNode textNode) {
                return fromString(textNode.asText());
            }
            throw new IOException("Input Role Value Node Type is missing :" + valueNode);
        }

        private Role fromString(final String value) throws IOException {
            try {
                return Role.valueOf(value);
            } catch (Exception e) {
                throw new IOException("Illegal Role Value: " + value, e);
            }
        }
    }
}
