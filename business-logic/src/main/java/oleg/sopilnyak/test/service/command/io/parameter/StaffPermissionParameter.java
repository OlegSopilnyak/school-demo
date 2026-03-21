package oleg.sopilnyak.test.service.command.io.parameter;

import static oleg.sopilnyak.test.service.command.io.IOFieldNames.TYPE_FIELD_NAME;
import static oleg.sopilnyak.test.service.command.io.IOFieldNames.VALUE_FIELD_NAME;

import oleg.sopilnyak.test.school.common.model.authentication.Permission;
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
@JsonSerialize(using = StaffPermissionParameter.Serializer.class)
@JsonDeserialize(using = StaffPermissionParameter.Deserializer.class)
public record StaffPermissionParameter(Permission value) implements Input<Permission> {
    /**
     * JSON: Serializer for StringParameter
     *
     * @see StdSerializer
     * @see StaffPermissionParameter
     */
    static class Serializer extends StdSerializer<StaffPermissionParameter> {
        public Serializer() {
            this(null);
        }

        protected Serializer(Class<StaffPermissionParameter> t) {
            super(t);
        }

        @Override
        public void serialize(final StaffPermissionParameter parameter, final JsonGenerator generator,
                              final SerializerProvider notUsed) throws IOException {
            generator.writeStartObject();
            generator.writeStringField(TYPE_FIELD_NAME, StaffPermissionParameter.class.getName());
            generator.writeStringField(VALUE_FIELD_NAME, parameter.value().name());
            generator.writeEndObject();
        }
    }

    /**
     * JSON: Deserializer for StringParameter
     *
     * @see StdDeserializer
     * @see StaffPermissionParameter
     */
    static class Deserializer extends StdDeserializer<StaffPermissionParameter> {

        public Deserializer() {
            this(StaffPermissionParameter.class);
        }

        protected Deserializer(Class<?> vc) {
            super(vc);
        }

        @Override
        public StaffPermissionParameter deserialize(final JsonParser jsonParser, final DeserializationContext deserializationContext)
                throws IOException {
            final TreeNode treeNode = jsonParser.readValueAsTree();
            IOBase.restoreIoBaseClass(treeNode, StaffPermissionParameter.class);
            return new StaffPermissionParameter(fromJson(treeNode.get(VALUE_FIELD_NAME)));
        }

        // private methods
        private Permission fromJson(final TreeNode valueNode) throws IOException {
            if (valueNode instanceof TextNode textNode) {
                return fromString(textNode.asText());
            }
            throw new IOException("Input Permission Value Node Type is missing :" + valueNode);
        }

        private Permission fromString(final String value) throws IOException {
            try {
                return Permission.valueOf(value);
            } catch (Exception e) {
                throw new IOException("Illegal Permission Value: " + value, e);
            }
        }
    }
}
