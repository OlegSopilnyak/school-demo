package oleg.sopilnyak.test.service.command.executable.core.executor.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import oleg.sopilnyak.test.service.message.CommandMessage;

import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.springframework.test.util.ReflectionTestUtils;
import lombok.experimental.SuperBuilder;

@ExtendWith(MockitoExtension.class)
class RootMessageProcessorTest {
    static String processorName = "FakeRootMessageProcessor";
    @Mock
    Logger logger;
    @Mock
    MessagesExchange exchange;

    RootMessageProcessor processor;

    @BeforeEach
    void setUp() {
        processor = spy(FakeRootMessageProcessor.builder().logger(logger).exchange(exchange).build());
    }

    @Test
    void shouldCheckIsOwnerActive() {

        assertThat(processor.isOwnerActive()).isFalse();
    }

    @Test
    void shouldCheckIsProcessorActive() {
        AtomicBoolean state = (AtomicBoolean) ReflectionTestUtils.getField(processor, "processorActive");

        assertThat(state).isNotNull();
        assertThat(processor.isProcessorActive()).isEqualTo(state.get());
    }

    @Test
    void shouldSetProcessorActive() {
        AtomicBoolean atomicState = (AtomicBoolean) ReflectionTestUtils.getField(processor, "processorActive");
        assertThat(atomicState).isNotNull();
        boolean state = atomicState.get();
        assertThat(processor.isProcessorActive()).isEqualTo(state);

        processor.setProcessorActive(!state);

        atomicState = (AtomicBoolean) ReflectionTestUtils.getField(processor, "processorActive");
        assertThat(atomicState).isNotNull();
        boolean newState = atomicState.get();
        assertThat(processor.isProcessorActive()).isEqualTo(newState);
        assertThat(state).isNotEqualTo(newState);
    }

    @Test
    void shouldShutdown() {

        processor.shutdown();

        verify(processor).accept(CommandMessage.EMPTY);
    }

    // class implementation
    @SuperBuilder
    static class FakeRootMessageProcessor extends RootMessageProcessor {

        @Override
        public String getProcessorName() {
            return processorName;
        }

        @Override
        public <T> CommandMessage<T> takeMessage() {
            return null;
        }

        @Override
        public boolean isEmpty() {
            return false;
        }

        @Override
        public <T> boolean accept(CommandMessage<T> message) {
            return false;
        }

        @Override
        public void onTakenMessage(CommandMessage<?> message) {
            throw new UnsupportedOperationException("onTakenMessage");
        }
    }
}
