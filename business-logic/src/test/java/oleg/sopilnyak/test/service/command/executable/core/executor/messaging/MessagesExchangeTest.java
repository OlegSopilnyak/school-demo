package oleg.sopilnyak.test.service.command.executable.core.executor.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import oleg.sopilnyak.test.school.common.business.facade.ActionContext;
import oleg.sopilnyak.test.service.command.type.core.Context;
import oleg.sopilnyak.test.service.command.type.core.RootCommand;
import oleg.sopilnyak.test.service.message.CommandMessage;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unchecked")
class MessagesExchangeTest {
    @Mock
    CommandMessage<?> original;
    @Mock
    CommandMessage<?> taken;
    @Mock
    MessagesProcessor responsesProcessor;
    @Mock
    Logger logger;
    @Mock
    Context context;
    @Mock
    RootCommand command;

    MessagesExchange exchange;

    @BeforeEach
    void setUp() {
        exchange = spy(new FakeMessageExchange());
    }

    @Test
    void shouldMakeMessageInProgress() {
        String correlationId = "correlation-id-1";

        assertThat(exchange.makeMessageInProgress(correlationId, original)).isTrue();

        Map<String, CommandMessageWatchdog<?>> messages =
                (Map<String, CommandMessageWatchdog<?>>) ReflectionTestUtils.getField(exchange, "messages");
        assertThat(messages).isNotNull();
        assertThat(messages.get(correlationId)).isNotNull();
        assertThat(messages.get(correlationId).getResult()).isNull();
    }

    @Test
    void shouldNotMakeMessageInProgress_CorrelationId_Duplication() {
        String correlationId = "correlation-id-2";

        assertThat(exchange.makeMessageInProgress(correlationId, original)).isTrue();
        assertThat(exchange.makeMessageInProgress(correlationId, original)).isFalse();
    }

    @Test
    void shouldStopWatchingMessage() {
        String correlationId = "correlation-id-3";
        assertThat(exchange.makeMessageInProgress(correlationId, original)).isTrue();
        Map<String, CommandMessageWatchdog<?>> messages =
                (Map<String, CommandMessageWatchdog<?>>) ReflectionTestUtils.getField(exchange, "messages");
        assertThat(messages).isNotNull();
        assertThat(messages.get(correlationId)).isNotNull();
        assertThat(messages.get(correlationId).getResult()).isNull();

        exchange.stopWatchingMessage(correlationId);

        assertThat(messages.get(correlationId)).isNull();
    }

    @Test
    <T> void shouldGetMessageWatchdogFor() {
        String correlationId = "correlation-id-4";
        assertThat(exchange.makeMessageInProgress(correlationId, original)).isTrue();

        Optional<CommandMessageWatchdog<T>> optionalWatchdog = exchange.messageWatchdogFor(correlationId);

        assertThat(optionalWatchdog).isNotEmpty();
        assertThat(optionalWatchdog.orElseThrow().getState()).isEqualTo(CommandMessageWatchdog.State.IN_PROGRESS);
    }

    @Test
    <T> void shouldNotGetMessageWatchdogFor() {
        String correlationId = "correlation-id-5";

        Optional<CommandMessageWatchdog<T>> optionalWatchdog = exchange.messageWatchdogFor(correlationId);

        assertThat(optionalWatchdog).isEmpty();
    }

    @Test
    void shouldExecuteWithActionContext() {
        // Init
        ActionContext actionContext = ActionContext.setup("test-facade", "test-action");
        ActionContext.release();
        String commandId = "command-id-50";
        String correlationId = "correlation-id-50";
        doReturn(actionContext).when(original).getActionContext();
        doReturn(correlationId).when(original).getCorrelationId();
        doReturn(CommandMessage.Direction.DO).when(original).getDirection();
        doReturn(context).when(original).getContext();
        doReturn(command).when(context).getCommand();
        doReturn(commandId).when(command).getId();
        assertThat(exchange.makeMessageInProgress(correlationId, original)).isTrue();

        // Act
        exchange.executeWithActionContext(original);

        // Verification
        verify(exchange).onTakenRequestMessage(original);
        verify(exchange).messageWatchdogFor(correlationId);
        verify(exchange).localExecutionResult(original);
        verify(command).doCommand(context);
        verify(logger).debug("++ Successfully processed request with direction:{} correlation-id:{}",
                CommandMessage.Direction.DO, correlationId);
        verify(responsesProcessor).accept(original);
        verify(exchange, never()).onErrorRequestMessage(any(CommandMessage.class), any(Throwable.class));
    }

