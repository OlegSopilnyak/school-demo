package oleg.sopilnyak.test.service.command.executable.core.executor.messaging.local;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import oleg.sopilnyak.test.service.command.executable.core.executor.messaging.CommandMessageWatchdog;
import oleg.sopilnyak.test.service.command.type.core.Context;
import oleg.sopilnyak.test.service.message.CommandMessage;

import java.time.Duration;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unchecked")
class LocalMessageInProgressWatchdogTest<T> {
    @Mock
    CommandMessage<T> commandMessage;
    LocalMessageInProgressWatchdog<T> watchdog;

    @BeforeEach
    void setUp() {
        watchdog = spy(new LocalMessageInProgressWatchdog<>(commandMessage, Duration.ofMillis(400L)));
    }

    @Test
    void shouldRunWatchdog() {

        assertThat(watchdog.getState()).isSameAs(CommandMessageWatchdog.State.IN_PROGRESS);
    }

    @Test
    void shouldWaitForMessageComplete() {
        assertThat(watchdog.getState()).isSameAs(CommandMessageWatchdog.State.IN_PROGRESS);
        final Object semaphore = ReflectionTestUtils.getField(watchdog, "resultSemaphore");
        assertThat(semaphore).isNotNull();
        new Thread(() -> {
            try {
                Thread.sleep(150);
                watchdog.setState(CommandMessageWatchdog.State.COMPLETED);
                synchronized (semaphore) {
                    semaphore.notifyAll();
                }
            } catch (InterruptedException _) {
            }
        }).start();

        watchdog.waitForMessageComplete();

        assertThat(watchdog.getState()).isSameAs(CommandMessageWatchdog.State.COMPLETED);
        assertThat(watchdog.getResult()).isNull();
    }

    @Test
    void shouldWaitForMessageExpired() {
        Context<T> context = mock(Context.class);
        doReturn(context).when(commandMessage).getContext();
        assertThat(watchdog.getState()).isSameAs(CommandMessageWatchdog.State.IN_PROGRESS);
        final Object semaphore = ReflectionTestUtils.getField(watchdog, "resultSemaphore");
        assertThat(semaphore).isNotNull();
        new Thread(() -> {
            try {
                Thread.sleep(1000);
                watchdog.setState(CommandMessageWatchdog.State.COMPLETED);
                synchronized (semaphore) {
                    semaphore.notifyAll();
                }
            } catch (InterruptedException _) {
            }
        }).start();

        watchdog.waitForMessageComplete();

        ArgumentCaptor<Exception> commandFailedCaptor = ArgumentCaptor.forClass(Exception.class);
        verify(context).failed(commandFailedCaptor.capture());
        assertThat(commandFailedCaptor.getValue()).isInstanceOf(TimeoutException.class);
        assertThat(commandFailedCaptor.getValue().getMessage()).startsWith("Expired message with id:");
        assertThat(watchdog.getState()).isSameAs(CommandMessageWatchdog.State.EXPIRED);
        assertThat(watchdog.getResult()).isSameAs(commandMessage);
    }

    @Test
    void shouldMakeMessageProcessingIsDone() {
        assertThat(watchdog.getState()).isSameAs(CommandMessageWatchdog.State.IN_PROGRESS);
        new Thread(() -> {
            try {
                Thread.sleep(150);
                watchdog.setState(CommandMessageWatchdog.State.COMPLETED);
                watchdog.messageProcessingIsDone();
            } catch (InterruptedException _) {

            }
        }).start();

        watchdog.waitForMessageComplete();

        assertThat(watchdog.getState()).isSameAs(CommandMessageWatchdog.State.COMPLETED);
        assertThat(watchdog.getResult()).isNull();
    }

    @Test
    void shouldMakeMessageProcessingIsDoneAndNotReleaseWatchdog() {
        Context<T> context = mock(Context.class);
        doReturn(context).when(commandMessage).getContext();
        assertThat(watchdog.getState()).isSameAs(CommandMessageWatchdog.State.IN_PROGRESS);
        new Thread(() -> {
            try {
                Thread.sleep(150);
                watchdog.messageProcessingIsDone();
            } catch (InterruptedException _) {

            }
        }).start();

        watchdog.waitForMessageComplete();

        ArgumentCaptor<Exception> commandFailedCaptor = ArgumentCaptor.forClass(Exception.class);
        verify(context).failed(commandFailedCaptor.capture());
        assertThat(commandFailedCaptor.getValue()).isInstanceOf(TimeoutException.class);
        assertThat(commandFailedCaptor.getValue().getMessage()).startsWith("Expired message with id:");
        assertThat(watchdog.getState()).isSameAs(CommandMessageWatchdog.State.EXPIRED);
        assertThat(watchdog.getResult()).isSameAs(commandMessage);
    }

    @Test
    void shouldGetSetResult() {
        assertThat(watchdog.getState()).isSameAs(CommandMessageWatchdog.State.IN_PROGRESS);
        assertThat(watchdog.getResult()).isNull();
        CommandMessage message = mock(CommandMessage.class);

        watchdog.setResult(message);

        assertThat(watchdog.getResult()).isSameAs(message);
        assertThat(watchdog.getState()).isSameAs(CommandMessageWatchdog.State.COMPLETED);
    }

    @Test
    void shouldNotGetSetResult_SettingResultIsNull() {
        assertThat(watchdog.getState()).isSameAs(CommandMessageWatchdog.State.IN_PROGRESS);
        assertThat(watchdog.getResult()).isNull();

        watchdog.setResult(null);

        assertThat(watchdog.getResult()).isNull();
        assertThat(watchdog.getState()).isSameAs(CommandMessageWatchdog.State.IN_PROGRESS);
    }

    @Test
    void shouldGetDefaultState() {
        assertThat(watchdog.getState()).isSameAs(CommandMessageWatchdog.State.IN_PROGRESS);
    }

    @Test
    void shouldSetExpiredState() {
        assertThat(watchdog.getState()).isSameAs(CommandMessageWatchdog.State.IN_PROGRESS);

        watchdog.setState(CommandMessageWatchdog.State.EXPIRED);

        assertThat(watchdog.getState()).isSameAs(CommandMessageWatchdog.State.EXPIRED);
    }
}