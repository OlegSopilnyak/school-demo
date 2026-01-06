package oleg.sopilnyak.test.service.command.executable.core.executor.messaging;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import oleg.sopilnyak.test.service.message.CommandMessage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unchecked")
class MessagesProcessorTest {
    MessagesProcessor processor;

    @Mock
    Logger logger;
    boolean ownerActive = false;
    boolean processorActive = false;
    String processorName = "FakeBasicMessageProcessor";

    @BeforeEach
    void setUp() {
        processor = spy(new FakeBasicMessageProcessor());
    }

    @Test
    void shouldActivateProcessor() {
        processor.activateProcessor();

        verify(processor).setProcessorActive(true);
    }

    @Test
    void shouldDeActivateProcessor() {
        processor.deActivateProcessor();

        verify(processor).setProcessorActive(false);
    }

    @Test
    void shouldDoingMainLoop_OwnerStops() throws InterruptedException {
        ownerActive = true;
        processorActive = false;
        when(processor.isOwnerActive()).thenReturn(false).thenReturn(true).thenReturn(true).thenReturn(false);
        CommandMessage<?> message = mock(CommandMessage.class);
        doReturn(message).when(processor).takeMessage();

        processor.doingMainLoop();

        verify(processor).isProcessorActive();
        verify(processor).activateProcessor();
        verify(processor, atLeast(2)).isOwnerActive();
        verify(processor).takeMessage();
        verify(processor).runAsyncTakenMessage(any(Runnable.class));
        verify(processor).onTakenMessage(message);
        verify(processor).deActivateProcessor();
    }

    @Test
    void shouldDoingMainLoop_LastMessageStops() throws InterruptedException {
        ownerActive = true;
        processorActive = false;
        CommandMessage message = mock(CommandMessage.class);
        when(processor.takeMessage()).thenReturn(message).thenReturn(CommandMessage.EMPTY);

        processor.doingMainLoop();

        verify(processor).isProcessorActive();
        verify(processor).activateProcessor();
        verify(processor, atLeast(2)).isOwnerActive();
        verify(processor, times(2)).takeMessage();
        verify(processor).runAsyncTakenMessage(any(Runnable.class));
        verify(processor).onTakenMessage(message);
        verify(processor).deActivateProcessor();
        verify(logger, times(2)).debug(anyString(), eq(processorName));
    }

    @Test
    void shouldNotDoingMainLoop_ProcessorIsActiveAlready() {
        processorActive = true;

        processor.doingMainLoop();

        verify(processor).isProcessorActive();
        verify(processor, never()).activateProcessor();
        verify(logger).warn(anyString(), eq(processorName));
    }

    @Test
    void shouldNotDoingMainLoop_OwnerIsNotActive() {
        ownerActive = false;
        processorActive = false;

        processor.doingMainLoop();

        verify(processor).isProcessorActive();
        verify(processor).activateProcessor();
        verify(processor, times(2)).isOwnerActive();
        verify(processor).deActivateProcessor();
    }

    @Test
    void shouldNotDoingMainLoop_TakeMessageThrows() throws InterruptedException {
        ownerActive = true;
        processorActive = false;
        Exception exception = new RuntimeException("Test Exception");
        doThrow(exception).when(processor).takeMessage();

        processor.doingMainLoop();

        verify(processor).isProcessorActive();
        verify(processor).activateProcessor();
        verify(processor, times(2)).isOwnerActive();
        verify(processor).takeMessage();
        verify(logger).warn(anyString(), eq(processorName), eq(exception));
        verify(processor).deActivateProcessor();
    }

    // class implementation
    class FakeBasicMessageProcessor implements MessagesProcessor {

        @Override
        public String getProcessorName() {
            return processorName;
        }

        @Override
        public boolean isOwnerActive() {
            return ownerActive;
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
        public boolean isProcessorActive() {
            return processorActive;
        }

        @Override
        public void setProcessorActive(boolean state) {
            processorActive = state;
        }

        @Override
        public void onTakenMessage(CommandMessage<?> message) {

        }

        @Override
        public void runAsyncTakenMessage(Runnable runnableForTakenMessage) {
            runnableForTakenMessage.run();
        }

        @Override
        public void shutdown() {
            notImplemented();
        }

        @Override
        public Logger getLogger() {
            return logger;
        }

        private void notImplemented() {
            throw new UnsupportedOperationException("notImplemented() cannot be performed because ...");
        }
    }
}