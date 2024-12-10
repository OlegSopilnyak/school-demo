package oleg.sopilnyak.test.service.message;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import oleg.sopilnyak.test.school.common.business.facade.ActionContext;
import oleg.sopilnyak.test.school.common.exception.core.CannotProcessActionException;
import oleg.sopilnyak.test.service.command.io.parameter.LongIdParameter;
import oleg.sopilnyak.test.service.command.io.result.BooleanResult;
import oleg.sopilnyak.test.service.command.type.base.Context;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.io.StringWriter;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.spy;

@ExtendWith(MockitoExtension.class)
class DoCommandMessageTest {
    private static final ObjectMapper objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
    private static final String COMMAND_ID = "command.id";
    private static final String CORRELATION_ID = "correlation-id";

    @Test
    void shouldStoreAndRestoreFailedMessage_SimpleException() throws JsonProcessingException {
        DoCommandMessage<Long, Boolean> message = new DoCommandMessage<>();
        String errorMessage = "Simple exception message";
        Exception ex = new Exception(errorMessage);
        ex.fillInStackTrace();
        message.setError(ex);

        String json = objectMapper.writeValueAsString(message);
        assertThat(json).isNotBlank();
        DoCommandMessage restored = objectMapper.readValue(json, DoCommandMessage.class);

        assertThat(restored).isInstanceOf(DoCommandMessage.class);
        assertThat(restored.getError()).isNotNull().isInstanceOf(Exception.class);
        assertThat(restored.getError().getMessage()).isNotBlank().isEqualTo(errorMessage);
    }

    @Test
    void shouldStoreAndRestoreFailedMessage_CannotProcessActionException() throws JsonProcessingException {
        DoCommandMessage<Long, Boolean> message = new DoCommandMessage<>();
        String errorMessage = "Simple exception message";
        IOException ex = new IOException(errorMessage);
        ex.fillInStackTrace();
        ActionContext.setup("test-facade", "test-action");
        CannotProcessActionException exception = new CannotProcessActionException(ex);
        String exceptionMessage = exception.getMessage();
        int stackTraceDepth = ex.getStackTrace().length;
        message.setError(exception);

        String json = objectMapper.writeValueAsString(message);
        assertThat(json).isNotBlank();
        DoCommandMessage restored = objectMapper.readValue(json, DoCommandMessage.class);

        assertThat(restored).isInstanceOf(DoCommandMessage.class);
        assertThat(restored.getError()).isNotNull().isInstanceOf(CannotProcessActionException.class);
        assertThat(restored.getError().getMessage()).isNotBlank().isEqualTo(exceptionMessage);
        assertThat(restored.getError().getStackTrace()).isNotNull().isNotEmpty().hasSize(stackTraceDepth);
        assertThat(restored.getError().getCause()).isNotNull().isInstanceOf(IOException.class);
        assertThat(restored.getError().getCause().getMessage()).isNotNull().isEqualTo(errorMessage);
        assertThat(restored.getError().getCause().getStackTrace()).isNotNull().isNotEmpty().hasSize(stackTraceDepth);
    }

    @Test
    void shouldSerializeExceptionUsingExceptionSerializer_SimpleException() throws IOException {
        CommandMessage.ExceptionSerializer<Exception> exceptionSerializer = new CommandMessage.ExceptionSerializer<>();
        String message = "Simple exception message";
        Exception ex = new Exception(message);
        ex.fillInStackTrace();
        int stackTraceDepth = ex.getStackTrace().length;
        StringWriter writer = new StringWriter();

        JsonGenerator generator = objectMapper.createGenerator(writer);
        exceptionSerializer.serialize(ex, generator, null);
        generator.close();

        String json = writer.toString();

        Map<String, Object> restored = objectMapper.readValue(json, Map.class);
        assertThat(restored).isNotNull().containsEntry("type", Exception.class.getName());
        Map<String, Object> value = (Map<String, Object>) restored.get("value");
        assertThat(value).isNotNull().containsEntry("message", message);
        List<Map<String, Object>> stackTrace = (List<Map<String, Object>>) value.get("stackTrace");
        assertThat(stackTrace).isNotNull().hasSize(stackTraceDepth);

        assertThat(value.get("cause")).isNull();
    }

    @Test
    void shouldSerializeExceptionUsingExceptionSerializer_CannotProcessActionException() throws IOException {
        CommandMessage.ExceptionSerializer<Exception> exceptionSerializer = new CommandMessage.ExceptionSerializer<>();
        String message = "Embedded IO exception message";
        IOException ex = new IOException(message);
        ex.fillInStackTrace();
        ActionContext.setup("test-facade", "test-action");
        CannotProcessActionException exception = new CannotProcessActionException(ex);
        String exceptionMessage = exception.getMessage();
        int stackTraceDepth = ex.getStackTrace().length;
        StringWriter writer = new StringWriter();

        JsonGenerator generator = objectMapper.createGenerator(writer);
        exceptionSerializer.serialize(exception, generator, null);
        generator.close();

        String json = writer.toString();

        Map<String, Object> restored = objectMapper.readValue(json, Map.class);
        assertThat(restored).isNotNull().containsEntry("type", CannotProcessActionException.class.getName());
        Map<String, Object> value = (Map<String, Object>) restored.get("value");
        assertThat(value).isNotNull().containsEntry("message", exceptionMessage);
        List<Map<String, Object>> stackTrace = (List<Map<String, Object>>) value.get("stackTrace");
        assertThat(stackTrace).isNotNull().hasSize(stackTraceDepth);

        Map<String, Object> restoredCause = (Map<String, Object>) value.get("cause");
        assertThat(restoredCause).isNotNull().containsEntry("type", IOException.class.getName());
        Map<String, Object> causeValue = (Map<String, Object>) restoredCause.get("value");
        assertThat(causeValue).isNotNull().containsEntry("message", message);
        List<Map<String, Object>> causeStackTrace = (List<Map<String, Object>>) causeValue.get("stackTrace");
        assertThat(causeStackTrace).isNotNull().hasSize(stackTraceDepth);
        assertThat(causeValue.get("cause")).isNull();
    }

