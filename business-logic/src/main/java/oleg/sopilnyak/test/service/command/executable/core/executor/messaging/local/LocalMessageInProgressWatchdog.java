package oleg.sopilnyak.test.service.command.executable.core.executor.messaging.local;

import oleg.sopilnyak.test.service.command.executable.core.executor.messaging.CommandMessageWatchdog;
import oleg.sopilnyak.test.service.message.CommandMessage;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import lombok.extern.slf4j.Slf4j;

/**
 * Watcher: local command-message in progress watcher
 *
 * @param <T> the type of command execution result
 * @see CommandMessageWatchdog
 */
@Slf4j
class LocalMessageInProgressWatchdog<T> implements CommandMessageWatchdog<T> {
    private final Duration duration;
    // original instance of the message to watch after
    private final CommandMessage<T> original;
    private final AtomicReference<CommandMessage<T>> result = new AtomicReference<>(null);
    private final AtomicReference<State> state = new AtomicReference<>(State.IN_PROGRESS);
    // monitor for processed message waiting
    private final Object resultSemaphore = new Object();

    public LocalMessageInProgressWatchdog(CommandMessage<T> original) {
        this(original, Duration.ofMillis(1000L));
    }

    public LocalMessageInProgressWatchdog(CommandMessage<T> original, Duration duration) {
        this.duration = duration;
        this.original = original;
    }

    @Override
    public void waitForMessageComplete() {
        synchronized (resultSemaphore) {
            result.getAndSet(null);
            final LocalDateTime startsAt = LocalDateTime.now();
            // waiting while state is in progress
            while (state.get() == State.IN_PROGRESS) {
                try {
                    resultSemaphore.wait(25);
                    // check result message expiration
                    if (Duration.between(startsAt, LocalDateTime.now()).compareTo(duration) > 0) {
                        // updating watchdog's state
                        setState(State.EXPIRED);
                        result.getAndSet(original);
                        // updating result message context
                        final String errorMessage = "Expired message with id:" + getResult().getCorrelationId();
                        log.warn(errorMessage);
                        getResult().getContext().failed(new TimeoutException(errorMessage));
                        break;
                    }
                } catch (InterruptedException e) {
                    log.warn("Interrupted while waiting for state to complete.", e);
                    /* Clean up whatever needs to be handled before interrupting  */
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    @Override
    public void messageProcessingIsDone() {
        synchronized (resultSemaphore) {
            if (state.get() == State.COMPLETED) {
                resultSemaphore.notifyAll();
            }
        }
    }

    @Override
    public CommandMessage<T> getResult() {
        return result.get();
    }

    @Override
    public void setResult(CommandMessage<T> result) {
        if (result != null) {
            this.result.getAndSet(result);
            this.state.getAndSet(State.COMPLETED);
        }
    }

    @Override
    public State getState() {
        return state.get();
    }

    public void setState(State state) {
        this.state.getAndSet(state);
    }
}
