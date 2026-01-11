package oleg.sopilnyak.test.service.command.executable.core.executor.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import oleg.sopilnyak.test.service.command.type.core.Context;
import oleg.sopilnyak.test.service.message.CommandMessage;

import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unchecked")
class MessageProgressWatchdogTest<T> {
    @Mock
    Context<T> context;
    @Mock
    CommandMessage<T> original;

    MessageProgressWatchdog<T> watchdog;

    @BeforeEach
    void setUp() {
        watchdog = spy(new MessageProgressWatchdog<>(original));
    }

    @Test
    void shouldSetResultAndSetStopWaitingFlag() {
        CommandMessage<T> result = mock(CommandMessage.class);
        assertThat(watchdog.getState()).isEqualTo(MessageProgressWatchdog.State.IN_PROGRESS);

        watchdog.setResult(result);

        assertThat(original).isNotEqualTo(watchdog.getResult());
        assertThat(watchdog.getState()).isEqualTo(MessageProgressWatchdog.State.COMPLETED);
    }

    @Test
    void shouldWaitForMessageComplete() {
        doReturn(context).when(original).getContext();
        CommandMessage<T> result = mock(CommandMessage.class);
        assertThat(watchdog.getState()).isEqualTo(MessageProgressWatchdog.State.IN_PROGRESS);
        new Thread(() -> {
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                e.printStackTrace();
                return;
            }
            watchdog.setResult(result);
            watchdog.messageProcessingIsDone();
        }).start();

        watchdog.waitForMessageComplete();

        assertThat(original).isNotEqualTo(watchdog.getResult());
        assertThat(watchdog.getState()).isEqualTo(MessageProgressWatchdog.State.COMPLETED);
        verify(original.getContext(), never()).failed(any(Exception.class));
    }

    @Test
    void shouldNotWaitingForMessageComplete_MessageExpired() {
        doReturn("correlation-id").when(original).getCorrelationId();
        doReturn(context).when(original).getContext();
        CommandMessage<T> result = mock(CommandMessage.class);
        assertThat(watchdog.getState()).isEqualTo(MessageProgressWatchdog.State.IN_PROGRESS);
        new Thread(() -> {
            try {
                Thread.sleep(1800);
            } catch (InterruptedException e) {
                e.printStackTrace();
                return;
            }
            watchdog.setResult(result);
            watchdog.messageProcessingIsDone();
        }).start();

        watchdog.waitForMessageComplete();

        assertThat(original).isEqualTo(watchdog.getResult());
        assertThat(watchdog.getState()).isEqualTo(MessageProgressWatchdog.State.EXPIRED);
        verify(original.getContext()).failed(any(TimeoutException.class));
    }
}