    @Test
    void shouldDeserializeExceptionUsingExceptionDeserializer_SimpleException() throws IOException {
        CommandMessage.ExceptionSerializer<Exception> exceptionSerializer = new CommandMessage.ExceptionSerializer<>();
        String message = "Simple IO exception message";
        IOException ex = new IOException(message);
        ex.fillInStackTrace();
        int stackTraceDepth = ex.getStackTrace().length;
        StringWriter writer = new StringWriter();
        JsonGenerator generator = objectMapper.createGenerator(writer);
        exceptionSerializer.serialize(ex, generator, null);
        generator.close();
        String json = writer.toString();
        CommandMessage.ExceptionDeserializer deserializer = new CommandMessage.ExceptionDeserializer();
        JsonParser parser = objectMapper.getFactory().createParser(json);

        Throwable restored = deserializer.deserialize(parser, null);

        assertThat(restored).isNotNull().isInstanceOf(IOException.class);
        assertThat(restored.getMessage()).isEqualTo(message);
        assertThat(restored.getStackTrace()).isNotNull().hasSize(stackTraceDepth);
        assertThat(restored.getCause()).isNull();
    }

    @Test
    void shouldDeserializeExceptionUsingExceptionDeserializer_CannotProcessActionException() throws IOException {
        CommandMessage.ExceptionSerializer<Exception> exceptionSerializer = new CommandMessage.ExceptionSerializer<>();
        String message = "Embedded IO exception message";
        IOException ex = new IOException(message);
        ex.fillInStackTrace();
        int stackTraceDepth = ex.getStackTrace().length;
        StringWriter writer = new StringWriter();
        ActionContext.setup("test-facade", "test-action");
        CannotProcessActionException exception = new CannotProcessActionException(ex);
        String exceptionMessage = exception.getMessage();
        JsonGenerator generator = objectMapper.createGenerator(writer);
        exceptionSerializer.serialize(exception, generator, null);
        generator.close();
        String json = writer.toString();
        CommandMessage.ExceptionDeserializer deserializer = new CommandMessage.ExceptionDeserializer();
        JsonParser parser = objectMapper.getFactory().createParser(json);

        Throwable restored = deserializer.deserialize(parser, null);

        assertThat(restored).isNotNull().isInstanceOf(CannotProcessActionException.class);
        assertThat(restored.getMessage()).isEqualTo(exceptionMessage);
        assertThat(restored.getStackTrace()).isNotNull().hasSize(stackTraceDepth);
        assertThat(restored.getCause()).isNotNull().isInstanceOf(IOException.class);
        assertThat(restored.getCause().getMessage()).isEqualTo(message);
    }

    @Test
    void shouldCreateMessageLongBoolean() {
        String commandId = DoCommandMessageTest.COMMAND_ID;
        String correlationCommandId = DoCommandMessageTest.CORRELATION_ID;
        long id = 1;
        boolean value = true;
        Context.State state = Context.State.DONE;
        ActionContext actionContext = ActionContext.builder().actionName("test-action").facadeName("test-facade").build();
        Instant startedAt = Instant.now();
        Duration duration = Duration.ofMillis(ChronoUnit.MILLIS.between(startedAt, Instant.now().plus(1, ChronoUnit.SECONDS)));
        DoCommandMessage<Long, Boolean> message = spy(new DoCommandMessage<>());
        LongIdParameter input = new LongIdParameter(id);
        BooleanResult result = new BooleanResult(value);
        message.setCorrelationId(correlationCommandId);
        message.setCommandId(commandId);
        message.setActionContext(actionContext);
        message.setParameter(input);
        message.setResult(result);
        message.setResultState(state);
        message.setStartedAt(startedAt);
        message.setDuration(duration);

        assertThat(message).isNotNull();
        assertThat(message.getCorrelationId()).isSameAs(correlationCommandId);
        assertThat(message.getCommandId()).isSameAs(commandId);
        assertThat(message.getActionContext()).isSameAs(actionContext);
        assertThat(message.getParameter().value()).isSameAs(id);
        assertThat(message.getResult().value()).isSameAs(value);
        assertThat(message.getResultState()).isSameAs(state);
        assertThat(message.getStartedAt()).isSameAs(startedAt);
        assertThat(message.getDuration()).isSameAs(duration);
    }
}