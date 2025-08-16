package oleg.sopilnyak.test.service.command.io;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static oleg.sopilnyak.test.service.command.io.IOFieldNames.TYPE_FIELD_NAME;
import static oleg.sopilnyak.test.service.command.io.IOFieldNames.VALUE_FIELD_NAME;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.node.TextNode;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.time.Duration;
import java.time.Instant;
import oleg.sopilnyak.test.school.common.business.facade.ActionContext;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.message.CommandMessage;

/**
 * Type: I/O school-command input-output base type
 *
 * @param <P> the type of command input-output entity
 */
public interface IOBase<P> extends Serializable {
    String EXCEPTION_MESSAGE_FIELD_NAME = "message";
    String EXCEPTION_CAUSE_FIELD_NAME = "cause";
    String EXCEPTION_STACK_TRACE_FIELD_NAME = "stackTrace";

    /**
     * To get the value of command input-output entity
     *
     * @return value of the parameter
     */
    P value();

    /**
     * To check is result's output value is empty
     *
     * @return true if no data in the output result
     */
    default boolean isEmpty() {
        return isNull(value());
    }

    /**
     * To restore the base class of Input/Output instance
     *
     * @param ioTreeNode   node with class-name of the parameter
     * @param shouldBeType variant of parameter or result type
     * @param <T>          value type of parameter or result
     * @return class of the parameter or result
     * @throws IOException throws if it cannot restore the class
     */
    @SuppressWarnings("unchecked")
    static <T extends IOBase<?>> Class<T> restoreIoBaseClass(final TreeNode ioTreeNode,
                                                             final Class<T> shouldBeType) throws IOException {
        final TreeNode ioClassNameNode = ioTreeNode.get(TYPE_FIELD_NAME);
        if (ioClassNameNode instanceof TextNode typeTextNode) {
            final String ioTypeClassName = typeTextNode.asText();
            try {
                return (Class<T>) Class.forName(ioTypeClassName).asSubclass(shouldBeType);
            } catch (ClassNotFoundException | ClassCastException e) {
                // class not found or class is not ioClass
                throw new IOException("Wrong class name in node-type: " + ioTypeClassName);
            }
        } else {
            throw new IOException("Wrong node-type of ioTreeNode: " + ioClassNameNode.getClass().getName());
        }
    }

    // inner classes for JSON serializing/deserializing

    /**
     * JSON: Serializer for Throwable
     *
     * @see StdSerializer
     * @see CommandMessage#getContext()
     * @see Context#getException()
     * @see Throwable
     */
    class ExceptionSerializer<T extends Throwable> extends StdSerializer<T> {
        public ExceptionSerializer() {
            this(null);
        }

        protected ExceptionSerializer(Class<T> t) {
            super(t);
        }

        @Override
        public void serialize(final T exception,
                              final JsonGenerator generator,
                              final SerializerProvider serializerProvider) throws IOException {
            generator.writeStartObject();
            generator.writeStringField(TYPE_FIELD_NAME, exception.getClass().getName());
            generator.writeFieldName(VALUE_FIELD_NAME);
            serializeExceptionStuff(exception, generator, serializerProvider);
            generator.writeEndObject();
        }

        @SuppressWarnings("unchecked")
        private void serializeExceptionStuff(final T exception,
                                             final JsonGenerator generator,
                                             final SerializerProvider serializerProvider) throws IOException {
            // store exception body
            generator.writeStartObject();
            generator.writeStringField(EXCEPTION_MESSAGE_FIELD_NAME, exception.getMessage());
            final Throwable cause = exception.getCause();
            if (nonNull(cause) && cause != exception) {// store exception's cause
                generator.writeFieldName(EXCEPTION_CAUSE_FIELD_NAME);
                // recursive call for exception's cause
                serialize((T) cause, generator, serializerProvider);
            }
            generator.writeFieldName(EXCEPTION_STACK_TRACE_FIELD_NAME);
            generator.writeRawValue(((ObjectMapper) generator.getCodec()).writeValueAsString(exception.getStackTrace()));
            generator.writeEndObject();
        }
    }

    /**
     * JSON: Deserializer for Throwable
     *
     * @see StdDeserializer
     * @see CommandMessage#getContext()
     * @see Context#getException()
     * @see Throwable
     */
    class ExceptionDeserializer extends StdDeserializer<Throwable> {

        public ExceptionDeserializer() {
            this(Throwable.class);
        }

        protected ExceptionDeserializer(Class<Throwable> vc) {
            super(vc);
        }

        @Override
        public Throwable deserialize(final JsonParser jsonParser,
                                     final DeserializationContext deserializationContext) throws IOException {
            return restoreExceptionBy(jsonParser.readValueAsTree(), jsonParser);
        }

        private Throwable restoreExceptionBy(final TreeNode exceptionNode,
                                             final JsonParser jsonParser) throws IOException {
            final Class<? extends Throwable> exceptionClass = restoreExceptionClass(exceptionNode);
            return restoreExceptionInstance(jsonParser, exceptionNode.get(VALUE_FIELD_NAME), exceptionClass);
        }

