package oleg.sopilnyak.test.service.command.executable.core.executor.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import oleg.sopilnyak.test.service.command.type.core.Context;
import oleg.sopilnyak.test.service.message.CommandMessage;

import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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

    MessagesExchange exchange;

    @BeforeEach
    void setUp() {
        exchange = spy(new FakeMessageExchange());
    }

    @Test
    void shouldMakeMessageInProgress() {
        String correlationId = "correlation-id-1";

        assertThat(exchange.makeMessageInProgress(correlationId, original)).isTrue();

        Map<String, MessageProgressWatchdog<?>> messages =
                (Map<String, MessageProgressWatchdog<?>>) ReflectionTestUtils.getField(exchange, "messageInProgress");
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
        Map<String, MessageProgressWatchdog<?>> messages =
                (Map<String, MessageProgressWatchdog<?>>) ReflectionTestUtils.getField(exchange, "messageInProgress");
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

        Optional<MessageProgressWatchdog<T>> optionalWatchdog = exchange.messageWatchdogFor(correlationId);

        assertThat(optionalWatchdog).isNotEmpty();
        assertThat(optionalWatchdog.orElseThrow().getState()).isEqualTo(MessageProgressWatchdog.State.IN_PROGRESS);
    }

    @Test
    <T> void shouldNotGetMessageWatchdogFor() {
        String correlationId = "correlation-id-5";

        Optional<MessageProgressWatchdog<T>> optionalWatchdog = exchange.messageWatchdogFor(correlationId);

        assertThat(optionalWatchdog).isEmpty();
    }

//    @Test
    void shouldExecuteWithActionContext() {
        // Init

        // Act
//        exchange.executeWithActionContext(original);

        // Verification
    }

    @Test
    void shouldActOnTakenRequestMessage() {
        String correlationId = "correlation-id-6";
        assertThat(exchange.makeMessageInProgress(correlationId, original)).isTrue();
        doReturn(correlationId).when(taken).getCorrelationId();
        doReturn(true).when(responsesProcessor).accept(taken);

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
        String correlationId = "correlation-id-62";
        assertThat(exchange.makeMessageInProgress(correlationId, original)).isTrue();
        doReturn(correlationId).when(taken).getCorrelationId();

        exchange.onTakenRequestMessage(taken);

        verify(exchange).messageWatchdogFor(correlationId);
        verify(exchange).localExecutionResult(taken);
        verify(responsesProcessor).accept(taken);
        verify(responsesProcessor).onTakenMessage(taken);
        verify(logger).error(anyString(), eq(correlationId));
    }

    @Test
    <T> void shouldActOnErrorRequestMessage_SendToResponses() {
        String correlationId = "correlation-id-7";
        doReturn(correlationId).when(taken).getCorrelationId();
        Context<T> context = mock(Context.class);
        doReturn(true).when(context).isFailed();
        doReturn(context).when(taken).getContext();
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
        Context<T> context = mock(Context.class);
        doReturn(context).when(taken).getContext();
        var exception = new RuntimeException("test");

        exchange.onErrorRequestMessage(taken, exception);

        verify(responsesProcessor, never()).accept(any(CommandMessage.class));
        verify(logger).error(anyString(), eq(context), eq(exception));
        verify(context).failed(exception);
    }

    @Test
    <T> void shouldActOnErrorRequestMessage_ContextIsFailed() {
        String correlationId = "correlation-id-71";
        Context<T> context = mock(Context.class);
        doReturn(true).when(context).isFailed();
        doReturn(correlationId).when(taken).getCorrelationId();
        doReturn(context).when(taken).getContext();
        doReturn(true).when(responsesProcessor).accept(taken);
        var exception = new RuntimeException("test");

        exchange.onErrorRequestMessage(taken, exception);

        verify(responsesProcessor).accept(taken);
        verify(responsesProcessor, never()).onTakenMessage(any(CommandMessage.class));
        verify(logger).error(anyString(), eq(correlationId));
        verify(context, never()).failed(any(Exception.class));
    }

    @Test
    <T> void shouldActOnErrorRequestMessage_ContextIsFailed_ResponseProcessorDoesNotAccept() {
        String correlationId = "correlation-id-71";
        Context<T> context = mock(Context.class);
        doReturn(true).when(context).isFailed();
        doReturn(correlationId).when(taken).getCorrelationId();
        doReturn(context).when(taken).getContext();
        var exception = new RuntimeException("test");

        exchange.onErrorRequestMessage(taken, exception);

        verify(responsesProcessor).accept(taken);
        verify(responsesProcessor).onTakenMessage(taken);
        verify(logger, times(2)).error(anyString(), eq(correlationId));
        verify(context, never()).failed(any(Exception.class));
    }

    @Test
    <T> void shouldActOnTakenResponseMessage() {
        String correlationId = "correlation-id-8";
        doReturn(correlationId).when(taken).getCorrelationId();
        Map<String, MessageProgressWatchdog<T>> messages =
                (Map<String, MessageProgressWatchdog<T>>) ReflectionTestUtils.getField(exchange, "messageInProgress");
        MessageProgressWatchdog<T> watchdog = (MessageProgressWatchdog<T>) spy(new MessageProgressWatchdog<>(original));
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
    void shouldFinalizeProcessedMessage_ResponsesAccepted() {
        String correlationId = "correlation-id-9";
        doReturn(true).when(responsesProcessor).accept(taken);

        ReflectionTestUtils.invokeMethod(exchange, "finalizeProcessedMessage", taken, correlationId);

        verify(responsesProcessor).accept(taken);
        verify(responsesProcessor, never()).onTakenMessage(any(CommandMessage.class));
        verify(logger).debug(anyString(), eq(correlationId));
    }

    @Test
    void shouldFinalizeProcessedMessage_ResponsesNotAccepted() {
        String correlationId = "correlation-id-91";

        ReflectionTestUtils.invokeMethod(exchange, "finalizeProcessedMessage", taken, correlationId);

        verify(responsesProcessor).accept(taken);
        verify(responsesProcessor).onTakenMessage(taken);
        verify(logger).error(anyString(), eq(correlationId));
        verify(responsesProcessor).onTakenMessage(taken);
    }

    // class implementation
    class FakeMessageExchange extends MessagesExchange {
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
        protected <T> CommandMessage<T> localExecutionResult(CommandMessage<T> commandMessage) {
            return commandMessage;
        }
    }
}