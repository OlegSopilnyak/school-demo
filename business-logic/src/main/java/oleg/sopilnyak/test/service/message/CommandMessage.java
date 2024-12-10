package oleg.sopilnyak.test.service.message;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.node.TextNode;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import oleg.sopilnyak.test.school.common.business.facade.ActionContext;
import oleg.sopilnyak.test.service.command.io.Input;
import oleg.sopilnyak.test.service.command.io.Output;
import oleg.sopilnyak.test.service.command.type.base.Context;

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.time.Duration;
import java.time.Instant;

import static java.util.Objects.nonNull;
import static oleg.sopilnyak.test.service.command.io.IOFieldNames.TYPE_FIELD_NAME;
import static oleg.sopilnyak.test.service.command.io.IOFieldNames.VALUE_FIELD_NAME;

public interface CommandMessage<I, O> extends Serializable {
    String EXCEPTION_MESSAGE = "message";
    String EXCEPTION_CAUSE = "cause";
    String EXCEPTION_STACK_TRACE = "stackTrace";

    /**
     * correlation ID of the message
     *
     * @return the value
     */
    String getCorrelationId();

    /**
     * the ID of command to execute
     *
     * @return the value
     * @see oleg.sopilnyak.test.service.command.factory.base.CommandsFactory
     */
    String getCommandId();

    /**
     * the direction of command's execution
     *
     * @return type of the command's message
     * @see Direction
     */
    Direction getDirection();

    /**
     * the context of command's execution
     *
     * @return the value
     */
    ActionContext getActionContext();

    /**
     * the command's input/undo parameters
     *
     * @return the command's input value
     * @see Input#value()
     */
    Input<I> getParameter();

    /**
     * the result of command's do execution
     *
     * @return command do execution's result value
     * @see Output
     */
    Output<O> getResult();

    /**
     * the error instance when something went wrong
     *
     * @return the value or null if there was no error
     * @see Exception
     * @see Context.State#FAIL
     */
    Exception getError();

    /**
     * the state after command execution
     *
     * @return the value
     * @see Context.State
     */
    Context.State getResultState();

    /**
     * the time when command's execution is started
     *
     * @return the value
     * @see Instant
     */
    Instant getStartedAt();

    /**
     * the value of command execution duration
     *
     * @return the value
     * @see Duration
     */
    Duration getDuration();

    /**
     * Enumeration of command execution direction whether it DO or UNDO of command's execution
     */
    enum Direction {
        DO, UNDO
    }

    // serializer/deserializer for message's fields

    /**
     * JSON: Serializer for Throwable
     *
     * @see StdSerializer
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

        private void serializeExceptionStuff(final T exception,
                                             final JsonGenerator generator,
                                             final SerializerProvider serializerProvider) throws IOException {
            // store exception body
            generator.writeStartObject();
            generator.writeStringField(EXCEPTION_MESSAGE, exception.getMessage());
            final Throwable cause = exception.getCause();
            if (nonNull(cause) && cause != exception) {// store exception's cause
                generator.writeFieldName(EXCEPTION_CAUSE);
                // recursive call for exception's cause
                serialize((T) cause, generator, serializerProvider);
            }
            generator.writeFieldName(EXCEPTION_STACK_TRACE);
            generator.writeRawValue(((ObjectMapper) generator.getCodec()).writeValueAsString(exception.getStackTrace()));
            generator.writeEndObject();
        }
    }

    /**
     * JSON: Deserializer for Throwable
     *
     * @see StdDeserializer
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
        public Throwable deserialize(final JsonParser jsonParser, final DeserializationContext deserializationContext)
                throws IOException {
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
            final TreeNode causeNode = valueNode.get(EXCEPTION_CAUSE);
            return causeNode == null ?
                    // no cause in value-node
                    addRestoredStackTraceFor(
                            createExceptionInstance(exceptionClass, exceptionMessage),
                            (ObjectMapper) jsonParser.getCodec(), valueNode.get(EXCEPTION_STACK_TRACE)
                    ) :
                    // restore cause and make exception instance
                    addRestoredStackTraceFor(
                            createExceptionInstance(exceptionClass, exceptionMessage,
                                    // recursion
                                    restoreExceptionBy(causeNode, jsonParser)
                            ),
                            (ObjectMapper) jsonParser.getCodec(), valueNode.get(EXCEPTION_STACK_TRACE)
                    );
        }

        private static String getExceptionMessage(final TreeNode valueNode) throws IOException {
            final TreeNode messageNode = valueNode.get(EXCEPTION_MESSAGE);
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
}

