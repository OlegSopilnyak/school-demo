package oleg.sopilnyak.test.service.message;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import oleg.sopilnyak.test.school.common.business.facade.ActionContext;
import oleg.sopilnyak.test.school.common.exception.core.CannotProcessActionException;
import oleg.sopilnyak.test.service.command.io.parameter.LongIdParameter;
import oleg.sopilnyak.test.service.command.io.result.BooleanResult;
import oleg.sopilnyak.test.service.command.type.base.Context;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.spy;

@ExtendWith(MockitoExtension.class)
class DoCommandMessageTest {
    private static final ObjectMapper objectMapper = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT)
            .registerModule(new JavaTimeModule());
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
        DoCommandMessage<Long, Boolean> restored = objectMapper.readValue(json, DoCommandMessage.class);

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
        DoCommandMessage<Long, Boolean> restored = objectMapper.readValue(json, DoCommandMessage.class);

        assertThat(restored).isInstanceOf(DoCommandMessage.class);
        assertThat(restored.getError()).isNotNull().isInstanceOf(CannotProcessActionException.class);
        assertThat(restored.getError().getMessage()).isNotBlank().isEqualTo(exceptionMessage);
        assertThat(restored.getError().getStackTrace()).isNotNull().isNotEmpty().hasSize(stackTraceDepth);
        assertThat(restored.getError().getCause()).isNotNull().isInstanceOf(IOException.class);
        assertThat(restored.getError().getCause().getMessage()).isNotNull().isEqualTo(errorMessage);
        assertThat(restored.getError().getCause().getStackTrace()).isNotNull().isNotEmpty().hasSize(stackTraceDepth);
    }

    @Test
    void shouldRestoreDoCommandMessageWithFail_SimpleException() throws IOException {
        DoCommandMessage<Long, Boolean> message = new DoCommandMessage<>();
        String exceptionMessage = "Simple exception message";
        Exception ex = new Exception(exceptionMessage);
        ex.fillInStackTrace();
        int stackTraceDepth = ex.getStackTrace().length;
        message.setError(ex);

        String json = objectMapper.writeValueAsString(message);
        assertThat(json).isNotNull().isNotBlank().contains(exceptionMessage);
        DoCommandMessage<Long, Boolean> restored = objectMapper.readValue(json, DoCommandMessage.class);

        assertThat(restored).isNotNull().isInstanceOf(DoCommandMessage.class);
        assertThat(restored.getError()).isNotNull().isInstanceOf(Exception.class);
        assertThat(restored.getError().getMessage()).isNotNull().isEqualTo(exceptionMessage);
        assertThat(restored.getError().getStackTrace()).isNotNull().hasSize(stackTraceDepth);
    }

    @Test
    void shouldRestoreDoCommandMessageWithFail_CannotProcessActionException() throws IOException {
        DoCommandMessage<Long, Boolean> commandMessage = new DoCommandMessage<>();
        String message = "Embedded IO exception message";
        IOException ex = new IOException(message);
        ex.fillInStackTrace();
        ActionContext.setup("test-facade", "test-action");
        CannotProcessActionException exception = new CannotProcessActionException(ex);
        String exceptionMessage = exception.getMessage();
        int stackTraceDepth = ex.getStackTrace().length;
        commandMessage.setError(exception);

        String json = objectMapper.writeValueAsString(commandMessage);
        assertThat(json).isNotNull().isNotBlank().contains(message);
        DoCommandMessage<Long, Boolean> restored = objectMapper.readValue(json, DoCommandMessage.class);

        assertThat(restored).isNotNull().isInstanceOf(DoCommandMessage.class);
        assertThat(restored.getError()).isNotNull().isInstanceOf(CannotProcessActionException.class);
        assertThat(restored.getError().getMessage()).isNotNull().isEqualTo(exceptionMessage);
        assertThat(restored.getError().getStackTrace()).isNotNull().hasSize(stackTraceDepth);
        assertThat(restored.getError().getCause()).isNotNull().isInstanceOf(IOException.class);
        assertThat(restored.getError().getCause().getMessage()).isEqualTo(message);
        assertThat(restored.getError().getCause().getStackTrace()).isNotNull().hasSize(stackTraceDepth);
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

    @Test
    void shouldStoreCommandMessageLongBoolean() throws JsonProcessingException {
        String commandId = DoCommandMessageTest.COMMAND_ID;
        String correlationCommandId = DoCommandMessageTest.CORRELATION_ID;
        long id = 2;
        boolean resultValue = true;
        Context.State state = Context.State.DONE;
        ActionContext actionContext = ActionContext.builder().actionName("test-action").facadeName("test-facade").build();
        Instant startedAt = Instant.now();
        Duration duration = Duration.ofMillis(ChronoUnit.MILLIS.between(startedAt, Instant.now().plus(100, ChronoUnit.MILLIS)));
        DoCommandMessage<Long, Boolean> message = new DoCommandMessage<>();
        LongIdParameter input = new LongIdParameter(id);
        BooleanResult result = new BooleanResult(resultValue);
        message.setCorrelationId(correlationCommandId);
        message.setCommandId(commandId);
        message.setActionContext(actionContext);
        message.setParameter(input);
        message.setResult(result);
        message.setResultState(state);
        message.setStartedAt(startedAt);
        message.setDuration(duration);

        String json = objectMapper.writeValueAsString(message);
        assertThat(json).isNotBlank()
                .contains("test-action")
                .contains("test-action")
                .contains(LongIdParameter.class.getName())
                .contains(BooleanResult.class.getName());

    }

    @Test
    void shouldRestoreCommandMessageLongBoolean() throws JsonProcessingException {
        String commandId = DoCommandMessageTest.COMMAND_ID;
        String correlationCommandId = DoCommandMessageTest.CORRELATION_ID;
        long id = 3;
        boolean resultValue = false;
        Context.State state = Context.State.DONE;
        ActionContext actionContext = ActionContext.builder().actionName("test-action").facadeName("test-facade").build();
        Instant startedAt = Instant.now();
        Duration duration = Duration.ofMillis(ChronoUnit.MILLIS.between(startedAt, Instant.now().plus(100, ChronoUnit.MILLIS)));
        DoCommandMessage<Long, Boolean> message = new DoCommandMessage<>();
        LongIdParameter input = new LongIdParameter(id);
        BooleanResult result = new BooleanResult(resultValue);
        message.setCorrelationId(correlationCommandId);
        message.setCommandId(commandId);
        message.setActionContext(actionContext);
        message.setParameter(input);
        message.setResult(result);
        message.setResultState(state);
        message.setStartedAt(startedAt);
        message.setDuration(duration);
        String json = objectMapper.writeValueAsString(message);

        DoCommandMessage<Long, Boolean> restoredMessage = objectMapper.readValue(json, DoCommandMessage.class);

        assertThat(restoredMessage).isNotNull().isEqualTo(message);
    }
}