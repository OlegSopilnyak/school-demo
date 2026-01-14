package oleg.sopilnyak.test.service.command.executable.core.executor.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

import oleg.sopilnyak.test.service.message.CommandMessage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CommandMessageWatchdogTest {

    TestCommandMessageWatchdog<?> watchdog;

    @BeforeEach
    void setUp() {
        watchdog = spy(TestCommandMessageWatchdog.builder().build());
    }

    @Test
    void shouldRunWatchdog() {

        assertThat(watchdog.getState()).isSameAs(CommandMessageWatchdog.State.IN_PROGRESS);
    }

    @Test
    void shouldWaitForMessageCompleted() {
        assertThat(watchdog.getState()).isSameAs(CommandMessageWatchdog.State.IN_PROGRESS);
        new Thread(() -> {
            try {
                Thread.sleep(150);
                watchdog.setState(CommandMessageWatchdog.State.COMPLETED);
                synchronized (watchdog.monitor) {
                    watchdog.monitor.notifyAll();
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
        assertThat(watchdog.getState()).isSameAs(CommandMessageWatchdog.State.IN_PROGRESS);
        new Thread(() -> {
            try {
                Thread.sleep(2000);
                watchdog.setState(CommandMessageWatchdog.State.COMPLETED);
                synchronized (watchdog.monitor) {
                    watchdog.monitor.notifyAll();
                }
            } catch (InterruptedException _) {

            }
        }).start();

        watchdog.waitForMessageComplete();

        assertThat(watchdog.getState()).isSameAs(CommandMessageWatchdog.State.EXPIRED);
        assertThat(watchdog.getResult()).isNull();
    }

    @Test
    void shouldMakeMessageProcessingIsDoneAndReleaseWatchdog() {
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
        assertThat(watchdog.getState()).isSameAs(CommandMessageWatchdog.State.IN_PROGRESS);
        new Thread(() -> {
            try {
                Thread.sleep(150);
                watchdog.messageProcessingIsDone();
            } catch (InterruptedException _) {

            }
        }).start();

        watchdog.waitForMessageComplete();

        assertThat(watchdog.getState()).isSameAs(CommandMessageWatchdog.State.EXPIRED);
        assertThat(watchdog.getResult()).isNull();
    }

    @SuppressWarnings("unchecked")
    @Test
    void shouldGetAndSetResult() {
        assertThat(watchdog.getState()).isSameAs(CommandMessageWatchdog.State.IN_PROGRESS);
        assertThat(watchdog.getResult()).isNull();
        CommandMessage message = mock(CommandMessage.class);

        watchdog.setResult(message);

        assertThat(watchdog.getResult()).isSameAs(message);
        assertThat(watchdog.getState()).isSameAs(CommandMessageWatchdog.State.COMPLETED);
    }
}