    @Test
    void shouldNotExecuteWithActionContext_NoActionContext() {
        // Init
        String correlationId = "correlation-id-51";
        doReturn(correlationId).when(original).getCorrelationId();
        doReturn(true).when(context).isFailed();
        doReturn(context).when(original).getContext();

        // Act
        exchange.executeWithActionContext(original);

        // Verification
        verify(exchange, never()).onTakenRequestMessage(any(CommandMessage.class));
        ArgumentCaptor<Exception> captor = ArgumentCaptor.forClass(Exception.class);
        verify(exchange).onErrorRequestMessage(eq(original), captor.capture());
        assertThat(captor.getValue()).isNotNull().isInstanceOf(IllegalArgumentException.class);
        assertThat(captor.getValue().getMessage()).isEqualTo("Action context must not be null");
        verify(responsesProcessor).accept(original);
    }

    @Test
    void shouldNotExecuteWithActionContext_NoMessageInProgress() {
        // Init
        ActionContext actionContext = ActionContext.setup("test-facade", "test-action");
        ActionContext.release();
        String correlationId = "correlation-id-52";
        doReturn(actionContext).when(original).getActionContext();
        doReturn(correlationId).when(original).getCorrelationId();
        doReturn(CommandMessage.Direction.DO).when(original).getDirection();

        // Act
        exchange.executeWithActionContext(original);

        // Verification
        verify(exchange).onTakenRequestMessage(original);
        verify(logger).warn("= Message with correlationId='{}' is NOT found in progress map", correlationId);
        verify(exchange, never()).onErrorRequestMessage(any(CommandMessage.class), any(Throwable.class));
    }

    @Test
    void shouldNotExecuteWithActionContext_CommandDoThrows() {
        // Init
        ActionContext actionContext = ActionContext.setup("test-facade", "test-action");
        ActionContext.release();
        String commandId = "command-id-53";
        String correlationId = "correlation-id-53";
        doReturn(actionContext).when(original).getActionContext();
        doReturn(correlationId).when(original).getCorrelationId();
        doReturn(CommandMessage.Direction.DO).when(original).getDirection();
        doReturn(context).when(original).getContext();
        doReturn(command).when(context).getCommand();
        doReturn(commandId).when(command).getId();
        doReturn(command).when(command).self();
        doCallRealMethod().when(command).doCommand(context);
        doReturn(true).when(context).isReady();
        doReturn(true).when(context).isFailed();
        assertThat(exchange.makeMessageInProgress(correlationId, original)).isTrue();
        Exception doCommandException = new RuntimeException("executeDo throws");
        doThrow(doCommandException).when(command).executeDo(context);

        // Act
        exchange.executeWithActionContext(original);

        // Verification
        verify(exchange).onTakenRequestMessage(original);
        verify(exchange).messageWatchdogFor(correlationId);
        verify(exchange).localExecutionResult(original);
        verify(command).doCommand(context);
        verify(command).executeDo(context);
        verify(logger).error("== Couldn't process message request with correlation-id:{}", correlationId, doCommandException);
        verify(exchange).onErrorRequestMessage(original, doCommandException);
        verify(responsesProcessor).accept(original);
        verify(logger).error("Result: message with correlationId='{}' is processed but is NOT sent to responses processor", correlationId);
        responsesProcessor.onTakenMessage(original);
    }

