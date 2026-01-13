package oleg.sopilnyak.test.service.command.executable.core.executor.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

import oleg.sopilnyak.test.service.message.CommandMessage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;

@ExtendWith(MockitoExtension.class)
class CommandMessageWatchdogTest {

    @Mock
    Watchdog watchdog;

    @BeforeEach
    void setUp() {
        watchdog = spy(Watchdog.builder().build());
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
                Thread.sleep(200);
                watchdog.state = CommandMessageWatchdog.State.COMPLETED;
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
                watchdog.state = CommandMessageWatchdog.State.COMPLETED;
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
                watchdog.state = CommandMessageWatchdog.State.COMPLETED;
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

    @Test
    void shouldGetAndSetResult() {
        assertThat(watchdog.getState()).isSameAs(CommandMessageWatchdog.State.IN_PROGRESS);
        assertThat(watchdog.getResult()).isNull();
        CommandMessage message = mock(CommandMessage.class);

        watchdog.setResult(message);

        assertThat(watchdog.getResult()).isSameAs(message);
        assertThat(watchdog.getState()).isSameAs(CommandMessageWatchdog.State.COMPLETED);
    }

    @Data
    @Builder
    static class Watchdog implements CommandMessageWatchdog {
        CommandMessage result;
        @Getter
        @Builder.Default
        State state = State.IN_PROGRESS;
        final Object monitor = new Object();

        @Override
        public void setResult(CommandMessage result) {
            if (result != null) {
                this.result = result;
                this.state = State.COMPLETED;
            }
        }

        @Override
        public void waitForMessageComplete() {
            int maximumTry = 3;
            int tryCount = 1;
            while (state == State.IN_PROGRESS) {
                synchronized (monitor) {
                    try {
                        monitor.wait(100);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    if (tryCount++ >= maximumTry) {
                        state = State.EXPIRED;
                        break;
                    }
                }
            }
        }

        @Override
        public void messageProcessingIsDone() {
            synchronized (monitor) {
                if (state == State.COMPLETED) {
                    monitor.notifyAll();
                }
            }
        }
    }
}