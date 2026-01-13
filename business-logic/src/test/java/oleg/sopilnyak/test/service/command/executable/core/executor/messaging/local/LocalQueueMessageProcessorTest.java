package oleg.sopilnyak.test.service.command.executable.core.executor.messaging.local;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import oleg.sopilnyak.test.service.command.executable.core.executor.messaging.MessagesExchange;
import oleg.sopilnyak.test.service.command.type.core.Context;
import oleg.sopilnyak.test.service.message.BaseCommandMessage;
import oleg.sopilnyak.test.service.message.CommandMessage;

import java.util.concurrent.BlockingQueue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;
import org.slf4j.Logger;
import org.springframework.test.util.ReflectionTestUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.experimental.SuperBuilder;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unchecked")
class LocalQueueMessageProcessorTest {
    @Mock
    Logger logger;
    @Mock
    ObjectMapper objectMapper;
    @Mock
    CommandMessage<?> message;
    @Mock
    Context context;
    @Mock
    MessagesExchange  messagesExchange;

    LocalQueueMessageProcessor processor;

    @BeforeEach
    void setUp() {
        processor = spy(ConcreteMessageProcessor.builder()
                .exchange(messagesExchange).logger(logger).objectMapper(objectMapper).build());
    }

    @Test
    void shouldRunAsyncTakenMessage() {
        // Init
        doAnswer((Answer<Void>) invocationOnMock -> {
            invocationOnMock.getArgument(0, Runnable.class).run();
            return null;
        }).when(messagesExchange).runAsync(any(Runnable.class));

        // Act
        processor.runAsyncTakenMessage(CommandMessage::getContext, message);

        // Verification
        verify(messagesExchange).runAsync(any(Runnable.class));
        verify(message).getContext();
    }

    @Test
    void shouldTakeMessage() throws InterruptedException, JsonProcessingException {
        // Init
        BlockingQueue<String> messages = (BlockingQueue<String>) ReflectionTestUtils.getField(processor, "messages");
        String messageJson = "test-message";
        assertThat(messages).isNotNull();
        assertThat(messages.offer(messageJson)).isTrue();
        doReturn(message).when(objectMapper).readValue(messageJson, BaseCommandMessage.class);

        // Act
        CommandMessage<?> taken = processor.takeMessage();

        // Verification
        verify(logger).debug("Taking available command message from the queue.");
        verify(objectMapper).readValue(messageJson, BaseCommandMessage.class);
        verify(logger).debug("Took from the queue command message {}", messageJson);
        assertThat(taken).isSameAs(message);
    }

    @Test
    void shouldNotTakeMessage_DeserializeThrowsRuntimeException() throws InterruptedException, JsonProcessingException {
        // Init
        BlockingQueue<String> messages = (BlockingQueue<String>) ReflectionTestUtils.getField(processor, "messages");
        String messageJson = "test-message";
        assertThat(messages).isNotNull();
        assertThat(messages.offer(messageJson)).isTrue();
        Exception exception = new RuntimeException("Something went wrong during deserialization of the command-message");
        doThrow(exception).when(objectMapper).readValue(messageJson, BaseCommandMessage.class);

        // Act
        CommandMessage<?> taken = processor.takeMessage();

        // Verification
        verify(logger).debug("Taking available command message from the queue.");
        verify(objectMapper).readValue(messageJson, BaseCommandMessage.class);
        verify(logger).debug("Took from the queue command message {}", messageJson);
        verify(logger).error("Something went wrong during deserialization of the command-message", exception);
        assertThat(taken).isSameAs(CommandMessage.EMPTY);
    }

    @Test
    void shouldNotTakeMessage_DeserializeThrowsJsonProcessingException() throws InterruptedException, JsonProcessingException {
        // Init
        BlockingQueue<String> messages = (BlockingQueue<String>) ReflectionTestUtils.getField(processor, "messages");
        String messageJson = "test-message";
        assertThat(messages).isNotNull();
        assertThat(messages.offer(messageJson)).isTrue();
        Exception exception = new JsonMappingException("Something went wrong during deserialization of the command-message");
        doThrow(exception).when(objectMapper).readValue(messageJson, BaseCommandMessage.class);

        // Act
        CommandMessage<?> taken = processor.takeMessage();

        // Verification
        verify(logger).debug("Taking available command message from the queue.");
        verify(objectMapper).readValue(messageJson, BaseCommandMessage.class);
        verify(logger).debug("Took from the queue command message {}", messageJson);
        verify(logger).error("Failed deserialization of command-message", exception);
        assertThat(taken).isSameAs(CommandMessage.EMPTY);
    }

    @Test
    void shouldBeEmpty() {

        // Act
        boolean empty = processor.isEmpty();

        // Verification
        assertThat(empty).isTrue();
    }

    @Test
    void shouldNotBeEmpty() {
        // Init
        BlockingQueue<String> messages = (BlockingQueue<String>) ReflectionTestUtils.getField(processor, "messages");
        assertThat(messages).isNotNull();
        assertThat(messages.offer("test-message")).isTrue();

        // Act
        boolean empty = processor.isEmpty();

        // Verification
        assertThat(empty).isFalse();
    }

    @Test
    void shouldAccept() throws JsonProcessingException, InterruptedException {
        // Init
        BlockingQueue<String> messages = (BlockingQueue<String>) ReflectionTestUtils.getField(processor, "messages");
        assertThat(messages).isNotNull();
        String messageJson = "test-message";
        doReturn(messageJson).when(objectMapper).writeValueAsString(message);

        // Act
        boolean accepted = processor.accept(message);

        // Verification
        verify(logger).debug("Put to the queue command message {}", messageJson);
        assertThat(accepted).isTrue();
        assertThat(messages.take()).isEqualTo(messageJson);
    }

    @Test
    void shouldNotAccept() throws JsonProcessingException {
        // Init
        BlockingQueue<String> messages = (BlockingQueue<String>) ReflectionTestUtils.getField(processor, "messages");
        assertThat(messages).isNotNull();
        doReturn(context).when(message).getContext();
        Exception exception = new JsonMappingException("Something went wrong during serialization of the command-message");
        doThrow(exception).when(objectMapper).writeValueAsString(message);

        // Act
        boolean accepted = processor.accept(message);

        // Verification
        verify(logger).warn("Failed to serialize message to json", exception);
        assertThat(accepted).isFalse();
        assertThat(messages).isEmpty();
    }

    @SuperBuilder
    private static class ConcreteMessageProcessor extends LocalQueueMessageProcessor {
    }
}