    @Test
    void shouldActOnTakenRequestMessage() {
        String commandId = "command-id-6";
        String correlationId = "correlation-id-6";
        doReturn(CommandMessage.Direction.DO).when(taken).getDirection();
        doReturn(correlationId).when(taken).getCorrelationId();
        doReturn(context).when(taken).getContext();
        doReturn(command).when(context).getCommand();
        doReturn(commandId).when(command).getId();
        doReturn(true).when(responsesProcessor).accept(taken);
        assertThat(exchange.makeMessageInProgress(correlationId, taken)).isTrue();

        exchange.onTakenRequestMessage(taken);

        verify(exchange).messageWatchdogFor(correlationId);
        verify(exchange).localExecutionResult(taken);
        verify(responsesProcessor).accept(taken);
        verify(responsesProcessor, never()).onTakenMessage(any(CommandMessage.class));
        verify(logger, times(3)).debug(anyString(), eq(correlationId));
    }

    @Test
    void shouldNotActOnTakenRequestMessage_NoMessageInProgress() {
        String correlationId = "correlation-id-61";
        doReturn(correlationId).when(taken).getCorrelationId();

        exchange.onTakenRequestMessage(taken);

        verify(exchange).messageWatchdogFor(correlationId);
        verify(exchange, never()).localExecutionResult(any(CommandMessage.class));
        verify(logger).warn(anyString(), eq(correlationId));
    }

    @Test
    void shouldNotActOnTakenRequestMessage_ResponseProcessorDoesNotAccept() {
        String commandId = "command-id-62";
        String correlationId = "correlation-id-62";
        doReturn(CommandMessage.Direction.DO).when(taken).getDirection();
        doReturn(correlationId).when(taken).getCorrelationId();
        doReturn(context).when(taken).getContext();
        doReturn(command).when(context).getCommand();
        doReturn(commandId).when(command).getId();
        assertThat(exchange.makeMessageInProgress(correlationId, taken)).isTrue();

        exchange.onTakenRequestMessage(taken);

        verify(exchange).messageWatchdogFor(correlationId);
        verify(exchange).localExecutionResult(taken);
        verify(command).doCommand(context);
        verify(responsesProcessor).accept(taken);
        verify(responsesProcessor).onTakenMessage(taken);
        verify(logger).error(anyString(), eq(correlationId));
    }

    @Test
    <T> void shouldActOnErrorRequestMessage_SendToResponses() {
        String correlationId = "correlation-id-7";
        doReturn(correlationId).when(taken).getCorrelationId();
        Context<T> mockedContext = mock(Context.class);
        doReturn(true).when(mockedContext).isFailed();
        doReturn(mockedContext).when(taken).getContext();
        var exception = new RuntimeException("test");
        doReturn(true).when(responsesProcessor).accept(taken);

        exchange.onErrorRequestMessage(taken, exception);

        verify(responsesProcessor).accept(taken);
        verify(responsesProcessor, never()).onTakenMessage(any(CommandMessage.class));
        verify(logger).debug(anyString(), eq(correlationId));
        verify(logger).error(anyString(), eq(correlationId));
    }

    @Test
    <T> void shouldActOnErrorRequestMessage_ContextNotFailed() {
        Context<T> mockedContext = mock(Context.class);
        doReturn(mockedContext).when(taken).getContext();
        var exception = new RuntimeException("test");

        exchange.onErrorRequestMessage(taken, exception);

        verify(responsesProcessor, never()).accept(any(CommandMessage.class));
        verify(logger).error(anyString(), eq(mockedContext), eq(exception));
        verify(mockedContext).failed(exception);
    }

    @Test
    <T> void shouldActOnErrorRequestMessage_ContextIsFailed() {
        String correlationId = "correlation-id-71";
        Context<T> mockedContext = mock(Context.class);
        doReturn(true).when(mockedContext).isFailed();
        doReturn(correlationId).when(taken).getCorrelationId();
        doReturn(mockedContext).when(taken).getContext();
        doReturn(true).when(responsesProcessor).accept(taken);
        var exception = new RuntimeException("test");

        exchange.onErrorRequestMessage(taken, exception);

        verify(responsesProcessor).accept(taken);
        verify(responsesProcessor, never()).onTakenMessage(any(CommandMessage.class));
        verify(logger).error(anyString(), eq(correlationId));
        verify(mockedContext, never()).failed(any(Exception.class));
    }