        private Throwable restoreExceptionInstance(final JsonParser jsonParser,
                                                   final TreeNode valueNode,
                                                   final Class<? extends Throwable> exceptionClass) throws IOException {
            final String exceptionMessage = getExceptionMessage(valueNode);
            final TreeNode causeNode = valueNode.get(EXCEPTION_CAUSE_FIELD_NAME);
            return causeNode == null ?
                    // no cause in value-node
                    addRestoredStackTraceFor(
                            createExceptionInstance(exceptionClass, exceptionMessage),
                            (ObjectMapper) jsonParser.getCodec(), valueNode.get(EXCEPTION_STACK_TRACE_FIELD_NAME)
                    ) :
                    // restore cause and make exception instance
                    addRestoredStackTraceFor(
                            createExceptionInstance(exceptionClass, exceptionMessage,
                                    // recursion
                                    restoreExceptionBy(causeNode, jsonParser)
                            ),
                            (ObjectMapper) jsonParser.getCodec(), valueNode.get(EXCEPTION_STACK_TRACE_FIELD_NAME)
                    );
        }

        private static String getExceptionMessage(final TreeNode valueNode) throws IOException {
            final TreeNode messageNode = valueNode.get(EXCEPTION_MESSAGE_FIELD_NAME);
            if (messageNode instanceof TextNode textNode) {
                return textNode.asText();
            } else {
                throw new IOException("Wrong node-type of exception's message: " + messageNode.getClass().getName());
            }
        }

        private static Throwable createExceptionInstance(final Class<? extends Throwable> exceptionClass,
                                                         final String exceptionMessage) throws IOException {
            try {
                final Constructor<? extends Throwable> constructor = exceptionClass.getConstructor(String.class);
                return constructor.newInstance(exceptionMessage);
            } catch (NoSuchMethodException e) {
                throw new IOException("No constructor(String message) for class: " + exceptionClass.getName(), e);
            } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
                throw new IOException("Cannot create instance of class: " + exceptionClass.getName(), e);
            }
        }

        private static Throwable createExceptionInstance(final Class<? extends Throwable> exceptionClass,
                                                         final String exceptionMessage,
                                                         final Throwable cause) throws IOException {
            try {
                final Constructor<? extends Throwable> constructor = exceptionClass.getConstructor(String.class, Throwable.class);
                return constructor.newInstance(exceptionMessage, cause);
            } catch (NoSuchMethodException e) {
                throw new IOException("No constructor(String message, Throwable cause) for class: " + exceptionClass.getName(), e);
            } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
                throw new IOException("Cannot create instance of class: " + exceptionClass.getName(), e);
            }
        }

        private static Throwable addRestoredStackTraceFor(final Throwable restoredException,
                                                          final ObjectMapper mapper,
                                                          final TreeNode stackNode) throws IOException {
            final StackTraceElement[] stackTraceElements = mapper.readValue(stackNode.toString(), StackTraceElement[].class);
            restoredException.setStackTrace(stackTraceElements);
            return restoredException;
        }

        private static Class<? extends Throwable> restoreExceptionClass(final TreeNode exceptionNode) throws IOException {
            final TreeNode typeNode = exceptionNode.get(TYPE_FIELD_NAME);
            if (typeNode instanceof TextNode typeTextNode) {
                try {
                    return Class.forName(typeTextNode.asText()).asSubclass(Throwable.class);
                } catch (ClassNotFoundException | ClassCastException e) {
                    // class not found or class is not Throwable
                }
                // default exception class in case class building is failed
                return Exception.class;
            } else {
                throw new IOException("Wrong node-type of exception's type: " + typeNode.getClass().getName());
            }
        }
    }

    /**
     * JSON: Deserializer for action-context field
     *
     * @see StdDeserializer
     * @see CommandMessage#getActionContext()
     * @see ActionContext
     * @see ActionContext#builder()
     */
    class ActionContextDeserializer extends StdDeserializer<ActionContext> {

        public ActionContextDeserializer() {
            this(ActionContext.class);
        }

        protected ActionContextDeserializer(Class<ActionContext> vc) {
            super(vc);
        }

        @Override
        public ActionContext deserialize(final JsonParser jsonParser,
                                         final DeserializationContext deserializationContext) throws IOException {
            final ObjectMapper mapper = (ObjectMapper) jsonParser.getCodec();
            final TreeNode contextNodeTree = jsonParser.readValueAsTree();
            final String facadeName = restoreString(contextNodeTree.get("facadeName"));
            final String actionName = restoreString(contextNodeTree.get("actionName"));
            final Instant startedAt = restoreValue(contextNodeTree.get("startedAt"), mapper, Instant.class);
            final Duration lasts = restoreValue(contextNodeTree.get("lasts"), mapper, Duration.class);
            return ActionContext.builder().facadeName(facadeName).actionName(actionName)
                    .startedAt(startedAt).lasts(lasts).build();
        }

        private static String restoreString(final TreeNode propertyValueNode) throws IOException {
            if (propertyValueNode instanceof TextNode valueNode) {
                return valueNode.asText();
            } else {
                throw new IOException("Wrong node-type of propertyValueNode: " + propertyValueNode.getClass().getName());
            }
        }

        private static <T> T restoreValue(final TreeNode propertyValueNode, final ObjectMapper mapper, final Class<T> valueClass) throws IOException {
            final String propertyStringValue = propertyValueNode.toString();
            return mapper.readValue(mapper.getFactory().createParser(propertyStringValue), valueClass);
        }
    }
}
