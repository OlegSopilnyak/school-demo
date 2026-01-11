package oleg.sopilnyak.test.service.command.executable.core.executor.messaging.local;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import oleg.sopilnyak.test.service.command.executable.core.executor.messaging.MessagesProcessor;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class LocalQueueCommandExecutorTest {
    private static final String REQUESTS_PROCESSOR = "RequestMessagesProcessor";
    private static final String RESPONSES_PROCESSOR = "ResponseMessagesProcessor";
    @Mock
    ObjectMapper objectMapper;

    LocalQueueCommandExecutor executor;

    @BeforeEach
    void setUp() {
        executor = spy(new LocalQueueCommandExecutor());
        executor.setObjectMapper(objectMapper);
    }

    @Test
    void shouldPrepareRequestsProcessor() {
        // Init

        // Act
        MessagesProcessor processor = executor.prepareRequestsProcessor();

        // Verification
        assertThat(processor).isNotNull();
        assertThat(processor.getProcessorName()).isEqualTo(REQUESTS_PROCESSOR);
    }

    @Test
    void shouldPrepareResponsesProcessor() {
        // Init

        // Act
        MessagesProcessor processor = executor.prepareResponsesProcessor();

        // Verification
        assertThat(processor).isNotNull();
        assertThat(processor.getProcessorName()).isEqualTo(RESPONSES_PROCESSOR);
    }

    @Test
    void shouldInitializeTakenMessagesExecutor() throws ExecutionException, InterruptedException, JsonProcessingException {
        // Init

        // Act
        executor.initializeTakenMessagesExecutor();

        // Verification
        ExecutorService takenMessagesExecutor = (ExecutorService) ReflectionTestUtils.getField(executor, "executor");
        assertThat(takenMessagesExecutor).isNotNull();
        assertThat(takenMessagesExecutor.isShutdown()).isFalse();
        // test executor functionality
        Runnable serializeRunnable = () -> {
            try {
                objectMapper.writeValueAsString(executor);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        };
        takenMessagesExecutor.submit(serializeRunnable).get();
        verify(objectMapper).writeValueAsString(executor);
    }

    @Test
    void shouldShutdownTakenMessagesExecutor() {
        // Init
        ExecutorService takenMessagesExecutor = mock(ExecutorService.class);
        ReflectionTestUtils.setField(executor, "executor", takenMessagesExecutor);

        // Act
        executor.shutdownTakenMessagesExecutor();

        // Verification
        verify(takenMessagesExecutor).shutdown();
        verify(takenMessagesExecutor).shutdownNow();
        assertThat(ReflectionTestUtils.getField(executor, "executor")).isNull();
    }
}