    @Test
    <T> void shouldActOnErrorRequestMessage_ContextIsFailed_ResponseProcessorDoesNotAccept() {
        String correlationId = "correlation-id-71";
        Context<T> mockedContext = mock(Context.class);
        doReturn(true).when(mockedContext).isFailed();
        doReturn(correlationId).when(taken).getCorrelationId();
        doReturn(mockedContext).when(taken).getContext();
        var exception = new RuntimeException("test");

        exchange.onErrorRequestMessage(taken, exception);

        verify(responsesProcessor).accept(taken);
        verify(responsesProcessor).onTakenMessage(taken);
        verify(logger, times(2)).error(anyString(), eq(correlationId));
        verify(mockedContext, never()).failed(any(Exception.class));
    }

    @Test
    <T> void shouldActOnTakenResponseMessage() {
        String correlationId = "correlation-id-8";
        doReturn(correlationId).when(taken).getCorrelationId();
        Map<String, CommandMessageWatchdog<T>> messages =
                (Map<String, CommandMessageWatchdog<T>>) ReflectionTestUtils.getField(exchange, "messages");
        CommandMessageWatchdog<T> watchdog = (CommandMessageWatchdog<T>) spy(TestCommandMessageWatchdog.builder().original(original).build());
        assertThat(messages).isNotNull();
        messages.put(correlationId, watchdog);

        exchange.onTakenResponseMessage(taken);

        verify(logger, times(2)).info(anyString(), eq(correlationId));
        verify(exchange).messageWatchdogFor(correlationId);
        verify(watchdog).setResult((CommandMessage<T>) taken);
        verify(watchdog).messageProcessingIsDone();
    }

    @Test
    void shouldActOnTakenResponseMessage_NoMessageInProgress() {
        String correlationId = "correlation-id-81";
        doReturn(correlationId).when(taken).getCorrelationId();

        exchange.onTakenResponseMessage(taken);

        verify(logger).info(anyString(), eq(correlationId));
        verify(exchange).messageWatchdogFor(correlationId);
        verify(logger).warn(anyString(), eq(correlationId));
    }

    @Test
    void shouldPassProcessedMessage_Out_ResponsesAccepted() {
        String correlationId = "correlation-id-9";
        doReturn(true).when(responsesProcessor).accept(taken);

        ReflectionTestUtils.invokeMethod(exchange, "passProcessedMessageOut", taken, correlationId);

        verify(responsesProcessor).accept(taken);
        verify(responsesProcessor, never()).onTakenMessage(any(CommandMessage.class));
        verify(logger).debug(anyString(), eq(correlationId));
    }

    @Test
    void shouldPassProcessedMessage_Out_ResponsesNotAccepted() {
        String correlationId = "correlation-id-91";

        ReflectionTestUtils.invokeMethod(exchange, "passProcessedMessageOut", taken, correlationId);

        verify(responsesProcessor).accept(taken);
        verify(responsesProcessor).onTakenMessage(taken);
        verify(logger).error(anyString(), eq(correlationId));
        verify(responsesProcessor).onTakenMessage(taken);
    }

    // class implementation
    class FakeMessageExchange extends MessagesExchange {
        Map<String, CommandMessageWatchdog<?>> messages = new HashMap<>();

        @Override
        public boolean isActive() {
            return false;
        }

        @Override
        public MessagesProcessor getResponsesProcessor() {
            return responsesProcessor;
        }

        @Override
        protected Logger getLogger() {
            return logger;
        }

        @Override
        protected boolean makeMessageInProgress(String correlationId, CommandMessage<?> original) {
            return messages.putIfAbsent(correlationId, TestCommandMessageWatchdog.builder().original(original).build()) == null;
        }

        @Override
        protected <T> Optional<CommandMessageWatchdog<T>> messageWatchdogFor(String correlationId) {
            return Optional.ofNullable((CommandMessageWatchdog<T>) messages.get(correlationId));
        }

        @Override
        protected void stopWatchingMessage(String correlationId) {
            messages.remove(correlationId);
        }
    }
}